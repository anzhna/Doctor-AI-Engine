package com.cloud.doctor.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "管理端-科室新增/修改请求")
public record DepartmentFormReq(
        @Schema(description = "科室名称", example = "眼科")
        String name,

        @Schema(description = "科室编码", example = "EYE")
        String code,

        @Schema(description = "父级ID (顶级为0)", example = "100")
        Long parentId,

        @Schema(description = "级别 (1-大类, 2-科室)", example = "2")
        Integer level,

        @Schema(description = "描述", example = "眼部疾病治疗")
        String description
) {}