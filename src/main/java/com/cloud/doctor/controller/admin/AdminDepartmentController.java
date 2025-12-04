package com.cloud.doctor.controller.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.cloud.doctor.common.Result;
import com.cloud.doctor.entity.dto.DepartmentFormReq;
import com.cloud.doctor.entity.vo.DepartmentVO;
import com.cloud.doctor.service.AdminDepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/dept")
@Tag(name = "管理端 科室管理")
@RequiredArgsConstructor
public class AdminDepartmentController {

    private final AdminDepartmentService adminDepartmentService;

    @PostMapping("/add")
    @Operation(summary = "新增科室(同步布隆)")
    @SaCheckLogin
    public Result<String> addDepartment(@RequestBody DepartmentFormReq req) {
        adminDepartmentService.addDepartment(req);
        return Result.success("新增成功");
    }

    @PostMapping("/delete/{id}")
    @Operation(summary = "删除科室")
    @SaCheckLogin
    public Result<String> deleteDepartment(@RequestParam Long id) {
        adminDepartmentService.deleteDepartment(id);
        return Result.success("删除成功");
    }

    @GetMapping("")
    @Operation(summary = "查询科室")
    @SaCheckRole("admin")
    public Result<List<DepartmentVO>> selectAllDepartment() {
        List<DepartmentVO> departmentVOS = adminDepartmentService.selectAllDepartments();
        return Result.success(departmentVOS);

    }
}