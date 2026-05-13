CREATE TABLE demo_order (
    order_id BIGINT NOT NULL AUTO_INCREMENT,
    request_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(18, 2) NOT NULL,
    create_time DATETIME NULL,
    update_time DATETIME NULL,
    create_by BIGINT NULL,
    update_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (order_id),
    UNIQUE KEY uk_demo_order_request_no (request_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Demo order table for idempotency example';
