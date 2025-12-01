package com.cloud.doctor.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.doctor.entity.Department;
import com.cloud.doctor.mapper.DepartmentMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Configuration
public class BloomFilterConfig {

    private final RedissonClient redissonClient;
    private final DepartmentMapper departmentMapper;

    public BloomFilterConfig(RedissonClient redissonClient, DepartmentMapper departmentMapper) {
        this.redissonClient = redissonClient;
        this.departmentMapper = departmentMapper;
    }

    @Bean
    public RBloomFilter<Long> deptBloomFilter() {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("bloom:dept:id");
        // 初始化：预计 1万数据，误差率 3%
        bloomFilter.tryInit(10000L, 0.03);
        return bloomFilter;
    }

    @PostConstruct
    public void init() {
        // 预热：把所有科室ID塞进去
        List<Department> list = departmentMapper.selectList(new LambdaQueryWrapper<Department>().select(Department::getId));
        RBloomFilter<Long> bloomFilter = deptBloomFilter();
        for (Department dept : list) {
            bloomFilter.add(dept.getId());
        }
        log.info(">>> 布隆过滤器预热完成，加载科室: {} 个", list.size());
    }
}
