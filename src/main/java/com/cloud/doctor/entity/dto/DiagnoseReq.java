package com.cloud.doctor.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "智能问诊请求")
public record DiagnoseReq(
        @Schema(description = "症状描述", example = "我昨天吃坏了肚子，现在恶心想吐，还一直拉肚子")
        String symptom
) {}