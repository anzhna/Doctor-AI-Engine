package com.cloud.doctor.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.doctor.entity.Department;
import com.cloud.doctor.entity.Doctor;
import com.cloud.doctor.entity.vo.DepartmentVO;
import com.cloud.doctor.entity.vo.DoctorVO;
import com.cloud.doctor.mapper.DepartmentMapper;
import com.cloud.doctor.mapper.DoctorMapper;
import com.cloud.doctor.service.HospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HospitalServiceImpl implements HospitalService {

    private final DepartmentMapper departmentMapper;

    private final DoctorMapper doctorMapper;

    @Override
    public List<DepartmentVO> listDeptTree() {
        //查询所有科室
        List<Department> departments = departmentMapper.selectList(null);
        //把类型变成vo
        List<DepartmentVO> departmentVOS = BeanUtil.copyToList(departments, DepartmentVO.class);
        //变成map结构
        Map<Object, List<DepartmentVO>> collect = departmentVOS.stream().collect(Collectors.groupingBy(DepartmentVO::getParentId));
        //根据0 节点分类
        List<DepartmentVO> roots = collect.getOrDefault(0L, new ArrayList<>());
        //子节点挂载
        for (DepartmentVO root : roots) {
            root.setChildren(collect.get(root.getId()));
        }

        return roots;
    }

    @Override
    public List<DoctorVO> listDoctors(Long deptId) {
        List<Doctor> doctors = doctorMapper.selectList(new LambdaQueryWrapper<Doctor>()
                .eq(Doctor::getDeptId, deptId)
                .eq(Doctor::getStatus, 1));

        String name = departmentMapper.selectById(deptId).getName();
        return doctors.stream().map(doctor -> {
            DoctorVO doctorVO = new DoctorVO();
            BeanUtil.copyProperties(doctor, doctorVO);
            doctorVO.setDeptName(name);
            return doctorVO;
        }).collect(Collectors.toList());
    }
}
