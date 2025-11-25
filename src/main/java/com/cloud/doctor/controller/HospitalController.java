package com.cloud.doctor.controller;

import com.cloud.doctor.common.Result;
import com.cloud.doctor.entity.vo.DepartmentVO;
import com.cloud.doctor.entity.vo.DoctorVO;
import com.cloud.doctor.service.HospitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/hospital")
@Tag(name = "医院信息模块")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    @GetMapping("/dept/tree")
    @Operation(summary = "查询科室列表(树形)")
    public Result<List<DepartmentVO>> listDeptTree() {
        return Result.success(hospitalService.listDeptTree());
    }

    @GetMapping("/doctor/list")
    @Operation(summary = "查询某科室下的医生")
    public Result<List<DoctorVO>> listDoctors(@RequestParam Long deptId) {
        return Result.success(hospitalService.listDoctors(deptId));
    }
}