package com.cloud.doctor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.doctor.entity.Department;
import com.cloud.doctor.service.DepartmentService;
import com.cloud.doctor.mapper.DepartmentMapper;
import org.springframework.stereotype.Service;

/**
* @author 10533
* @description 针对表【base_department(医院科室层级表)】的数据库操作Service实现
* @createDate 2025-11-20 22:59:11
*/
@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department>
    implements DepartmentService{

}




