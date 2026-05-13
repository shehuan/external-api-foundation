package com.example.externalapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.externalapi.common.error.ErrorCode;
import com.example.externalapi.common.exception.BizException;
import com.example.externalapi.common.page.PageResult;
import com.example.externalapi.dto.order.CreateOrderRequest;
import com.example.externalapi.dto.order.CreateOrderResponse;
import com.example.externalapi.dto.order.OrderPageItem;
import com.example.externalapi.dto.order.OrderPageQuery;
import com.example.externalapi.entity.DemoOrderEntity;
import com.example.externalapi.infrastructure.idempotency.IdempotencyCheckResult;
import com.example.externalapi.infrastructure.idempotency.IdempotencyService;
import com.example.externalapi.infrastructure.idempotency.IdempotencyStatus;
import com.example.externalapi.infrastructure.security.replay.SignatureUtils;
import com.example.externalapi.mapper.DemoOrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderExampleService {

    private static final String BIZ_TYPE_CREATE_ORDER = "CREATE_ORDER";

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;
    private final DemoOrderMapper demoOrderMapper;

    public OrderExampleService(IdempotencyService idempotencyService, ObjectMapper objectMapper,
            DemoOrderMapper demoOrderMapper) {
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
        this.demoOrderMapper = demoOrderMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public CreateOrderResponse createOrder(String idempotencyKey, CreateOrderRequest request) {
        String requestHash = hashRequest(request);
        IdempotencyCheckResult checkResult = idempotencyService.checkAndStart(
                BIZ_TYPE_CREATE_ORDER, idempotencyKey, requestHash);

        if (!checkResult.firstRequest()) {
            return handleRepeatedRequest(checkResult);
        }

        try {
            DemoOrderEntity order = new DemoOrderEntity();
            order.setRequestNo(idempotencyKey);
            order.setUserId(request.userId());
            order.setAmount(request.amount());
            demoOrderMapper.insert(order);

            CreateOrderResponse response = new CreateOrderResponse(order.getOrderId(), idempotencyKey);
            idempotencyService.markSuccess(BIZ_TYPE_CREATE_ORDER, idempotencyKey, writeResponse(response));
            return response;
        } catch (Exception exception) {
            idempotencyService.markFailed(BIZ_TYPE_CREATE_ORDER, idempotencyKey, exception.getMessage());
            throw exception;
        }
    }

    public PageResult<OrderPageItem> pageOrders(OrderPageQuery query) {
        LambdaQueryWrapper<DemoOrderEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getUserId() != null, DemoOrderEntity::getUserId, query.getUserId())
                .eq(hasText(query.getRequestNo()), DemoOrderEntity::getRequestNo, query.getRequestNo())
                .orderByDesc(DemoOrderEntity::getOrderId);

        Page<DemoOrderEntity> page = demoOrderMapper.selectPage(query.toMybatisPage(), wrapper);
        return PageResult.from(page, OrderPageItem::from);
    }

    private CreateOrderResponse handleRepeatedRequest(IdempotencyCheckResult checkResult) {
        if (checkResult.status() == IdempotencyStatus.SUCCESS) {
            return readResponse(checkResult.responseBody(), CreateOrderResponse.class);
        }
        if (checkResult.status() == IdempotencyStatus.PROCESSING) {
            throw new BizException(ErrorCode.CONFLICT, "Request is processing");
        }
        throw new BizException(ErrorCode.CONFLICT, "Previous request failed");
    }

    private String hashRequest(Object request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            return SignatureUtils.sha256Hex(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to hash request", exception);
        }
    }

    private String writeResponse(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to write idempotency response", exception);
        }
    }

    private <T> T readResponse(String responseBody, Class<T> type) {
        try {
            return objectMapper.readValue(responseBody, type);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read idempotency response", exception);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
