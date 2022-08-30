package com.ruyuan.eshop.order.config.sharding;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.shardingsphere.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.complex.ComplexKeysShardingValue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 自定义复合分库策略算法
 * @author zhonghuashishan
 * @version 1.0
 */
@Slf4j
@NoArgsConstructor
public class CustomDatabaseComplexKeysShardingAlgorithm implements ComplexKeysShardingAlgorithm<String> {

    /**
     * 分片键优先级依次为：
     * order_id、user_id、after_sale_id、parent_order_id
     *
     * 虽然是多字段路由，但最后都是取的userId的后三位
     *
     * @param dataSourceNames 数据源名称集合
     * @param shardingValue 分片键信息
     * @return 匹配的数据源集合
     */
    @Override
    public Collection<String> doSharding(Collection<String> dataSourceNames,
                                         ComplexKeysShardingValue<String> shardingValue) {
        Collection<String> orderIds = shardingValue.getColumnNameAndShardingValuesMap().get("order_id");
        Collection<String> userIds = shardingValue.getColumnNameAndShardingValuesMap().get("user_id");
        Collection<String> afterSaleIds = shardingValue.getColumnNameAndShardingValuesMap().get("after_sale_id");
        Collection<String> parentOrderIds = shardingValue.getColumnNameAndShardingValuesMap().get("parent_order_id");
        if (CollectionUtils.isNotEmpty(orderIds)) {
            return getDatabaseNames(dataSourceNames, orderIds);
        }
        if (CollectionUtils.isNotEmpty(userIds)) {
            return getDatabaseNames(dataSourceNames, userIds);
        }
        if (CollectionUtils.isNotEmpty(afterSaleIds)) {
            return getDatabaseNames(dataSourceNames, afterSaleIds);
        }
        if (CollectionUtils.isNotEmpty(parentOrderIds)) {
            return getDatabaseNames(dataSourceNames, parentOrderIds);
        }
        return null;
    }

    /**
     * 获取数据源名
     * @param columnValues 分片键的值
     * @return 匹配的数据源集合
     */
    private Set<String> getDatabaseNames(Collection<String> dataSourceNames, Collection<String> columnValues) {
        Set<String> set = new HashSet<>();
        for(String columnValue : columnValues) {
            // 获取用户ID后三位
            String valueSuffix = (columnValue.length() < 3) ? columnValue :
                    columnValue.substring(columnValue.length() - 3);
            String databaseSuffix = ShardingAlgorithmHelper.getDatabaseSuffix(Integer.parseInt(valueSuffix));
            for(String dataSourceName : dataSourceNames) {
                if(dataSourceName.endsWith(databaseSuffix)) {
                    set.add(dataSourceName);
                }
            }
        }
        return set;
    }
}