package com.cloud.doctor.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Snowflake;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.doctor.entity.*;
import com.cloud.doctor.entity.dto.AppointSubmitReq;
import com.cloud.doctor.entity.vo.AppointmentVO;
import com.cloud.doctor.mapper.*;
import com.cloud.doctor.service.AppointmentService;
import com.cloud.doctor.service.ScheduleService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 10533
* @description 针对表【bus_appointment(挂号预约订单表)】的数据库操作Service实现
* @createDate 2025-11-20 22:59:11
*/
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment>
    implements AppointmentService{

    private final ScheduleMapper scheduleMapper;

    private final UserMapper userMapper;

    private final AppointmentMapper appointmentMapper;

    private final Snowflake snowflake;

    private final DoctorMapper doctorMapper;

    private final DepartmentMapper departmentMapper;

    private final StringRedisTemplate redisTemplate; // 注入 Redis

    private DefaultRedisScript<Long> deductStockScript; // 脚本对象

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitOrder(AppointSubmitReq req, Long userId) {
        Long scheduleId = req.scheduleId();
        String stockKey = "schedule:stock:" + scheduleId;

        // 1.【Redis + Lua】核心扣减逻辑
        // 执行脚本，参数是 Key
        Long result = redisTemplate.execute(deductStockScript, Collections.singletonList(stockKey));

        // 2.处理返回结果
        if (result == 0) {
            throw new RuntimeException("号源已抢光"); // ⚡️ 流量直接在这里被挡住，不查数据库
        }

        if (result == -1) {
            // Redis 里没这个 Key？说明是冷门排班，或者Redis重启了。
            // 【兜底策略】去数据库查一次，初始化 Redis，再试一次
            // (实际高并发场景这里需要加分布式锁防止击穿，为了简单我们先直接查)
            Schedule schedule = scheduleMapper.selectById(scheduleId);
            if (schedule == null || schedule.getRemainingQuota() <= 0) {
                throw new RuntimeException("号源已抢光");
            }
            // 将数据库库存写入 Redis (预热)
            redisTemplate.opsForValue().set(stockKey, schedule.getRemainingQuota().toString());
            // 再扣一次
            Long retry = redisTemplate.execute(deductStockScript, Collections.singletonList(stockKey));
            if (retry <= 0) throw new RuntimeException("号源已抢光");
        }

        // 走到这里，说明 Redis 扣减成功，拿到了资格

        try {
            // 3.【MySQL 落地】执行真正的业务
            // 注意：这里依然保留乐观锁 version，作为最后一道防线
            int rows = scheduleMapper.update(null, new LambdaUpdateWrapper<Schedule>()
                    .setSql("remaining_quota = remaining_quota - 1")
                    .setSql("version = version + 1")
                    .eq(Schedule::getId, scheduleId)
                    .gt(Schedule::getRemainingQuota, 0)); // 双重保险

            if (rows == 0) {
                throw new RuntimeException("并发冲突，请重试");
            }

            // 4.创建订单
            User user = userMapper.selectById(userId);
            Doctor doctor = doctorMapper.selectById(scheduleMapper.selectById(scheduleId).getDoctorId()); // 查医生为了拿费用

            Appointment order = new Appointment();
            order.setOrderNo(snowflake.nextIdStr());
            order.setUserId(userId);
            order.setScheduleId(scheduleId);
            order.setDoctorId(doctor.getId());
            order.setPatientName(user.getRealName());
            order.setFee(doctor.getConsultPrice());
            order.setStatus(0); // 待支付

            appointmentMapper.insert(order);
            return order.getId();

        } catch (Exception e) {
            // 5.如果 MySQL 失败了，把 Redis 扣掉的库存还回去
            redisTemplate.opsForValue().increment(stockKey);
            throw e; // 继续抛出异常，让事务回滚
        }
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
    }

    // 初始化时加载 Lua 脚本，防止每次请求都去读文件 IO
    @PostConstruct
    public void init() {
        deductStockScript = new DefaultRedisScript<>();
        deductStockScript.setResultType(Long.class);
        deductStockScript.setLocation(new ClassPathResource("scripts/deduct_stock.lua"));
    }
}




