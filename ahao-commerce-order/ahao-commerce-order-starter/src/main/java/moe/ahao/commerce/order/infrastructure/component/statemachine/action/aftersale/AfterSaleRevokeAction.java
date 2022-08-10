package moe.ahao.commerce.order.infrastructure.component.statemachine.action.aftersale;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.aftersale.api.command.RevokeAfterSaleCommand;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.AfterSaleLogDAO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleInfoDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleItemDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleInfoMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleItemMapper;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleRefundMapper;
import moe.ahao.commerce.common.constants.RedisLockKeyConstants;
import moe.ahao.commerce.common.enums.AfterSaleItemTypeEnum;
import moe.ahao.commerce.common.enums.AfterSaleReturnCompletionMarkEnum;
import moe.ahao.commerce.common.enums.AfterSaleStatusChangeEnum;
import moe.ahao.commerce.common.enums.AfterSaleStatusEnum;
import moe.ahao.commerce.order.infrastructure.component.statemachine.action.AfterSaleStateAction;
import moe.ahao.commerce.order.infrastructure.exception.OrderExceptionEnum;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;

/**
 * 撤销售后Action
 */
@Component
@Slf4j
public class AfterSaleRevokeAction extends AfterSaleStateAction<RevokeAfterSaleCommand> {
    @Autowired
    private AfterSaleInfoMapper afterSaleInfoMapper;
    @Autowired
    private AfterSaleItemMapper afterSaleItemMapper;
    @Autowired
    private AfterSaleRefundMapper afterSaleRefundMapper;
    @Autowired
    private AfterSaleLogDAO afterSaleLogDAO;

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Override
    public AfterSaleStatusChangeEnum event() {
        return AfterSaleStatusChangeEnum.AFTER_SALE_REVOKED;
    }

    @Override
    protected String onStateChangeInternal(AfterSaleStatusChangeEnum event, RevokeAfterSaleCommand command) {
        // 1. 参数校验
        String afterSaleId = command.getAfterSaleId();
        if (command == null) {
            throw OrderExceptionEnum.REVOKE_AFTER_SALE_REQUEST_IS_NULL.msg();
        }
        if (StringUtils.isEmpty(afterSaleId)) {
            throw OrderExceptionEnum.AFTER_SALE_ID_IS_NULL.msg();
        }

        // 2. 加分布式锁, 锁整个售后单
        // 2.1. 防并发
        // 2.2. 业务上的考虑, 只要涉及售后表的更新, 就需要加锁, 锁整个售后表, 否则算钱的时候, 就会由于突然撤销, 导致钱多算了
        String lockKey = RedisLockKeyConstants.REFUND_KEY + afterSaleId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = lock.tryLock();
        if (!locked) {
            throw OrderExceptionEnum.AFTER_SALE_CANNOT_REVOKE.msg();
        }

        try {
            // 3. 撤销售后单
            this.invoke(command);
            return afterSaleId;
        } finally {
            // 4. 释放分布式锁
            lock.unlock();
        }
    }

    private String invoke(RevokeAfterSaleCommand command) {
        return transactionTemplate.execute(transactionStatus -> {
            // 1. 参数检查
            String afterSaleId = command.getAfterSaleId();
            AfterSaleInfoDO afterSaleInfo = afterSaleInfoMapper.selectOneByAfterSaleId(afterSaleId);
            this.check(afterSaleInfo);

            // 2. 更新售后单状态为："已撤销"
            afterSaleInfoMapper.updateAfterSaleStatusByAfterSaleId(afterSaleInfo.getAfterSaleId(), AfterSaleStatusEnum.COMMITTED.getCode(), AfterSaleStatusEnum.REVOKE.getCode());

            // 3. 增加售后单操作日志
            afterSaleLogDAO.save(afterSaleInfo, AfterSaleStatusChangeEnum.AFTER_SALE_REVOKED);

            // 4. 更新售后条目的退货完成标记
            /**
             * 售后条目数据格式: 售后条目不同(有优惠券条目、也有运费条目),但是after_sale_id、order_id、sku_code相同,所以可以get(0)
             * after_sale_id        order_id                sku_code    product_name
             * 2022011254135352100, 1022011270929057100,    10101011,   demo商品
             * 2022011254135352100, 1022011270929057100,    10101011,   1001001
             * 2022011254135352100, 1022011270929057100,    10101011,   运费
             */
            List<AfterSaleItemDO> afterSaleItemDOList = afterSaleItemMapper.selectListByAfterSaleId(afterSaleId);
            AfterSaleItemDO afterSaleItem = afterSaleItemDOList.get(0);
            String orderId = afterSaleItem.getOrderId();
            String skuCode = afterSaleItem.getSkuCode();

            // 如果是20，说明此次撤销的是售后条目的最后一笔, 要把return_completion_mark由 "20已全部售后" 更新回 "10未全部售后"
            if (Objects.equals(AfterSaleReturnCompletionMarkEnum.ALL_RETURN_GOODS.getCode(), afterSaleItem.getReturnCompletionMark())) {
                afterSaleItemMapper.updateReturnCompletionMark(orderId, skuCode, AfterSaleReturnCompletionMarkEnum.NOT_ALL_RETURN_GOODS.getCode());
            }
            return afterSaleId;
        });
    }

    private void check(AfterSaleInfoDO afterSaleInfo) {
        // 1. 查询售后单
        if (afterSaleInfo == null) {
            throw OrderExceptionEnum.AFTER_SALE_ID_IS_NULL.msg();
        }

        // 2. 只有提交申请状态才可以撤销
        if (!Objects.equals(AfterSaleStatusEnum.COMMITTED.getCode(), afterSaleInfo.getAfterSaleStatus())) {
            throw OrderExceptionEnum.AFTER_SALE_CANNOT_REVOKE.msg();
        }
        // 3. 业务限制:当前订单已发起尾笔条目的售后, 不能再次撤回非尾笔的售后
        String orderId = afterSaleInfo.getOrderId();
        //  查询该笔订单的优惠券售后单和运费售后单
        // TODO 这里可以优化, 将最后一笔售后单的标记落到afterSaleInfo表里, 就不用这样别扭地查了
        List<AfterSaleItemDO> afterSaleItemCouponAndFreightList = afterSaleItemMapper.selectListByOrderIdAndType(orderId,
            AfterSaleItemTypeEnum.AFTER_SALE_COUPON.getCode(), AfterSaleItemTypeEnum.AFTER_SALE_FREIGHT.getCode());
        if (afterSaleItemCouponAndFreightList.isEmpty()) {
            // 该笔订单还没有优惠券售后单or运费售后单, 说明还没有释放优惠券或退运费, 允许撤销
        } else if (afterSaleInfo.getAfterSaleId().equals(afterSaleItemCouponAndFreightList.get(0).getAfterSaleId())) {
            // 发起撤销申请的售后id和该笔订单的尾笔售后id相同, 尾笔条目自己撤销自己, 允许撤销
        } else {
            // 在已有 尾笔优惠券售后单 or 运费售后单 的前提下, 已申请售后的非尾笔条目不允许撤销
            throw OrderExceptionEnum.CANNOT_REVOKE_AFTER_SALE.msg();
        }
    }
}
