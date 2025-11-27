package com.cloud.doctor.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "修改个人信息请求")
public record UserUpdateReq(
        @Schema(description = "真实姓名") String realName,
        @Schema(description = "邮箱") String email,
        @Schema(description = "年龄") Integer age,
        @Schema(description = "性别 1-男 2-女") Integer sex
) {}
