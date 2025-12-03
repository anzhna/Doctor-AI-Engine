package com.cloud.doctor.controller.admin;


import cn.dev33.satoken.annotation.SaCheckRole;
import com.cloud.doctor.common.Result;
import com.cloud.doctor.entity.Doctor;
import com.cloud.doctor.entity.dto.DoctorReq;
import com.cloud.doctor.entity.vo.DoctorVO;
import com.cloud.doctor.service.AdminDoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Select;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/doctor")
@Tag(name = "【管理端】医生管理")
@RequiredArgsConstructor
public class AdminDoctorController {
    private final AdminDoctorService adminDoctorService;

    @PostMapping("/add")
    @Operation(summary = "新增医生")
    @SaCheckRole("admin")
    public Result<String> addDoctor(@RequestBody DoctorReq req) {
        adminDoctorService.addDoctor(req);
        return Result.success("医生添加成功");
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除医生")
    @SaCheckRole("admin")
    public Result<String> deleteDoctor(@PathVariable Long id) {
        adminDoctorService.deleteDoctor(id);
        return Result.success("医生删除成功");
    }

    @GetMapping("")
    @Operation(summary = "查询医生")
    @SaCheckRole("admin")
    public Result<List<DoctorVO>> selectDoctor(){
        List<DoctorVO> doctorVOS = adminDoctorService.selectDoctor();
        return Result.success(doctorVOS);
    }
}
