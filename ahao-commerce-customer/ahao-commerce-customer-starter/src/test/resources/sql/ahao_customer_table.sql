DROP TABLE IF EXISTS `customer_receives_after_sales_info`;
CREATE TABLE `customer_receives_after_sales_info`
(
    `id`                   bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`              VARCHAR(20) NOT NULL COMMENT '购买用户ID',
    `order_id`             VARCHAR(50) NOT NULL COMMENT '订单ID',
    `after_sale_id`        BIGINT      NOT NULL COMMENT '售后ID',
    `after_sale_refund_id` BIGINT      NOT NULL COMMENT '售后支付单ID',
    `after_sale_type`      TINYINT     NOT NULL COMMENT '售后类型 1 退款  2 退货',
    `apply_refund_amount`  INT         NOT NULL COMMENT '申请退款金额',
    `return_good_amount`   INT         NOT NULL COMMENT '实际退款金额',
    `gmt_create`           datetime    NOT NULL COMMENT '创建时间',
    `gmt_modified`         datetime    NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_customer_receives_after_sales_info_id` (`after_sale_id`)
) ENGINE = InnoDB COMMENT ='客服接收售后申请信息表';



