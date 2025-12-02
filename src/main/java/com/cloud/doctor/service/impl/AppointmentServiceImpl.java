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
import org.springframework.transaction.support.TransactionTemplate;

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
    //编程式事务管理器
    private final TransactionTemplate transactionTemplate;


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
    // ❌ 【重点】这里千万不要加 @Transactional 注解！否则 Redis 拦截层会失效，所有请求都会占用数据库连接。
    public Long submitOrder(AppointSubmitReq req, Long userId) {
        Long scheduleId = req.scheduleId();
        String stockKey = "schedule:stock:" + scheduleId;

        // Redis (纯内存操作，极速，不占数据库连接)
        Long result = redisTemplate.execute(deductStockScript, Collections.singletonList(stockKey));

        // 1. 库存不足 (Redis 拦截)
        if (result == 0) {
            // 建议：压测时可以在 GlobalExceptionHandler 里针对这个异常不打印堆栈，防止控制台刷屏
            //throw new RuntimeException("号源已抢光");
            return -1L;
        }

        // 2. 缓存未预热 (Redis 拦截)
        if (result == -1) {
            // 严禁在这里查库回填 Redis！必须由管理员提前预热！
            //throw new RuntimeException("系统异常：排班未预热");
            return -2L;
        }

        // 数据库落地 (只有抢到号的 30 个人会走到这里)
        return transactionTemplate.execute(status -> {
            try {
                // 1. MySQL 乐观锁扣减 (双重保障)
                // SQL: UPDATE ... SET quota = quota - 1, version = version + 1 ...
                int rows = scheduleMapper.update(null, new LambdaUpdateWrapper<Schedule>()
                        .setSql("remaining_quota = remaining_quota - 1")
                        .setSql("version = version + 1")
                        .eq(Schedule::getId, scheduleId)
                        .gt(Schedule::getRemainingQuota, 0));

                if (rows == 0) {
                    // 极低概率会走到这里 (Redis 放行了但 MySQL 没更新成功)
                    throw new RuntimeException("并发冲突，请重试");
                }

                // 2. 准备订单数据
                // (为了数据完整性，虽然 Update 了，还是查一下获取 DoctorId 和 Fee)
                Schedule schedule = scheduleMapper.selectById(scheduleId);
                Doctor doctor = doctorMapper.selectById(schedule.getDoctorId());
                User user = userMapper.selectById(userId);

                // 3. 创建订单
                Appointment order = new Appointment();
                order.setOrderNo(snowflake.nextIdStr()); // 雪花算法
                order.setUserId(userId);
                order.setScheduleId(scheduleId);
                order.setDoctorId(doctor.getId());
                order.setPatientName(user.getRealName());
                order.setFee(doctor.getConsultPrice());
                order.setStatus(0); // 0-待支付
                order.setCreateTime(LocalDateTime.now());

                appointmentMapper.insert(order);
                return order.getId();

            } catch (Exception e) {
                // 异常处理 (事务回滚 + Redis 补偿)

                // 1. 标记数据库事务回滚
                status.setRollbackOnly();

                // 2. 【关键】把 Redis 刚才扣掉的 1 个库存还回去！
                redisTemplate.opsForValue().increment(stockKey);

                // 3. 抛出异常给 Controller
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




