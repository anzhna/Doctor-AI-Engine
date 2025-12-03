package com.cloud.doctor.controller;


import cn.dev33.satoken.stp.StpUtil;
import com.cloud.doctor.common.Result;
import com.cloud.doctor.entity.dto.AppointSubmitReq;
import com.cloud.doctor.entity.vo.AppointmentVO;
import com.cloud.doctor.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
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
        if (appId == -1L) {
            return Result.error("抢完了");
        }
        return Result.success(appId);
    }
    @GetMapping("/list")
    @Operation(summary = "查询我的挂号记录")
    public Result<List<AppointmentVO>> listMyAppointments(){
        //获取用户id
        long userId = StpUtil.getLoginIdAsLong();

        return Result.success(appointmentService.getAppointment(userId));
    }

    @PostMapping("/pay")
    @Operation(summary = "模拟支付订单")
    public Result<String> payOrder(@RequestParam Long orderId) {
        appointmentService.payOrder(orderId);
        return Result.success("支付成功");
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消订单(回退库存)")
    public Result<String> cancelOrder(@RequestParam Long orderId) {
        appointmentService.cancelOrder(orderId);
        return Result.success("取消成功");
    }
}
