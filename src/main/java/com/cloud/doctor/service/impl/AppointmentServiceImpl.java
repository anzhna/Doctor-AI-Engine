package com.cloud.doctor.service.impl;

import cn.hutool.core.lang.Snowflake;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.doctor.entity.Appointment;
import com.cloud.doctor.entity.Department;
import com.cloud.doctor.entity.Doctor;
import com.cloud.doctor.entity.Schedule;
import com.cloud.doctor.entity.User;
import com.cloud.doctor.entity.dto.AppointSubmitReq;
import com.cloud.doctor.entity.vo.AppointmentVO;
import com.cloud.doctor.mapper.*;
import com.cloud.doctor.service.AppointmentService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import cn.hutool.core.bean.BeanUtil; // 记得检查 import

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements AppointmentService {

    private final ScheduleMapper scheduleMapper;
    private final UserMapper userMapper;
    private final AppointmentMapper appointmentMapper;
    private final Snowflake snowflake;
    private final DoctorMapper doctorMapper;
    private final DepartmentMapper departmentMapper;
    private final StringRedisTemplate redisTemplate;
    //编程式事务管理器
    private final TransactionTemplate transactionTemplate;
    //Redisson 客户端 (用于加锁)
    private final RedissonClient redissonClient;

    private DefaultRedisScript<Long> deductStockScript;
    /*@Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitOrder(AppointSubmitReq req, Long userId) {
        Schedule schedule = scheduleMapper.selectById(req.scheduleId());
        if (schedule == null) {
            throw new RuntimeException("排班不存在");
        }
        if (schedule.getRemainingQuota() <= 0) {
            throw new RuntimeException("号源已抢光");
        }

        //乐观锁订单和扣减业务
        int update = scheduleMapper.update(null, new LambdaUpdateWrapper<Schedule>()
                .eq(Schedule::getId, schedule.getId())
                .eq(Schedule::getVersion, schedule.getVersion())
                .gt(Schedule::getRemainingQuota, 0)
                .setSql("version = version+1")
                .setSql("remaining_quota = remaining_quota-1")
        );

        if (update != 1) {
            throw new RuntimeException("挂号失败，请重试");
        }

        //查用户信息 (为了订单快照，冗余 patient_name)
        User user = userMapper.selectById(userId);

        //设计模式使用“工厂思维”构建订单对象
        Appointment appointment = new Appointment();
        //雪花算法设计id
        appointment.setOrderNo(snowflake.nextIdStr());
        appointment.setUserId(userId);
        appointment.setDoctorId(schedule.getDoctorId());
        appointment.setScheduleId(schedule.getId());
        appointment.setPatientName(user.getRealName());
        appointment.setFee(doctorMapper.selectById(schedule.getDoctorId()).getConsultPrice());
        appointment.setStatus(0);


        appointmentMapper.insert(appointment);

        return appointment.getId();


    }*/

    // 初始化时加载 Lua 脚本，防止每次请求都去读文件 IO
    @PostConstruct
    public void init() {
        deductStockScript = new DefaultRedisScript<>();
        deductStockScript.setResultType(Long.class);
        deductStockScript.setLocation(new ClassPathResource("scripts/deduct_stock.lua"));
    }
    @Override
    // 这里不要加 @Transactional（Redis 拦截要在事务外）
    public Long submitOrder(AppointSubmitReq req, Long userId) {
        Long scheduleId = req.scheduleId();
        String stockKey = "schedule:stock:" + scheduleId;

        // 1. 【Redis + Lua】第一次尝试扣减
        Long result = redisTemplate.execute(deductStockScript, Collections.singletonList(stockKey));

        // 2. 库存不足
        if (result == 0) {
            throw new RuntimeException("号源已抢光");
        }

        // 3. 缓存未预热 (返回 -1) -> 触发懒加载 + 分布式锁
        if (result == -1) {
            // 定义锁 Key
            String lockKey = "lock:schedule:warmup:" + scheduleId;
            RLock lock = redissonClient.getLock(lockKey);

            try {
                // 尝试加锁：最多等 3 秒，上锁后 10 秒自动释放
                boolean isLocked = lock.tryLock(3, 10, TimeUnit.SECONDS);
                if (isLocked) {
                    try {
                        // 进锁之后再试一次 Redis，可能已经缓存写入
                        result = redisTemplate.execute(deductStockScript, Collections.singletonList(stockKey));

                        if (result == 1) {
                            // 已经把缓存写入，直接跳出if执行数据库逻辑
                        } else if (result == 0) {
                            throw new RuntimeException("号源已抢光");
                        } else {
                            // 还是 -1，说明我是第一个进来的 -> 查询数据库
                            Schedule schedule = scheduleMapper.selectById(scheduleId);
                            if (schedule == null || schedule.getRemainingQuota() <= 0) {
                                throw new RuntimeException("号源已抢光或排班不存在");
                            }

                            // 写入redis缓存
                            redisTemplate.opsForValue().set(stockKey, schedule.getRemainingQuota().toString());

                            // 再次执行扣减redis逻辑
                            result = redisTemplate.execute(deductStockScript, Collections.singletonList(stockKey));
                            if (result != 1) {
                                throw new RuntimeException("号源已抢光");
                            }
                        }
                    } finally {
                        // 释放锁
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                } else {
                    // 加锁失败（人太多了）
                    throw new RuntimeException("系统繁忙，请稍后重试");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("系统繁忙");
            }
        }

        // 数据库
        return transactionTemplate.execute(status -> {
            try {
                // MySQL乐观锁扣减
                int rows = scheduleMapper.update(null, new LambdaUpdateWrapper<Schedule>()
                        .setSql("remaining_quota = remaining_quota - 1")
                        .setSql("version = version + 1")
                        .eq(Schedule::getId, scheduleId)
                        .gt(Schedule::getRemainingQuota, 0));

                if (rows == 0) {
                    throw new RuntimeException("并发冲突，请重试");
                }

                // 2. 准备数据
                Schedule schedule = scheduleMapper.selectById(scheduleId);
                Doctor doctor = doctorMapper.selectById(schedule.getDoctorId());
                User user = userMapper.selectById(userId);

                // 3. 创建订单
                Appointment order = new Appointment();
                order.setOrderNo(snowflake.nextIdStr());
                order.setUserId(userId);
                order.setScheduleId(scheduleId);
                order.setDoctorId(doctor.getId());
                order.setPatientName(user.getRealName());
                order.setFee(doctor.getConsultPrice());
                order.setStatus(0); // 待支付
                order.setCreateTime(LocalDateTime.now());

                appointmentMapper.insert(order);
                return order.getId();

            } catch (Exception e) {
                // 异常处理：回滚事务 + 回滚 Redis
                status.setRollbackOnly();
                redisTemplate.opsForValue().increment(stockKey);
                throw new RuntimeException(e.getMessage());
            }
        });
    }

    @Override
    public List<AppointmentVO> getAppointment(Long Userid) {
        //根据用户id查询订单
        List<Appointment> appointments = appointmentMapper.selectList(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getUserId, Userid)
                .eq(Appointment::getIsDeleted, 0)
                .orderByDesc(Appointment::getCreateTime));

        //将信息填充到vo
        return appointments.stream().map(appointment -> {
            AppointmentVO appointmentVO = new AppointmentVO();
            BeanUtil.copyProperties(appointment, appointmentVO);
            //医生姓名
            Doctor doctor = doctorMapper.selectById(appointment.getDoctorId());
            if (doctor != null) {
                appointmentVO.setDoctorName(doctor.getRealName());
                //科室姓名
                Department department = departmentMapper.selectById(doctor.getDeptId());
                if (department != null) {
                    appointmentVO.setDeptName(department.getName());
                }

            }

            Schedule schedule = scheduleMapper.selectById(appointment.getScheduleId());
            if (schedule != null) {
                //就诊日期
                appointmentVO.setWorkDate(schedule.getWorkDate());
                //就诊班次
                appointmentVO.setShiftDesc(schedule.getShiftType()==1 ? "上午":"下午");
            }


            //订单状态
            String statusStr = switch (appointment.getStatus()) {
                case 0 -> "待支付";
                case 1 -> "已支付";
                case 2 -> "已完成";
                case 3 -> "已取消";
                default -> "未知状态";
            };
            appointmentVO.setStatusDesc(statusStr);

            return appointmentVO;
        }).collect(Collectors.toList());
    }

    @Override
    public void payOrder(Long orderId) {
        Appointment order = appointmentMapper.selectById(orderId);
        if (order == null) throw new RuntimeException("订单不存在");
        if (order.getStatus() != 0) throw new RuntimeException("订单状态无法支付");

        order.setStatus(1); // 已支付
        order.setPayTime(LocalDateTime.now());
        appointmentMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 事务！
    public void cancelOrder(Long orderId) {
        //查询订单
        Appointment appointment = appointmentMapper.selectById(orderId);
        if (appointment == null) {
            throw new RuntimeException("订单不存在");
        }
        //确认订单是否已取消
        if (appointment.getStatus() == 3) {
            throw new RuntimeException("订单已取消");
        }
        //回滚事务
        appointmentMapper.update(null,new LambdaUpdateWrapper<Appointment>()
                .setSql("status = 3")
                .eq(Appointment::getId, orderId));
        scheduleMapper.update(null,new LambdaUpdateWrapper<Schedule>()
                .setSql("remaining_quota = remaining_quota+1")
                .eq(Schedule::getId,appointment.getScheduleId()));
        //redis也重新回滚
        String stockKey = "schedule:stock:" + appointment.getScheduleId();
        redisTemplate.opsForValue().increment(stockKey);
    }

}




