package com.cloud.doctor.service;

import com.cloud.doctor.entity.Doctor;
import com.cloud.doctor.entity.dto.DoctorReq;
import com.cloud.doctor.entity.vo.DoctorVO;

import java.util.List;

public interface AdminDoctorService {
    // 新增医生（后台管理）
    void addDoctor(DoctorReq req);

    void deleteDoctor(Long id);

    List<DoctorVO> selectDoctor();
}
