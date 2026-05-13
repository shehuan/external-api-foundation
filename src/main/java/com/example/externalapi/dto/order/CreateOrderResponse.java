package com.example.externalapi.dto.order;

public record CreateOrderResponse(
        Long orderId,
        String requestNo
) {
}
