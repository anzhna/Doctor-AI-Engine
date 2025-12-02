package com.cloud.doctor.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.doctor.entity.Department;
import com.cloud.doctor.entity.Schedule;
import com.cloud.doctor.mapper.DepartmentMapper;
import com.cloud.doctor.mapper.ScheduleMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Configuration
public class BloomFilterConfig {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private DepartmentMapper departmentMapper;
    @Autowired
    private ScheduleMapper scheduleMapper; // 记得注入这个

    // 1. 定义 Bean：科室布隆过滤器
    @Bean
    public RBloomFilter<Long> deptBloomFilter() {
        // 这里的逻辑和 init 里重复没关系，Redisson 的 tryInit 是幂等的（存在就不初始化）
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("bloom:dept:id");
        bloomFilter.tryInit(10000L, 0.03);
        return bloomFilter;
    }

    // 2. 定义 Bean：排班布隆过滤器
    @Bean
    public RBloomFilter<Long> scheduleBloomFilter() {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("bloom:schedule:id");
        bloomFilter.tryInit(100000L, 0.03);
        return bloomFilter;
    }

    // 3. 预热数据 (解决循环依赖的关键：不要调用上面的方法，直接用 redissonClient 操作)
    @PostConstruct
    public void init() {
        // --- 预热科室 ---
        List<Department> depts = departmentMapper.selectList(new LambdaQueryWrapper<Department>().select(Department::getId));
        // ✅ 修改点：直接从 client 获取，不走 Spring 代理
        RBloomFilter<Long> deptFilter = redissonClient.getBloomFilter("bloom:dept:id");
        deptFilter.tryInit(10000L, 0.03); // 确保初始化
        for (Department dept : depts) {
            deptFilter.add(dept.getId());
        }
        log.info(">>> 科室布隆过滤器预热完成，数量：{}", depts.size());
    }
}
