package moe.ahao.commerce.aftersale.api.query;

import lombok.Data;
import moe.ahao.commerce.aftersale.api.enums.AfterSaleQueryDataTypeEnums;

import java.io.Serializable;

/**
 * 售后单详情请求
 */
@Data
public class AfterSaleDetailQuery {
    /**
     * 售后单id
     */
    private String afterSaleId;
    /**
     * 售后单项查询枚举
     */
    private AfterSaleQueryDataTypeEnums[] queryDataTypes;
}
