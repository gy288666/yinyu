package com.yinyu.job;

import com.yinyu.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时关闭任务：每分钟扫描超过支付截止时间（下单后 15 分钟，可配置）的待支付订单置为超时关闭
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final OrderService orderService;

    @Scheduled(fixedRate = 60 * 1000, initialDelay = 30 * 1000)
    public void closeExpiredOrders() {
        try {
            int closed = orderService.closeExpired();
            if (closed > 0) {
                log.info("超时关闭订单 {} 笔", closed);
            }
        } catch (Exception e) {
            log.warn("订单超时关闭任务异常: {}", e.getMessage());
        }
    }
}
