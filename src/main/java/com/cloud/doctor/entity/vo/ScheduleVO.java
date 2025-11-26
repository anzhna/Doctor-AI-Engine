package com.cloud.doctor.entity.vo;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ScheduleVO {
    private Long id;            // 排班ID (核心！提交订单要传这个)
    private LocalDate workDate; // 哪天上班
    private String dayOfWeek;   // 星期几 (方便前端展示)
    private Integer shiftType;  // 1-上午, 2-下午, 3-晚班
    private Integer totalQuota; // 总号源
    private Integer remainingQuota; // 剩余号源 (前端要用来显示 "余号: 20")
    private Integer status;     // 1-正常, 0-停诊
}
