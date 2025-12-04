package com.cloud.doctor.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.doctor.entity.Department;
import com.cloud.doctor.entity.dto.DepartmentFormReq;
import com.cloud.doctor.entity.vo.DepartmentVO;
import com.cloud.doctor.mapper.DepartmentMapper;
import com.cloud.doctor.service.AdminDepartmentService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDepartmentServiceImpl implements AdminDepartmentService {

    private final DepartmentMapper departmentMapper;

    // ✅ 注入你在 BloomFilterConfig 里配好的科室布隆过滤器
    private final RBloomFilter<Long> deptBloomFilter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addDepartment(DepartmentFormReq req) {
        // 转换 DTO -> PO
        Department dept = new Department();
        BeanUtil.copyProperties(req, dept);
        if (dept.getParentId() == null) dept.setParentId(0L); // 默认顶级

        // 插入 MySQL
        departmentMapper.insert(dept);

        // 同步更新布隆过滤器
        deptBloomFilter.add(dept.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDepartment(Long id) {
        // 如果有子科室不允许直接删除父级
        Long childrenCount = departmentMapper.selectCount(
                new LambdaQueryWrapper<Department>().eq(Department::getParentId, id)
        );
        if (childrenCount > 0) {
            throw new RuntimeException("该科室下包含子科室，无法删除");
        }

        // 逻辑删除 (MyBatis Plus 会自动处理 is_deleted=1)
        departmentMapper.deleteById(id);

    }

    @Override
    public List<DepartmentVO> selectAllDepartments() {
        List<Department> departments = departmentMapper.selectList(null);
        return departments.stream().map(department -> {
            DepartmentVO vo = new DepartmentVO();
            BeanUtils.copyProperties(department, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}
