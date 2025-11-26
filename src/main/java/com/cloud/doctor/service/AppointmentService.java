package com.cloud.doctor.service;

import com.cloud.doctor.entity.Appointment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.doctor.entity.dto.AppointSubmitReq;

/**
* @author 10533
* @description 针对表【bus_appointment(挂号预约订单表)】的数据库操作Service
* @createDate 2025-11-20 22:59:12
*/
public interface AppointmentService extends IService<Appointment> {

    Long submitOrder(AppointSubmitReq req, Long userId);
}
