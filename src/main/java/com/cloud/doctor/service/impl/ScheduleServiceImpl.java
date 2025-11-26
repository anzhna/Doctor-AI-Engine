package com.cloud.doctor.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.doctor.entity.Schedule;
import com.cloud.doctor.entity.vo.ScheduleVO;
import com.cloud.doctor.service.ScheduleService;
import com.cloud.doctor.mapper.ScheduleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
* @author 10533
* @description 针对表【bus_schedule(医生排班号源表)】的数据库操作Service实现
* @createDate 2025-11-20 22:59:12
*/
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, Schedule>
    implements ScheduleService{

}




