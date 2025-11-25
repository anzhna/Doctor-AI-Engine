package com.cloud.doctor.entity.vo;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class DepartmentVO {
    private Long id;
    private String name;
    private Long parentId;
    private Integer level;
    // 子科室列表 (树形结构的核心)
    private List<DepartmentVO> children = new ArrayList<>();
}