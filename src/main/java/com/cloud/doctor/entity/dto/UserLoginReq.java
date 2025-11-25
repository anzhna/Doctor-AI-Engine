package com.cloud.doctor.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户登录请求")
public record UserLoginReq(
        @Schema(description = "手机号", example = "13800138000")
        String phone,
        @Schema(description = "密码", example = "123456")
        String password
){}
