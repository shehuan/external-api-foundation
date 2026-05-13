package com.example.externalapi.dto.order;

import com.example.externalapi.common.page.PageQuery;

public class OrderPageQuery extends PageQuery {

    private Long userId;

    private String requestNo;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRequestNo() {
        return requestNo;
    }

    public void setRequestNo(String requestNo) {
        this.requestNo = requestNo;
    }
}
