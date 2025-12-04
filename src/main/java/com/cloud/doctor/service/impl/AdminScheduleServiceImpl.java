package com.cloud.doctor.service.impl;

import com.cloud.doctor.entity.Schedule;
import com.cloud.doctor.entity.dto.ScheduleFormReq;
import com.cloud.doctor.entity.vo.ScheduleVO;
import com.cloud.doctor.mapper.ScheduleMapper;
import com.cloud.doctor.service.AdminScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminScheduleServiceImpl implements AdminScheduleService {

    private final ScheduleMapper scheduleMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSchedule(ScheduleFormReq req) {
        // 自动拆分班次(上午+下午)
        int quotaPerShift = req.totalQuota() / 2;

        // 发布上午班
        createAndWarmUp(req.doctorId(), req.workDate(), 1, quotaPerShift);

        // 发布下午班
        createAndWarmUp(req.doctorId(), req.workDate(), 2, quotaPerShift);
    }

    @Override
    public List<ScheduleVO> selectAllSchedule() {
        List<Schedule> schedules = scheduleMapper.selectList(null);
        return schedules.stream().map(schedule -> {
            ScheduleVO scheduleVO = new ScheduleVO();
            BeanUtils.copyProperties(schedule, scheduleVO);
            return scheduleVO;
        }).collect(Collectors.toList());
    }


    // 创建单条排班并预热方法

    private void createAndWarmUp(Long doctorId, LocalDate date, Integer shiftType, Integer quota) {
        //组装实体对象
        Schedule schedule = new Schedule();
        schedule.setDoctorId(doctorId);
        schedule.setWorkDate(date);
        schedule.setShiftType(shiftType); // 1上午, 2下午
        schedule.setTotalQuota(quota);
        schedule.setRemainingQuota(quota); // 初始剩余 = 总量
        schedule.setVersion(0); // 乐观锁初始版本
        schedule.setStatus(1);  // 1-正常

        //MySQL
        scheduleMapper.insert(schedule);
        Long scheduleId = schedule.getId();

        //自动预热 Redis 库存
        // Key 格式和 Lua 脚本里保持一致：schedule:stock:{id}
        String stockKey = "schedule:stock:" + scheduleId;
        redisTemplate.opsForValue().set(stockKey, quota.toString());

        log.info("排班发布成功：id={}, 医生={}, 日期={}, 班次={}, 库存预热={}",
                scheduleId, doctorId, date, shiftType, quota);
    }
}