package com.ruyuan.eshop.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.ruyuan.eshop.order.converter.AfterSaleConverter;
import com.ruyuan.eshop.order.dao.AfterSaleInfoDAO;
import com.ruyuan.eshop.order.dao.AfterSaleItemDAO;
import com.ruyuan.eshop.order.dao.AfterSaleLogDAO;
import com.ruyuan.eshop.order.dao.AfterSaleRefundDAO;
import com.ruyuan.eshop.order.domain.dto.AfterSaleOrderDetailDTO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleInfoDO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleItemDO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleLogDO;
import com.ruyuan.eshop.order.domain.entity.AfterSaleRefundDO;
import com.ruyuan.eshop.order.enums.AfterSaleQueryDataTypeEnums;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * 售后单详情builder
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Component
@Scope("prototype")
public class AfterSaleDetailBuilder {

    private AfterSaleOrderDetailDTO.AfterSaleOrderDetailDTOBuilder builder = AfterSaleOrderDetailDTO.builder();

    private boolean allNull = true;

    @Autowired
    private AfterSaleInfoDAO afterSaleInfoDAO;

    @Autowired
    private AfterSaleItemDAO afterSaleItemDAO;

    @Autowired
    private AfterSaleRefundDAO afterSaleRefundDAO;

    @Autowired
    private AfterSaleLogDAO afterSaleLogDAO;

    @Autowired
    private AfterSaleConverter afterSaleConverter;

    public AfterSaleDetailBuilder buildAfterSale(AfterSaleQueryDataTypeEnums dataType, String afterSaleId) {
        if (AfterSaleQueryDataTypeEnums.AFTER_SALE.equals(dataType)) {
            // 查询售后单
            AfterSaleInfoDO afterSaleInfoDO = afterSaleInfoDAO.getOneByAfterSaleId(afterSaleId);
            if (isNull(afterSaleInfoDO)) {
                return this;
            }
            builder.afterSaleInfo(afterSaleConverter.afterSaleInfoDO2DTO(afterSaleInfoDO));
        }
        return this;
    }

    public AfterSaleDetailBuilder buildAfterSaleItems(AfterSaleQueryDataTypeEnums dataType, String afterSaleId) {
        if (AfterSaleQueryDataTypeEnums.AFTER_SALE_ITEM.equals(dataType)) {
            // 查询售后单条目
            List<AfterSaleItemDO> afterSaleItems = afterSaleItemDAO.listByAfterSaleId(afterSaleId);
            if (isEmpty(afterSaleItems)) {
                return this;
            }
            builder.afterSaleItems(afterSaleConverter.afterSaleItemDO2DTO(afterSaleItems));
        }
        return this;
    }

    public AfterSaleDetailBuilder buildAfterSaleRefunds(AfterSaleQueryDataTypeEnums dataType, String afterSaleId) {
        if (AfterSaleQueryDataTypeEnums.AFTER_SALE_REFUND.equals(dataType)) {
            // 查询售后退款单
            List<AfterSaleRefundDO> afterSaleRefunds = afterSaleRefundDAO.listByAfterSaleId(afterSaleId);
            if (isEmpty(afterSaleRefunds)) {
                return this;
            }

            builder.afterSaleRefunds(afterSaleConverter.afterSaleRefundDO2DTO(afterSaleRefunds));
        }
        return this;
    }

    public AfterSaleDetailBuilder buildAfterSaleLogs(AfterSaleQueryDataTypeEnums dataType, String afterSaleId) {
        if (AfterSaleQueryDataTypeEnums.AFTER_SALE_LOG.equals(dataType)) {
            // 查询订单配送信息
            List<AfterSaleLogDO> afterSaleLogs = afterSaleLogDAO.listByAfterSaleId(afterSaleId);
            if (isNull(afterSaleLogs)) {
                return this;
            }

            builder.afterSaleLogs(afterSaleConverter.afterSaleLogDO2DTO(afterSaleLogs));
        }
        return this;
    }


    public AfterSaleOrderDetailDTO build() {
        return builder.build();
    }

    public boolean allNull() {
        return allNull;
    }

    private boolean isNull(Object obj) {
        if (null != obj) {
            allNull = false;
            return false;
        }
        return true;
    }

    private boolean isEmpty(Collection<?> col) {
        if (CollectionUtils.isNotEmpty(col)) {
            allNull = false;
            return false;
        }
        return true;
    }
}
