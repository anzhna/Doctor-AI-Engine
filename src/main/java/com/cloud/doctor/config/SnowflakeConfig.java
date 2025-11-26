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

    // 机器ID (DataCenterId) 和 终端ID (WorkerId),暂时写死为 1, 1
    // 在分布式系统中，这两个 ID 需要根据服务器 IP 自动生成。
    private long workerId = 1;
    private long datacenterId = 1;

    @Bean
    public Snowflake snowflake() {
        // 使用 Hutool 工具包构建雪花算法对象
        return IdUtil.getSnowflake(workerId, datacenterId);
    }
}
