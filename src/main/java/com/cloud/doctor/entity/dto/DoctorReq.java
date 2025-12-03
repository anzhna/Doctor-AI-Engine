package com.cloud.doctor.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "医生接口")
public record DoctorReq (
        @Schema(description = "医生姓名",example = "王胤钢")
        String realName,
        @Schema(description = "所属科室id",example = "2")
        Long deptId,
        @Schema(description = "职称",example = "主治医师")
        String title,
        @Schema(description = "医生简介",example = "傻子")
        String intro,
        @Schema(description = "问诊挂号费",example = "9")
        BigDecimal consultPrice,
        @Schema(description = "状态",example = "1")
        Integer status
){}
