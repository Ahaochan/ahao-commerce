package moe.ahao.commerce.common.constants;

/**
 * RocketMQ 常量类
 */
public class RocketMqConstant {
    public static final String ORDER_FORWARD_TOPIC = "order-forward";
    public static final String ORDER_FORWARD_GROUP = "order-forward-group";
    public static final String DEFAULT_ORDER_BINLOG = "default-order-binlog";
    public static final String DEFAULT_ORDER_BINLOG_GROUP = "default-order-binlog-group";
    public static final String ORDER_REVERSE = "order-reverse";
    public static final String ORDER_REVERSE_GROUP = "order-reverse-group";
    /**
     * 完成订单创建发送事务消息 topic
     */
    public static String CREATE_ORDER_SUCCESS_TOPIC = "create_order_success_topic";

    /**
     * 完成订单创建 consumer 分组
     */
    public static String CREATE_ORDER_SUCCESS_CONSUMER_GROUP = "create_order_success_consumer_group";

    /**
     * 默认的producer分组
     */
    public static String ORDER_DEFAULT_PRODUCER_GROUP = "order_default_producer_group";

    /**
     * 支付订单成功 producer 分组
     */
    public static String PAID_ORDER_SUCCESS_PRODUCER_GROUP = "paid_order_success_producer_group";

    /**
     * 支付订单超时自动关单发送延迟消息 topic
     */
    public final static String PAY_ORDER_TIMEOUT_DELAY_TOPIC = "pay_order_timeout_delay_topic";

    /**
     * 支付订单超时自动关单 consumer 分组
     */
    public final static String PAY_ORDER_TIMEOUT_DELAY_CONSUMER_GROUP = "pay_order_timeout_delay_consumer_group";

    /**
     * 完成订单支付发送普通消息 topic
     */
    public static String PAID_ORDER_SUCCESS_TOPIC = "paid_order_success_topic";

    /**
     * 完成订单支付 consumer 分组
     */
    public static String PAID_ORDER_SUCCESS_CONSUMER_GROUP = "paid_order_success_consumer_group";

    /**
     * 触发订单履约发送事务消息 topic
     */
    public static String TRIGGER_ORDER_FULFILL_TOPIC = "trigger_order_fulfill_topic";

    /**
     * 触发订单履约 producer 分组
     */
    public static String TRIGGER_ORDER_FULFILL_PRODUCER_GROUP = "trigger_order_fulfill_producer_group";

    /**
     * 触发订单履约 consumer 分组
     */
    public static String TRIGGER_ORDER_FULFILL_CONSUMER_GROUP = "trigger_order_fulfill_consumer_group";

    /**
     * 取消订单 发送释放权益 topic
     */
    public final static String RELEASE_ASSETS_TOPIC = "release_assets_topic";

    /**
     * 客服审核售后通过 发送释放库存 topic
     */
    public static final String AFTER_SALE_RELEASE_INVENTORY_TOPIC = "after_sale_release_inventory_topic";

    /**
     * 客服审核售后通过 发送释放权益资产 topic
     */
    public static final String AFTER_SALE_RELEASE_PROPERTY_TOPIC = "after_sale_release_property_topic";

    /**
     * 客服审核售后通过 释放优惠券consumer分组
     */
    public static final String AFTER_SALE_RELEASE_PROPERTY_CONSUMER_GROUP = "after_sale_release_property_consumer_group";

    /**
     * 客服审核售后通过 释放库存分组
     */
    public static final String AFTER_SALE_RELEASE_INVENTORY_CONSUMER_GROUP = "after_sale_release_inventory_consumer_group";

    /**
     * 发送实际退款 topic
     */
    public final static String ACTUAL_REFUND_TOPIC = "actual_refund_topic";

    /**
     * 监听实际退款consumer分组
     */
    public final static String ACTUAL_REFUND_CONSUMER_GROUP = "actual_refund_consumer_group";

    /**
     * 监听实际退款producer分组
     */
    public static String ACTUAL_REFUND_PRODUCER_GROUP = "actual_refund_producer_group";

    /**
     * 监听退款请求分组
     */
    public final static String REQUEST_CONSUMER_GROUP = "request_consumer_group";

    /**
     * 监听释放权益资产consumer分组
     */
    public static final String RELEASE_PROPERTY_CONSUMER_GROUP = "release_property_consumer_group";

    /**
     * 监听释放权益资产producer分组
     */
    public static String RELEASE_PROPERTY_PRODUCER_GROUP = "release_property_producer_group";

    /**
     * 监听释放库存分组
     */
    public static final String RELEASE_INVENTORY_CONSUMER_GROUP = "release_inventory_consumer_group";

    /**
     * 监听释放资产consumer分组
     */
    public static String RELEASE_ASSETS_CONSUMER_GROUP = "release_assets_consumer_group";

    /**
     * 监听释放资产producer分组
     */
    public static String RELEASE_ASSETS_PRODUCER_GROUP = "release_assets_producer_group";

    /**
     * 正向订单物流配送结果结果相关的topic信息
     */
    public final static String ORDER_WMS_SHIP_RESULT_TOPIC = "wms_ship_result_topic";

    /**
     * 正向订单物流配送结果结果 consumer 分组
     */
    public final static String ORDER_WMS_SHIP_RESULT_CONSUMER_GROUP = "wms_ship_result_consumer_group";


    /**
     * 售后申请发送给客服审核 topic
     */
    public static final String AFTER_SALE_CUSTOMER_AUDIT_TOPIC = "after_sale_customer_audit_topic";

    /**
     * 监听客服审核申请分组
     */
    public static final String AFTER_SALE_CUSTOMER_AUDIT_GROUP = "after_sale_customer_audit_group";

    /**
     * 客服审核通过后发送释放资产 topic
     */
    public final static String CUSTOMER_AUDIT_PASS_RELEASE_ASSETS_TOPIC = "customer_audit_pass_release_assets_topic";

    /**
     * 客服审核通过后监听释放资产consumer分组
     */
    public final static String CUSTOMER_AUDIT_PASS_RELEASE_ASSETS_CONSUMER_GROUP = "customer_audit_pass_release_assets_consumer_group";

    /**
     * 客服审核通过后监听释放资产producer分组
     */
    public static String CUSTOMER_AUDIT_PASS_RELEASE_ASSETS_PRODUCER_GROUP = "customer_audit_pass_release_assets_producer_group";

    /**
     * 缺品处理 producer 分组
     */
    public static String LACK_ITEM_PRODUCER_GROUP = "lack_item_producer_group";


    /**
     * 订单正向标准变更消息 topic
     */
    public final static String ORDER_STD_CHANGE_EVENT_TOPIC = "order_std_change_event_topic";

    /**
     * 订单正向标准变更消息 producer分组
     */
    public static String ORDER_STD_CHANGE_EVENT_PRODUCT_GROUP = "order_std_change_producer_group";

    /**
     * 营销系统对订单正向标准变更消息 consumer分组
     */
    public static final String MARKET_ORDER_STD_CHANGE_EVENT_CONSUMER_GROUP = "market_order_std_change_consumer_group";

    /**
     * 订单系统对订单正向标准变更消息 consumer分组
     */
    public static final String ORDER_STD_CHANGE_EVENT_CONSUMER_GROUP = "order_std_change_consumer_group";
}
