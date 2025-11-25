package com.cloud.doctor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.doctor.entity.Schedule;
import com.cloud.doctor.service.ScheduleService;
import com.cloud.doctor.mapper.ScheduleMapper;
import org.springframework.stereotype.Service;

/**
* @author 10533
* @description 针对表【bus_schedule(医生排班号源表)】的数据库操作Service实现
* @createDate 2025-11-20 22:59:12
*/
@Service
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, Schedule>
    implements ScheduleService{

}




