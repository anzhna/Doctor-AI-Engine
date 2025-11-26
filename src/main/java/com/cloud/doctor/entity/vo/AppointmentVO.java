package com.cloud.doctor.entity.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AppointmentVO {
    private Long id;            // 订单主键
    private String orderNo;     // 订单号

    private String doctorName;  // 医生姓名 (需要组装)
    private String deptName;    // 科室姓名 (需要组装)
    private LocalDate workDate;    // 就诊日期 (从排班表查)
    private String shiftDesc;   // 班次 (上午/下午)

    private BigDecimal fee;     // 费用
    private Integer status;     // 0-待支付, 1-已支付, 2-已取消
    private String statusDesc;  // 状态描述 (方便前端展示)

    private LocalDateTime createTime; // 下单时间
}
