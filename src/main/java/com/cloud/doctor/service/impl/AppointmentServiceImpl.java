package com.cloud.doctor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.doctor.entity.Appointment;
import com.cloud.doctor.service.AppointmentService;
import com.cloud.doctor.mapper.AppointmentMapper;
import org.springframework.stereotype.Service;

/**
* @author 10533
* @description 针对表【bus_appointment(挂号预约订单表)】的数据库操作Service实现
* @createDate 2025-11-20 22:59:11
*/
@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment>
    implements AppointmentService{

}




