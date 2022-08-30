package moe.ahao.commerce.order.infrastructure.repository.impl.hbase;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.json.JSONUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.order.infrastructure.repository.impl.hbase.data.OrderSnapshotDO;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CompareOperator;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.filter.SubstringComparator;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.MD5Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单快照表 DAO
 */
@Slf4j
@Component
public class OrderSnapshotRepository {
    /**
     * create_namespace 'ORDER_NAMESPACE'
     * <p>
     * create 'ORDER_NAMESPACE:ORDER_SNAPSHOT',{NAME => "SNAPSHOT",COMPRESSION => "GZ"},{NUMREGIONS=>5,SPLITALGO=>'HexStringSplit'}
     * <p>
     * 表名  格式: 命名空间:表名 = 表名
     */
    public static final String TABLE_NAME = "ORDER_NAMESPACE:ORDER_SNAPSHOT";
    /**
     * 列族名
     */
    public static final String COLUMN_FAMILY = "SNAPSHOT";
    /**
     * rowKey前缀截取的位数
     */
    private static final int ROW_KEY_LENGTH = 10;

    /**
     * 获取HBase连接
     */
    @Autowired
    private Connection connection;

    /**
     * 根据RowKey获取指定列数据
     *
     * @param tableName 表名
     * @param rowKey    rowKey
     * @param colFamily 列族
     * @param cols      列
     * @return Result
     */
    public Result getData(String tableName, String rowKey, String colFamily, List<String> cols) throws Exception {
        if (!isTableExist(tableName)) {
            throw new RuntimeException("表[" + tableName + "]不存在");
        }
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            Get get = new Get(Bytes.toBytes(rowKey));
            if (null != cols) {
                cols.forEach(col -> get.addColumn(Bytes.toBytes(colFamily), Bytes.toBytes(col)));
            }
            return table.get(get);
        }
    }

    /**
     * 根据订单id查询订单快照
     *
     * @param orderId 订单id
     */
    @SneakyThrows
    public List<OrderSnapshotDO> getListByOrderId(String orderId) {
        Table table = null;
        ResultScanner resultScanner = null;
        OrderSnapshotDO orderSnapshotDO;
        List<OrderSnapshotDO> resultSnapshotList = new ArrayList<>();
        try {
            // 下面这种hbase scan+filter的写法，是不可取的，这里是给大家演示一个hbase bad case设计
            // rowkey设计+时间戳hash值，均匀分散，这个设计是没问题的，hbase rowkey设计里，用于避免数据热点问题的一个经典设计方案
            // 查询的时候，直接按照substring子串搜索是不可取的
            // 会导致一个问题，hbase最强悍的是什么，是基于rowkey整值去构建多个级别的缓存、索引，就是直接基于rowkey进行等值匹配和查询
            // 他是大型的分布式海量数据kv读取存储系统，按照key来进行等值读取，所有优化设计，都是围绕你rowkey等值来
            // 会导致的他有一个类似于全表扫描一样的问题，多个region会被他一个一个的扫描过去，尝试去读取每个rowkey匹配里面的orderid
            // 要基于时间戳作为前缀的rowkey查找
            // 性能优化的一个技巧，scan设置startRow和endRow，起始行和截止行
            // 起始行，要设置起始的时间戳，截止行，要设置截止时间戳，设置时间戳的范围之后
            // 就可以帮助hbase缩小他行扫描的范围，可以在一个小范围之内，快速的扫描，找到你的orderid他对应的数据
            // 必须建议，性能优化，把订单每个订单快照插入的时候，那一个时间戳给找出来
            // 就是说通过时间戳去设置startRow和endRow，缩小每条数据查询的范围，快速的检索这样子
            // 把每个订单快照的插入时间戳，往前面倒推一些，往后面后推一些，形成一个范围，对起止时间戳生成纳秒级别的hash串
            // 把这两个hash串，作为一个范围，去进行缩小范围的查询这样子

            // 1. 获取表
            table = connection.getTable(TableName.valueOf(TABLE_NAME));
            // 2. 构建scan请求对象
            Scan scan = new Scan();
            // 3. 构建过滤器 这里使用订单id作为子串来构造过滤器
            RowFilter rowFilter = new RowFilter(CompareOperator.EQUAL, new SubstringComparator(orderId));
            scan.setFilter(rowFilter);
            // scan.withStartRow()
            // scan.withStopRow()
            // 4. 执行过滤扫描请求
            resultScanner = table.getScanner(scan);
            // 5. 迭代打印result
            for (Result result : resultScanner) {
                orderSnapshotDO = new OrderSnapshotDO();
                // 6. 迭代单元格列表 result.listCells() 列族所有的单元格
                for (Cell cell : result.listCells()) {
                    // 获取当前遍历到的列的名称
                    String columnName = Bytes.toString(cell.getQualifierArray(), cell.getQualifierOffset(), cell.getQualifierLength());
                    // 设置当前列的值
                    ReflectUtil.setFieldValue(orderSnapshotDO, columnName, getColumnValue(cell, columnName));
                }
                resultSnapshotList.add(orderSnapshotDO);
            }
            return resultSnapshotList;
        } catch (Exception e) {
            log.error("根据订单id查询订单快照时，发生异常", e);
            return new ArrayList<>(0);
        } finally {
            // 7. 关闭ResultScanner
            if (!ObjectUtils.isEmpty(resultScanner)) {
                resultScanner.close();
            }
            // 8. 关闭表
            if (!ObjectUtils.isEmpty(table)) {
                table.close();
            }
        }
    }

    private Object getColumnValue(Cell cell, String columnName) {
        if ("id".equals(columnName)) {
            return Bytes.toLong(cell.getValueArray());
        } else if ("gmtCreate".equals(columnName) || "gmtModified".equals(columnName)) {
            Date date = new Date();
            date.setTime(Bytes.toLong(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength()));
            return date;
        } else if ("orderId".equals(columnName)) {
            return Bytes.toString(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength());
        } else if ("snapshotType".equals(columnName)) {
            return Bytes.toInt(cell.getValueArray());
        } else if ("snapshotJson".equals(columnName)) {
            return Bytes.toString(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength());
        }
        return "";
    }

    /**
     * 插入操作
     *
     * @param tableName        表名
     * @param columnFamilyName 列族名称
     * @param rowKey           要插入HBase中的rowKey
     * @param columnMap        要插入的列以及列对应的值
     * @return 插入结果
     */
    public boolean insert(String tableName, String columnFamilyName, String rowKey, Map<String, Object> columnMap) {
        try {
            // 指定要插入的表
            Table table = connection.getTable(TableName.valueOf(tableName));
            // 构造插入要插入的数据
            Put put = buildPutOperation(columnFamilyName, rowKey, columnMap);
            // 执行插入操作
            table.put(put);
            return true;
        } catch (Exception e) {
            log.error("插入hbase时，发送异常", e);
            return false;
        }
    }

    /**
     * 批量插入操作
     *
     * @param orderSnapshotDOList 要插入快照的集合
     */
    public void saveBatch(List<OrderSnapshotDO> orderSnapshotDOList) {
        try {
            // 指定要插入的表
            Table table = connection.getTable(TableName.valueOf(TABLE_NAME));
            // 构造要插入的数据的集合
            List<Put> putCollection = orderSnapshotDOList.stream()
                .map(OrderSnapshotDO::toMap)
                .map((columnMap) -> this.buildPutOperation(COLUMN_FAMILY, getRowKey(columnMap.get("orderId")), columnMap)
                )
                .collect(Collectors.toList());
            // 执行批量插入操作
            table.put(putCollection);
        } catch (Exception e) {
            log.error("插入订单快照时，发送异常", e);
        }
    }

    /**
     * 获取订单快照的RowKey
     * 为了避免rowKey过长截取前10位, 同时基于HASH算法, 计算拼接好的RowKey的hash值, 以达到将数据散列到不同的HRegionServer
     * 让多个HRegionServer来分摊压力, 避免读写热点
     *
     * @param orderId 订单id
     * @return RowKey格式, 时间戳hash值前10位_订单id
     */
    private String getRowKey(Object orderId) {
        StringJoiner rowKeyJoiner = new StringJoiner("_", "", "");
        // 获取纳秒时间戳
        // 纳秒这个级别的时间戳，纳秒级别的时间戳，粒度已经非常非常细了，就是说是不要出现多个操作拿到的时间戳是同一个时间戳
        long timestamp = System.currentTimeMillis() * 1000000L + System.nanoTime() % 1000000L;
        // 获取纳秒时间戳hash值
        String timestampHex = MD5Hash.getMD5AsHex(Bytes.toBytes(timestamp));
        // 形成rowKey：时间戳hash值前10位_订单id
        // 有一个很关键的优化的思路是在里面，订单数据很容易出现热点数据，热点数据，最近产生的一些订单数据
        // 他快照都写入到了你的hbase里去，可能都会进入他的同一个region里去，数据节点里去，造成数据热点的问题，最近订单写入到了一个数据区域里去
        // 对最近订单快照查询是最多的，就会导致说针对你的这个region里的数据他的热点查询会比较严重
        // 导致hbase某个节点压力比较大一些
        // rowkey为了避免数据热点的问题，我们可以采取对rowkey加时间戳作为一个前缀，时间戳是用的hash值，就可以让我们最近一段时间写入hbase的订单
        // 分散到hbase里多个节点里去，多个region里面去，数据分散，读取的时候，对最近一段时间的订单读取，就可以分散到多个数据区域去读取，避免了数据热点的问题
        // 数据扩展性，如果说我们一个orderid有多个snapshotType类型，有多条数据
        // 写入多个snapshotType，用纳秒级别时间戳作为前缀，一个orderId多个数据，纳秒前缀是不同的
        return rowKeyJoiner.add(timestampHex.substring(0, ROW_KEY_LENGTH))
            .add((String) orderId)
            .toString();
    }

    /**
     * 构造HBase PUT操作
     *
     * @param familyName 列族名
     * @param rowKey     rowKey
     * @param columnMap  列名与列值的集合
     * @return 构造好的HBase的PUT操作对象
     */
    private Put buildPutOperation(String familyName, String rowKey, Map<String, Object> columnMap) {
        Put put = new Put(Bytes.toBytes(rowKey));
        // 对给列族加入对应的列
        for (Map.Entry<String, Object> entry : columnMap.entrySet()) {
            String columnName = entry.getKey();
            Object columnValue = entry.getValue();

            byte[] family = Bytes.toBytes(familyName); // 列族名称
            byte[] qualifier = Bytes.toBytes(columnName); // 这个列族里的列名称
            byte[] value = getOriginColumnValue(columnName, columnValue); // 对应的值
            // 添加列名与列值到HBase列中
            put.addColumn(family, qualifier, value);
        }
        return put;
    }


    /**
     * 获取对应列的数据类型
     *
     * @param columnName  要转换的列名
     * @param columnValue 列值
     * @return 获取列值的字节数组
     */
    private byte[] getOriginColumnValue(String columnName, Object columnValue) {
        if (ObjectUtils.isEmpty(columnValue)) {
            return new byte[0];
        }

        if (columnValue instanceof Short) {
            return Bytes.toBytes((short) columnValue);
        }
        if (columnValue instanceof Integer) {
            return Bytes.toBytes((int) columnValue);
        }
        if (columnValue instanceof Long) {
            return Bytes.toBytes((long) columnValue);
        }
        if (columnValue instanceof Date) {
            long time = ((Date) columnValue).getTime();
            return Bytes.toBytes(time);
        }

        if (columnValue instanceof Float) {
            return Bytes.toBytes((float) columnValue);
        }
        if (columnValue instanceof Double) {
            return Bytes.toBytes((double) columnValue);
        }
        if (columnValue instanceof Boolean) {
            return Bytes.toBytes((boolean) columnValue);
        }
        if (columnValue instanceof BigDecimal) {
            return Bytes.toBytes((BigDecimal) columnValue);
        }
        if (columnValue instanceof ByteBuffer) {
            return Bytes.toBytes((ByteBuffer) columnValue);
        }
        if (columnValue instanceof ArrayList) {
            return Bytes.toBytes(JSONUtil.toJsonStr(columnValue));
        }

        if (columnValue instanceof String) {
            log.info("columnName = {}, columnValue = {}", columnName, columnValue);
            return Bytes.toBytes((String) columnValue);
        }

        throw new RuntimeException("columnName为空，无法进行数据类型转换失败");
    }

    /**
     * 判断表是否存在
     *
     * @param tableName 表名称
     * @return 表是否存在
     */
    private boolean isTableExist(String tableName) throws IOException {
        boolean exists = false;
        try (HBaseAdmin admin = (HBaseAdmin) connection.getAdmin()) {
            // 判断是否存在
            exists = admin.tableExists(TableName.valueOf(tableName));
        }
        return exists;
    }
}
