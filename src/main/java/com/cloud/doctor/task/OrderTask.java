package com.cloud.doctor.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.doctor.entity.Appointment;
import com.cloud.doctor.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单定时任务
 * 职责：负责处理订单的超时自动关闭
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTask {

    private final AppointmentService appointmentService;

    /**
     * 每分钟执行一次，检查是否有超时未支付订单
     * Cron表达式：0 0/1 * * * ? (每分钟的第0秒开始执行)
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void checkOrderTimeout() {
        log.info(">>> 开始执行订单超时巡检任务...");

        // 定义超时时间：当前时间 - 15分钟
        // 创建时间早于 expireTime 的，都算超时
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(15);

        // 查询符合条件的订单：Status=0 (未支付) AND CreateTime < expireTime
        List<Appointment> expiredOrders = appointmentService.list(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getStatus, 0)
                .lt(Appointment::getCreateTime, expireTime));

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("发现 {} 个超时订单，准备回收...", expiredOrders.size());

        // 3. 遍历取消
        for (Appointment order : expiredOrders) {
            try {
                // try-catch防止一个订单取消失败导致整个任务中断
                appointmentService.cancelOrder(order.getId());
                log.info("订单 [{}] 已自动取消，库存已释放", order.getOrderNo());
            } catch (Exception e) {
                log.error("订单 [{}] 自动取消失败: {}", order.getOrderNo(), e.getMessage());
            }
        }
    }
}
