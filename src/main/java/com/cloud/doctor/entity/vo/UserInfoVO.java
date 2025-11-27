package com.cloud.doctor.entity.vo;

import lombok.Data;

@Data
public class UserInfoVO {
    private Long id;
    private String username;
    private String realName;
    private String phone; // 建议脱敏处理，如 138****1234
    private String email;
    private Integer age;
    private Integer sex;
}
