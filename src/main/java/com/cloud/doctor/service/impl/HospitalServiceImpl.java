package com.cloud.doctor.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.doctor.entity.Department;
import com.cloud.doctor.entity.Doctor;
import com.cloud.doctor.entity.Schedule;
import com.cloud.doctor.entity.vo.DepartmentVO;
import com.cloud.doctor.entity.vo.DoctorVO;
import com.cloud.doctor.entity.vo.ScheduleVO;
import com.cloud.doctor.mapper.DepartmentMapper;
import com.cloud.doctor.mapper.DoctorMapper;
import com.cloud.doctor.mapper.ScheduleMapper;
import com.cloud.doctor.service.HospitalService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HospitalServiceImpl implements HospitalService {

    private final DepartmentMapper departmentMapper;

    private final DoctorMapper doctorMapper;

    private final ScheduleMapper scheduleMapper;

    private final StringRedisTemplate redisTemplate;

    private final RedissonClient redissonClient;

    private final RBloomFilter<Long> deptBloomFilter;


    @Override
    public List<DepartmentVO> listDeptTree() {
        //查询所有科室
        List<Department> departments = departmentMapper.selectList(null);
        //把类型变成vo
        List<DepartmentVO> departmentVOS = BeanUtil.copyToList(departments, DepartmentVO.class);
        //变成map结构
        Map<Object, List<DepartmentVO>> collect = departmentVOS.stream().collect(Collectors.groupingBy(DepartmentVO::getParentId));
        //根据0 节点分类
        List<DepartmentVO> roots = collect.getOrDefault(0L, new ArrayList<>());
        //子节点挂载
        for (DepartmentVO root : roots) {
            root.setChildren(collect.get(root.getId()));
        }

        return roots;
    }

    /*@Override
    public List<DoctorVO> listDoctors(Long deptId) {
        List<Doctor> doctors = doctorMapper.selectList(new LambdaQueryWrapper<Doctor>()
                .eq(Doctor::getDeptId, deptId)
                .eq(Doctor::getStatus, 1));

        String name = departmentMapper.selectById(deptId).getName();
        return doctors.stream().map(doctor -> {
            DoctorVO doctorVO = new DoctorVO();
            BeanUtil.copyProperties(doctor, doctorVO);
            doctorVO.setDeptName(name);
            return doctorVO;
        }).collect(Collectors.toList());
    }*/
    // 注入
    /* private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final RBloomFilter<Long> deptBloomFilter;
    */

    @Override
    public List<DoctorVO> listDoctors(Long deptId) {
        // 布隆过滤器拦截
        if (!deptBloomFilter.contains(deptId)) {
            return Collections.emptyList();
        }

        String key = "hospital:doctor:" + deptId;

        // 查缓存
        String json = redisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(json)) {
            // 空值缓存处理
            if ("".equals(json)) {
                return Collections.emptyList();
            }
            // 命中缓存，反序列化返回
            return JSONUtil.toList(json, DoctorVO.class);
        }

        // 缓存未命中，准备查库，上分布式锁，单行
        RLock lock = redissonClient.getLock("lock:doctor:" + deptId);

        try {
            // 尝试加锁：等待 5秒，锁自动过期 10秒
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                try {
                    // 加锁后检查redis是否更新
                    json = redisTemplate.opsForValue().get(key);
                    if (StringUtils.hasText(json)) {
                        if ("".equals(json)) return Collections.emptyList();
                        return JSONUtil.toList(json, DoctorVO.class);
                    }

                    // 数据库查询
                    // 先查科室名称
                    Department dept = departmentMapper.selectById(deptId);
                    String deptName = (dept != null) ? dept.getName() : "未知科室";

                    // 查医生列表
                    List<Doctor> doctors = doctorMapper.selectList(new LambdaQueryWrapper<Doctor>()
                            .eq(Doctor::getDeptId, deptId)
                            .eq(Doctor::getStatus, 1)); // 只查在职

                    // 组装 VO
                    List<DoctorVO> resultList = doctors.stream().map(doc -> {
                        DoctorVO vo = new DoctorVO();
                        BeanUtil.copyProperties(doc, vo);
                        vo.setDeptName(deptName);
                        return vo;
                    }).collect(Collectors.toList());

                    // 写缓存
                    if (resultList.isEmpty()) {
                        // 数据库也没数据就存空值，过期时间2分钟
                        redisTemplate.opsForValue().set(key, "", 2, TimeUnit.MINUTES);
                    } else {
                        // 防雪崩 设置随机过期时间 (30分钟 + 随机 0-5分钟)
                        long timeout = 30 + new Random().nextInt(5);
                        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(resultList), timeout, TimeUnit.MINUTES);
                    }

                    return resultList;
                } finally {
                    // 释放锁
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                // 没抢到锁，说明有人正在查库，等待，递归重试 (自旋)
                Thread.sleep(50);
                return listDoctors(deptId);
            }
        } catch (InterruptedException e) {
            // 恢复中断状态
            Thread.currentThread().interrupt();
            throw new RuntimeException("服务器繁忙，请稍后重试");
        }
    }

    @Override
    public List<ScheduleVO> getScheduleList(Long doctorId) {
        List<Schedule> schedules = scheduleMapper.selectList(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getDoctorId, doctorId)
                .ge(Schedule::getWorkDate, LocalDate.now())
                .eq(Schedule::getStatus, 1)
                .orderByAsc(Schedule::getWorkDate));

        return schedules.stream().map(schedule -> {
            ScheduleVO scheduleVO = new ScheduleVO();
            BeanUtil.copyProperties(schedule, scheduleVO);
            //转换星期
            scheduleVO.setDayOfWeek(schedule.getWorkDate().getDayOfWeek()
                    .getDisplayName(TextStyle.FULL, Locale.CHINESE));
            return scheduleVO;
        }).collect(Collectors.toList());
    }


}
