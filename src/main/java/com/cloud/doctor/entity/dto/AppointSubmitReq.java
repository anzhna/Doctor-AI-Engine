package com.cloud.doctor.entity.dto;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "挂号接口")
public record AppointSubmitReq(
        @Schema(description = "排班ID (即你要挂哪一天的哪个班)", example = "1")
        Long scheduleId
) {}
