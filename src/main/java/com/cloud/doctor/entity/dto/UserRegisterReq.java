package com.cloud.doctor.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户注册请求参数
 * 使用 Java 17 record 特性，自动生成构造器、Getter、ToString，简洁且不可变。
 */
@Schema(description = "用户注册请求参数")
public record UserRegisterReq(
        @Schema(description = "手机号", example = "13800138000")
        String phone,

        @Schema(description = "密码", example = "123456")
        String password,

        @Schema(description = "真实姓名", example = "张三")
        String realName
) {}
