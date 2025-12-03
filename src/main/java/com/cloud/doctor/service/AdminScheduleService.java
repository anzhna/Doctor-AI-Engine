package com.cloud.doctor.service;

import com.cloud.doctor.entity.dto.ScheduleFormReq;
import com.cloud.doctor.entity.vo.ScheduleVO;

import java.util.List;

/**
 * 管理端排班服务接口
 * 专门处理后台管理员发布、修改排班的逻辑
 */
public interface AdminScheduleService {

    /**
     * 发布排班 (入库 + Redis预热 + 布隆过滤器同步)
     * @param req 排班表单
     */
    void addSchedule(ScheduleFormReq req);
    //查询所有排班
    List<ScheduleVO> selectAllSchedule();
}
