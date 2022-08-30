package com.ruyuan.eshop.order.hbase.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 订单表拓展字段
 * </p>
 *
 * @author zhonghuashishan
 */
@Data
public class OrderInfoExtJsonDTO implements Serializable {

    /**
     * 订单快照预分配好的hbase rowKey前缀  数量由下单时订单快照的数量来决定
     */
    private List<String> orderSnapshotRowKeyPrefixList;

}
