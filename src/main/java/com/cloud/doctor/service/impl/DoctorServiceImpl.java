package com.cloud.doctor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.doctor.entity.Doctor;
import com.cloud.doctor.service.DoctorService;
import com.cloud.doctor.mapper.DoctorMapper;
import org.springframework.stereotype.Service;

/**
* @author 10533
* @description 针对表【base_doctor(医生信息表)】的数据库操作Service实现
* @createDate 2025-11-20 22:59:11
*/
@Service
public class DoctorServiceImpl extends ServiceImpl<DoctorMapper, Doctor>
    implements DoctorService{

}




