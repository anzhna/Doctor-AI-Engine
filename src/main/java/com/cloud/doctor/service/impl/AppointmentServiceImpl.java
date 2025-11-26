package com.cloud.doctor.service.impl;

import cn.hutool.core.lang.Snowflake;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.doctor.entity.Appointment;
import com.cloud.doctor.entity.Schedule;
import com.cloud.doctor.entity.User;
import com.cloud.doctor.entity.dto.AppointSubmitReq;
import com.cloud.doctor.mapper.DoctorMapper;
import com.cloud.doctor.mapper.ScheduleMapper;
import com.cloud.doctor.mapper.UserMapper;
import com.cloud.doctor.service.AppointmentService;
import com.cloud.doctor.mapper.AppointmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    @Override
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


    }
}




