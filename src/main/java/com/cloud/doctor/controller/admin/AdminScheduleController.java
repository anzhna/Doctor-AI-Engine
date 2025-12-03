package com.cloud.doctor.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.cloud.doctor.common.Result;
import com.cloud.doctor.entity.dto.ScheduleFormReq;
import com.cloud.doctor.entity.vo.ScheduleVO;
import com.cloud.doctor.service.AdminScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Select;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/schedule")
@Tag(name = "【管理端】排班管理")
@RequiredArgsConstructor
public class AdminScheduleController {

    // ✅ 注入专门的管理端 Service
    private final AdminScheduleService adminScheduleService;

    @PostMapping("/add")
    @Operation(summary = "发布排班")
    @SaCheckRole("admin")
    public Result<String> addSchedule(@RequestBody ScheduleFormReq req) {

        adminScheduleService.addSchedule(req);

        return Result.success("排班发布成功，库存已同步");
    }

    @GetMapping("")
    @Operation(summary = "查询排版")
    @SaCheckRole("admin")
    public Result<List<ScheduleVO>> selectAllSchedule() {
        return Result.success(adminScheduleService.selectAllSchedule());
    }
}
