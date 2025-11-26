package com.cloud.doctor.controller;


import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.cloud.doctor.common.Result;
import com.cloud.doctor.entity.dto.AppointSubmitReq;
import com.cloud.doctor.entity.vo.AppointmentVO;
import com.cloud.doctor.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/appoint")
@Tag(name = "挂号交易模块")
@RequiredArgsConstructor
public class AppointmentController {
    private final AppointmentService appointmentService;

    @PostMapping("/submit")
    @Operation(summary = "提交挂号订单")
    public Result<Long> submitOrder(@RequestBody AppointSubmitReq req){
        long userId = StpUtil.getLoginIdAsLong();

        Long appId = appointmentService.submitOrder(req, userId);
        return Result.success(appId);
    }
    @GetMapping("/list")
    @Operation(summary = "查询我的挂号记录")
    public Result<List<AppointmentVO>> listMyAppointments(){
        //获取用户id
        long userId = StpUtil.getLoginIdAsLong();

        return Result.success(appointmentService.getAppointment(userId));
    }
}
