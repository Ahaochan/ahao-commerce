drop table if exists `customer_receives_after_sales_info`;
create table `customer_receives_after_sales_info`
(
    `id`                   bigint(20)  not null auto_increment comment '主键ID',
    `user_id`              varchar(20) not null comment '购买用户ID',
    `order_id`             varchar(50) not null comment '订单ID',
    `after_sale_id`        varchar(50) not null comment '售后ID',
    `after_sale_refund_id` varchar(50) not null comment '售后支付单ID',
    `after_sale_type`      TINYINT     not null comment '售后类型 1 整笔退款 2 售后退货',
    `apply_refund_amount`  INT         not null comment '申请退款金额',
    `return_good_amount`   INT         not null comment '实际退款金额',
    `create_time`          datetime    not null default current_timestamp comment '创建时间',
    `update_time`          datetime    not null default current_timestamp on update current_timestamp comment '更新时间',
    primary key (`id`),
    key `idx_customer_receives_after_sales_info_id` (`after_sale_id`)
) engine = InnoDB;



