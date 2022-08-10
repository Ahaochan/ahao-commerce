DROP TABLE IF EXISTS `market_coupon`;
CREATE TABLE `market_coupon`
(
    `id`               bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `coupon_id`        varchar(50) DEFAULT NULL COMMENT '优惠券ID',
    `coupon_config_id` varchar(50) NOT NULL COMMENT '优惠券配置ID',
    `user_id`          varchar(50) NOT NULL COMMENT '用户ID',
    `is_used`          tinyint(4) NOT NULL COMMENT '是否使用过这个优惠券，1：使用了，0：未使用',
    `used_time`        datetime             DEFAULT NULL COMMENT '使用优惠券的时间',
    `amount`           int(10)     NOT NULL COMMENT '抵扣金额',
    `create_time`      datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_coupon_id_user_id` (`coupon_config_id`, `user_id`),
    KEY `idx_market_coupon_user_id` (`user_id`)
) ENGINE = InnoDB COMMENT ='优惠券领取记录表';

DROP TABLE IF EXISTS `market_coupon_config`;
CREATE TABLE `market_coupon_config`
(
    `id`               bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `coupon_config_id` varchar(50)   NOT NULL COMMENT '优惠券配置ID',
    `name`             varchar(1024) NOT NULL COMMENT '优惠券名称',
    `type`             tinyint(4)    NOT NULL COMMENT '优惠券类型，1：现金券，2：满减券',
    `amount`           int(10)       NOT NULL COMMENT '优惠券抵扣金额',
    `condition_amount` int(10)       NOT NULL COMMENT '订单满多少金额才可以使用',
    `valid_start_time` datetime      NOT NULL COMMENT '有效期开始时间',
    `valid_end_time`   datetime      NOT NULL COMMENT '有效期结束时间',
    `give_out_count`   bigint(20)    NOT NULL COMMENT '优惠券发行数量',
    `received_count`   bigint(20)    NOT NULL COMMENT '优惠券已经领取的数量',
    `give_out_type`    tinyint(4)    NOT NULL COMMENT '优惠券发放方式，1：可发放可领取，2：仅可发放，3：仅可领取',
    `status`           tinyint(4)    NOT NULL COMMENT '优惠券状态，1：未开始；2：发放中，3：已发完；4：已过期',
    `create_time`      datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT ='优惠券表';

DROP TABLE IF EXISTS `market_freight_template`;
CREATE TABLE `market_freight_template`
(
    `id`               bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `template_id`      varchar(50) NOT NULL COMMENT '模板ID',
    `name`             varchar(50) NOT NULL COMMENT '模板名称',
    `region_id`        varchar(50) NOT NULL COMMENT '区域ID',
    `shipping_amount`  int(10)     NOT NULL COMMENT '标准运费',
    `condition_amount` int(10)     NOT NULL COMMENT '订单满多少钱则免运费',
    `create_time`      datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_region_id` (`region_id`)
) ENGINE = InnoDB COMMENT ='运费模板';

DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log`
(
    `id`            bigint(20)   NOT NULL AUTO_INCREMENT,
    `branch_id`     bigint(20)   NOT NULL,
    `xid`           varchar(100) NOT NULL,
    `context`       varchar(128) NOT NULL,
    `rollback_info` longblob     NOT NULL,
    `log_status`    int(11)      NOT NULL,
    `log_created`   datetime     NOT NULL,
    `log_modified`  datetime     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB;

DROP TABLE IF EXISTS `member_point`;
CREATE TABLE `member_point` (
    `id`           bigint      NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `user_id`      varchar(20) NOT NULL COMMENT '用户id',
    `point`        int         NOT NULL COMMENT '会员积分',
    `create_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_member_point_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT='会员中心的会员积分表';

-- ----------------------------
-- Table structure for membership_member_point
-- ----------------------------
DROP TABLE IF EXISTS `member_point_detail`;
CREATE TABLE `member_point_detail` (
    `id`              bigint      NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `user_id`         varchar(20) NOT NULL COMMENT '用户id',
    `member_point_id` bigint      NOT NULL COMMENT '会员积分ID',
    `old_point`       int         NOT NULL COMMENT '本次变动之前的会员积分',
    `updated_point`   int         NOT NULL COMMENT '本次变动的会员积分',
    `new_point`       int         NOT NULL COMMENT '本次变动之后的会员积分',
    `create_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='会员积分变动的明细表';

INSERT INTO `market_coupon`VALUES (6, '1001001', '2001001', '100', 1, '2021-12-12 12:15:03', 500, '2021-11-26 14:29:43', '2021-12-12 12:15:03');
INSERT INTO `market_coupon`VALUES (9, '1001002', '2001001', '101', 0, '2021-12-08 21:15:34', 500, '2021-11-26 14:29:43', '2021-12-08 21:15:34');
INSERT INTO `market_coupon`VALUES (10, '1001003', '2001001', '102', 1, '2021-12-08 22:58:56', 500, '2021-11-26 14:29:43', '2021-12-08 22:58:56');
INSERT INTO `market_coupon`VALUES (11, '1001004', '2001001', '103', 1, '2021-12-09 20:11:16', 500, '2021-11-26 14:29:43', '2021-12-09 20:11:16');
INSERT INTO `market_coupon`VALUES (12, '1001005', '2001001', '104', 1, '2021-12-10 11:36:47', 500, '2021-11-26 14:29:43', '2021-12-10 11:36:47');
INSERT INTO `market_coupon`VALUES (13, '1001006', '2001001', '105', 1, '2021-12-10 12:23:57', 500, '2021-11-26 14:29:43', '2021-12-10 12:23:57');
INSERT INTO `market_coupon_config`VALUES (2, '2001001', '测试优惠券', 2, 500, 1000, '2021-11-01 12:24:29', '2024-06-30 12:24:35', 1000, 1, 1, 2, '2021-11-28 12:25:17', '2021-11-28 12:25:29');
