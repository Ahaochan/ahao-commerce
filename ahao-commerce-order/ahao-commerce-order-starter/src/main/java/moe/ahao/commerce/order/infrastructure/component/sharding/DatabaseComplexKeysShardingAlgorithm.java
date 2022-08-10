package moe.ahao.commerce.order.infrastructure.component.sharding;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.shardingsphere.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.complex.ComplexKeysShardingValue;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingValue;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自定义复合分库策略算法
 */
@Slf4j
@NoArgsConstructor
public class DatabaseComplexKeysShardingAlgorithm implements ComplexKeysShardingAlgorithm<String>, PreciseShardingAlgorithm<String> {
    /**
     * 总的分库数量  默认8
     */
    public static final int DATABASE_SIZE = 8;

    /**
     * 分片键优先级依次为：
     * order_id、user_id、after_sale_id、parent_order_id
     * <p>
     * 虽然是多字段路由，但最后都是取的userId的后三位
     *
     * @param dataSourceNames 数据源名称集合
     * @param shardingValue   分片键信息
     * @return 匹配的数据源集合
     */
    @Override
    public Collection<String> doSharding(Collection<String> dataSourceNames, ComplexKeysShardingValue<String> shardingValue) {
        String[] columns = {"order_id", "user_id", "after_sale_id", "parent_order_id"};

        for (String column : columns) {
            Collection<String> columnValues = shardingValue.getColumnNameAndShardingValuesMap().get(column);
            if (CollectionUtils.isNotEmpty(columnValues)) {
                return this.getDatabaseNames(dataSourceNames, columnValues);
            }
        }
        return null;
    }

    /**
     * @param datasourceNames      数据源名称集合
     * @param preciseShardingValue 分片键信息
     * @return 匹配的数据源名
     */
    @Override
    public String doSharding(Collection<String> datasourceNames, PreciseShardingValue<String> preciseShardingValue) {
        return this.getDatabaseName(datasourceNames, preciseShardingValue.getValue());
    }

    /**
     * 获取数据源名
     *
     * @param columnValues 分片键的值
     * @return 匹配的数据源集合
     */
    private Set<String> getDatabaseNames(Collection<String> dataSourceNames, Collection<String> columnValues) {
        return columnValues.stream()
            .map(v -> this.getDatabaseName(dataSourceNames, v))
            .collect(Collectors.toSet());
    }

    /**
     * 获取数据源名
     *
     * @param columnValue 分片键的值
     * @return 匹配的数据源名
     */
    private String getDatabaseName(Collection<String> dataSourceNames, String columnValue) {
        // 获取用户id后三位
        String valueSuffix = (columnValue.length() < 3) ? columnValue : columnValue.substring(columnValue.length() - 3);
        // 计算将路由到的数据源名后缀
        String databaseSuffix = this.getDatabaseSuffix(Integer.parseInt(valueSuffix));
        for (String dataSourceName : dataSourceNames) {
            // 返回匹配到的真实数据源名
            if (dataSourceName.endsWith(databaseSuffix)) {
                return dataSourceName;
            }
        }
        return null;
    }

    /**
     * 计算database的后缀
     *
     * @param valueSuffix 分片键的值后三位
     * @return 数据源名后缀
     */
    private String getDatabaseSuffix(int valueSuffix) {
        return valueSuffix % DATABASE_SIZE + "";
    }
}
