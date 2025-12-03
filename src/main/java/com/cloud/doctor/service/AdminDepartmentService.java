package com.cloud.doctor.service;

import com.cloud.doctor.entity.dto.DepartmentFormReq;
import com.cloud.doctor.entity.vo.DepartmentVO;

import java.util.List;

public interface AdminDepartmentService {

    // 新增科室
    void addDepartment(DepartmentFormReq req);

    // 删除科室
    void deleteDepartment(Long id);

    //查询所有科室
    List<DepartmentVO> selectAllDepartments();
}