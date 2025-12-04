package com.cloud.doctor.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 雪花算法配置类
 * 作用：为整个系统提供高性能的唯一 ID 生成器
 */
@Configuration
public class SnowflakeConfig {

    // 机器ID和终端ID暂时写死为 1, 1
    private long workerId = 1;
    private long datacenterId = 1;

    @Bean
    public Snowflake snowflake() {
        // Hutool工具包构建雪花算法对象
        return IdUtil.getSnowflake(workerId, datacenterId);
    }
}
