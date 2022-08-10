package moe.ahao.commerce.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 售后单状态变化枚举
 */
@Getter
@AllArgsConstructor
public enum AfterSaleStatusChangeEnum {
    //售后单已创建 0 未创建 -> 10 提交申请
    AFTER_SALE_CREATED(AfterSaleStatusEnum.UN_CREATED, AfterSaleStatusEnum.COMMITTED, AfterSaleOperateTypeEnum.NEW_AFTER_SALE, "initiate_after_sale"),
    //缺品售后单已创建 0 未创建 -> 20 审核通过
    LACK_AFTER_SALE_CREATED(AfterSaleStatusEnum.UN_CREATED, AfterSaleStatusEnum.REVIEW_PASS, AfterSaleOperateTypeEnum.NEW_LACK_AFTER_SALE, "cancel_order"),
    //取消订单售后单已创建 0 未创建 -> 20 审核通过
    CANCEL_AFTER_SALE_CREATED(AfterSaleStatusEnum.UN_CREATED, AfterSaleStatusEnum.REVIEW_PASS, AfterSaleOperateTypeEnum.NEW_CANCEL_AFTER_SALE, "cancel_order"),
    //售后单已审核通过 10 提交申请 -> 20 审核通过
    AFTER_SALE_REVIEWED_PASS(AfterSaleStatusEnum.COMMITTED, AfterSaleStatusEnum.REVIEW_PASS, AfterSaleOperateTypeEnum.REVIEW_AFTER_SALE_PASS, "audit_pass"),
    //售后单已审核拒绝 10 提交申请 -> 30 审核拒绝
    AFTER_SALE_REVIEWED_REJECTION(AfterSaleStatusEnum.COMMITTED, AfterSaleStatusEnum.REVIEW_REJECTED, AfterSaleOperateTypeEnum.REVIEW_AFTER_SALE_REJECTION, "audit_reject"),
    //售后单已撤销 10 提交申请 -> 127 撤销申请
    AFTER_SALE_REVOKED(AfterSaleStatusEnum.COMMITTED, AfterSaleStatusEnum.REVOKE, AfterSaleOperateTypeEnum.REVOKE_AFTER_SALE, "revoke_after_sale"),
    //售后单退款中 20 审核通过 -> 40 退款中
    AFTER_SALE_REFUNDING(AfterSaleStatusEnum.REVIEW_PASS, AfterSaleStatusEnum.REFUNDING, AfterSaleOperateTypeEnum.AFTER_SALE_REFUNDING, "refunding"),
    //售后单退款成功 40 退款中 -> 50 退款成功 如果退款失败，在状态机内部有流转记录
    AFTER_SALE_REFUNDED(AfterSaleStatusEnum.REFUNDING, AfterSaleStatusEnum.REFUNDED, AfterSaleOperateTypeEnum.AFTER_SALE_REFUNDED, "refund_success"),
    //售后单退款失败 40 退款中 -> 60 退款失败
    AFTER_SALE_REFUND_FAILED(AfterSaleStatusEnum.REFUNDING, AfterSaleStatusEnum.FAILED, AfterSaleOperateTypeEnum.AFTER_SALE_REFUND_FAIL, false),

    // TODO 兼容处理
    // AFTER_SALE_REVOKE(AfterSaleStatusEnum.COMMITTED, AfterSaleStatusEnum.REVOKE, "售后单撤销"),
    // AFTER_SALE_CUSTOMER_AUDIT_PASS(AfterSaleStatusEnum.COMMITTED, AfterSaleStatusEnum.REVIEW_PASS, "客服审核通过"),
    // AFTER_SALE_CUSTOMER_AUDIT_REJECT(AfterSaleStatusEnum.COMMITTED, AfterSaleStatusEnum.REVIEW_REJECTED, "客服审核拒绝"),
    // AFTER_SALE_PAYMENT_CALLBACK_PASS(AfterSaleStatusEnum.REFUNDING, AfterSaleStatusEnum.REFUNDED, "三方支付系统回调退款成功"),
    // AFTER_SALE_PAYMENT_CALLBACK_FAILED(AfterSaleStatusEnum.REFUNDING, AfterSaleStatusEnum.FAILED, "三方支付系统回调退款失败"),
    ;
    private final AfterSaleStatusEnum fromStatus;
    private final AfterSaleStatusEnum toStatus;
    private final AfterSaleOperateTypeEnum operateType;
    private String tags;
    private boolean sendEvent;

    AfterSaleStatusChangeEnum(AfterSaleStatusEnum fromStatus, AfterSaleStatusEnum toStatus, AfterSaleOperateTypeEnum operateType, String tags) {
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.operateType = operateType;
        this.tags = tags;
        this.sendEvent = true;
    }

    AfterSaleStatusChangeEnum(AfterSaleStatusEnum fromStatus, AfterSaleStatusEnum toStatus, AfterSaleOperateTypeEnum operateType, boolean sendEvent) {
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.operateType = operateType;
        this.sendEvent = sendEvent;
    }
}
