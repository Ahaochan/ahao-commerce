package com.ruyuan.eshop.order.domain.dto;

import com.ruyuan.eshop.order.builder.FullOrderData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author zhonghuashishan
 * @version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SplitOrderDTO {
    private Integer productType;
    private FullOrderData fullOrderData;
}
