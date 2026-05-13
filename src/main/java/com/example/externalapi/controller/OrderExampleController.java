package com.example.externalapi.controller;

import com.example.externalapi.common.response.ApiResponse;
import com.example.externalapi.common.page.PageResult;
import com.example.externalapi.dto.order.CreateOrderRequest;
import com.example.externalapi.dto.order.CreateOrderResponse;
import com.example.externalapi.dto.order.OrderPageItem;
import com.example.externalapi.dto.order.OrderPageQuery;
import com.example.externalapi.service.OrderExampleService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/example/orders")
public class OrderExampleController {

    private final OrderExampleService orderExampleService;

    public OrderExampleController(OrderExampleService orderExampleService) {
        this.orderExampleService = orderExampleService;
    }

    @Operation(summary = "Create order with idempotency")
    @PostMapping
    public ApiResponse<CreateOrderResponse> createOrder(
            @NotBlank(message = "Idempotency-Key is required")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(orderExampleService.createOrder(idempotencyKey, request));
    }

    @Operation(summary = "Page query orders")
    @PostMapping("/page")
    public ApiResponse<PageResult<OrderPageItem>> pageOrders(@Valid @RequestBody OrderPageQuery query) {
        return ApiResponse.success(orderExampleService.pageOrders(query));
    }
}
