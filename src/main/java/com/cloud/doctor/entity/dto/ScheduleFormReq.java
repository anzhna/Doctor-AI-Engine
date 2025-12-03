package com.cloud.doctor.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "排班发布请求")
public record ScheduleFormReq(
        @Schema(description = "医生ID", example = "1")
        Long doctorId,

        @Schema(description = "排班日期", example = "2025-12-01")
        LocalDate workDate,

        @Schema(description = "总号源数量", example = "30")
        Integer totalQuota
) {}
