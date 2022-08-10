package moe.ahao.commerce.order.infrastructure.component.sharding;

import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.shardingsphere.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.complex.ComplexKeysShardingValue;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingValue;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static moe.ahao.commerce.order.infrastructure.component.sharding.DatabaseComplexKeysShardingAlgorithm.DATABASE_SIZE;

/**
 * 自定义复合分表策略算法
 */
@NoArgsConstructor
public class TableComplexKeysShardingAlgorithm implements ComplexKeysShardingAlgorithm<String>, PreciseShardingAlgorithm<String> {
    /**
     * 每个库的分表数量 默认64
     */
    public static final int TABLE_SIZE = 64;

    /**
     * 分片键优先级依次为：
     * order_id、user_id、after_sale_id、parent_order_id
     * <p>
     * 虽然是多字段路由，但最后都是取的userId的后三位
     *
     * @param tableNames    表名集合
     * @param shardingValue 分片键信息
     * @return 匹配的表名集合
     */
    @Override
    public Collection<String> doSharding(Collection<String> tableNames, ComplexKeysShardingValue<String> shardingValue) {
        String[] columns = {"order_id", "user_id", "after_sale_id", "parent_order_id"};

        for (String column : columns) {
            Collection<String> columnValues = shardingValue.getColumnNameAndShardingValuesMap().get(column);
            if (CollectionUtils.isNotEmpty(columnValues)) {
                return this.getTableNames(tableNames, columnValues);
            }
        }
        return null;
    }

    /**
     * @param tableNames    表名集合
     * @param shardingValue 分片键信息
     * @return 匹配的表名集合
     */
    @Override
    public String doSharding(Collection<String> tableNames, PreciseShardingValue<String> shardingValue) {
        return this.getTableName(tableNames, shardingValue.getValue());
    }

    /**
     * 获取真实的表名
     *
     * @param columnValues 分片键的值
     * @return 匹配的表名集合
     */
    private Set<String> getTableNames(Collection<String> tableNames, Collection<String> columnValues) {
        return columnValues.stream()
            .map(v -> this.getTableName(tableNames, v))
            .collect(Collectors.toSet());
    }

    /**
     * 获取数据源名
     *
     * @param columnValue 分片键信息
     * @return 匹配的表名
     */
    private String getTableName(Collection<String> tableNames, String columnValue) {
        // 获取用户id后三位
        String valueSuffix = (columnValue.length() < 3) ? columnValue : columnValue.substring(columnValue.length() - 3);
        // 计算将路由到的数据表名后缀
        String tableSuffix = this.getTableSuffix(Integer.parseInt(valueSuffix));
        for (String tableName : tableNames) {
            // 返回匹配到的真实数据表名
            if (tableName.endsWith(tableSuffix)) {
                return tableName;
            }
        }
        return null;
    }

    /**
     * 计算table的后缀
     *
     * @param valueSuffix 分片键的值后三位
     * @return 表名后缀
     */
    private String getTableSuffix(int valueSuffix) {
        return valueSuffix / DATABASE_SIZE % TABLE_SIZE + "";
    }
}
