package com.example.externalapi.dto.order;

import com.example.externalapi.entity.DemoOrderEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderPageItem(
        Long orderId,
        String requestNo,
        Long userId,
        BigDecimal amount,
        LocalDateTime createTime
) {

    public static OrderPageItem from(DemoOrderEntity entity) {
        return new OrderPageItem(
                entity.getOrderId(),
                entity.getRequestNo(),
                entity.getUserId(),
                entity.getAmount(),
                entity.getCreateTime());
    }
}
