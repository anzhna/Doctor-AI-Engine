package com.cloud.doctor.service;

import com.cloud.doctor.entity.vo.DepartmentVO;
import com.cloud.doctor.entity.vo.DoctorVO;
import org.springframework.stereotype.Service;

import java.util.List;

public interface HospitalService {
    // 查科室树
    List<DepartmentVO> listDeptTree();
    // 查某科室下的医生
    List<DoctorVO> listDoctors(Long deptId);
}
