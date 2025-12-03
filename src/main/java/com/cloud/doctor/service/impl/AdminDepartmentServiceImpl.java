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
        // 1. 转换 DTO -> PO
        Department dept = new Department();
        BeanUtil.copyProperties(req, dept);
        if (dept.getParentId() == null) dept.setParentId(0L); // 默认顶级

        // 2. 插入 MySQL
        departmentMapper.insert(dept);

        // 3. 🔥【关键】同步更新布隆过滤器
        // 如果不加这一步，新科室 ID 在 C 端会被当成“非法攻击”直接拦截！
        deptBloomFilter.add(dept.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDepartment(Long id) {
        // 1. 校验：如果有子科室，不允许直接删除父级
        Long childrenCount = departmentMapper.selectCount(
                new LambdaQueryWrapper<Department>().eq(Department::getParentId, id)
        );
        if (childrenCount > 0) {
            throw new RuntimeException("该科室下包含子科室，无法删除");
        }

        // 2. 逻辑删除 (MyBatis Plus 会自动处理 is_deleted=1)
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
