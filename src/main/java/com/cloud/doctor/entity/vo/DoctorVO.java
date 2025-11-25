package com.cloud.doctor.entity.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DoctorVO {
    private Long id;
    private String realName;
    private String title; // 职称
    private String intro; // 简介
    private BigDecimal consultPrice; // 挂号费
    private String deptName; // 冗余一个科室名称，方便前端展示
}