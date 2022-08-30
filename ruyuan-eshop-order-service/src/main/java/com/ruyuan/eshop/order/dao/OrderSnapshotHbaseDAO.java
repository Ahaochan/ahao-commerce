package com.ruyuan.eshop.order.dao;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Maps;
import com.ruyuan.eshop.order.domain.entity.OrderSnapshotDO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.beans.BeanMap;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单快照表 hbase DAO
 *
 * @author zhonghuashishan
 */
@Slf4j
//@Component
public class OrderSnapshotHbaseDAO implements OrderSnapshotDAO{

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
     * @param rowKeyPrefixList 预分配好的rowKey前缀集合
     */
    @Override
    @SneakyThrows
    public List<OrderSnapshotDO> queryOrderSnapshotByOrderId(String orderId, List<String> rowKeyPrefixList) {
        log.info("OrderSnapshotHbaseDAO.queryOrderSnapshotByOrderId: orderId={}",orderId);
        Table table = null;
        Result[] resultList;
        OrderSnapshotDO orderSnapshotDO;
        List<OrderSnapshotDO> resultSnapshotList = new ArrayList<>();
        try {
            // 1. 获取表
            table = connection.getTable(TableName.valueOf(TABLE_NAME));
            // 2. 构造HBase批量查询RowKey的条件
            List<Get> getOpList = rowKeyPrefixList.stream()
                    .map(rowKeyPrefix -> new Get(Bytes.toBytes(rowKeyPrefix + "_" + orderId)))
                    .collect(Collectors.toList());
            // 获取查询结果
            resultList = table.get(getOpList);
            // 3. 迭代resultList
            for (Result result : resultList) {
                orderSnapshotDO = new OrderSnapshotDO();
                // 4. 迭代单元格列表 result.listCells() 列族所有的单元格
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
            // 5. 关闭表
            if (!ObjectUtils.isEmpty(table)) {
                table.close();
            }
        }
    }

    @Override
    public List<OrderSnapshotDO> queryOrderSnapshotByOrderIds(List<String> orderIds) {
        return null;
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
     * @param orderSnapshotRowKeyPrefixList 预分配好的RowKey前缀
     */
    @Override
    public void batchSave(List<OrderSnapshotDO> orderSnapshotDOList, List<String> orderSnapshotRowKeyPrefixList) {
        try {
            // 指定要插入的表
            Table table = connection.getTable(TableName.valueOf(TABLE_NAME));
            List<Put> putCollection = new ArrayList<>();
            List<Map<String, Object>> columnMapList = objectToMapList(orderSnapshotDOList);
            for (int i = 0; i < columnMapList.size(); i++) {
                Map<String, Object> columnMap = columnMapList.get(i);
                putCollection.add(buildPutOperation(COLUMN_FAMILY, getRowKey(columnMap.get("orderId"), orderSnapshotRowKeyPrefixList.get(i)), columnMap));
            }
            // 执行批量插入操作
            table.put(putCollection);
        } catch (Exception e) {
            log.error("插入订单快照时，发生异常", e);
        }
    }

    /**
     * 获取订单快照的RowKey
     * 为了避免rowKey过长截取前10位，同时基于HASH算法，计算拼接好的RowKey的hash值，以达到将数据散列到不同的HRegionServer，
     * 让多个HRegionServer来分摊压力,避免读写热点
     *
     * @param orderId 订单id
     * @param rowKeyPrefix 预分配好的RowKey前缀 生成方式: MD5(UUID).substring(10)
     * @return RowKey格式：时间戳hash值前10位_订单id
     */
    private String getRowKey(Object orderId, String rowKeyPrefix) {
        StringJoiner rowKeyJoiner = new StringJoiner("_", "", "");
        // 形成rowKey：uuid hash值前10位_订单id
        return rowKeyJoiner
                .add(rowKeyPrefix)
                .add((String) orderId)
                .toString();
    }

    /**
     * 对象转map
     *
     * @param obj 待转换的对象
     * @return 转换的结果
     */
    public Map<String, Object> objectToMap(Object obj) {
        Map<String, Object> map = Maps.newHashMap();
        if (obj != null) {
            net.sf.cglib.beans.BeanMap beanMap = BeanMap.create(obj);
            for (Object key : beanMap.keySet()) {
                map.put(key + "", beanMap.get(key));
            }
        }
        return map;
    }

    /**
     * 对象转map
     *
     * @param objList 待转换的对象集合
     * @return 转换的结果
     */
    public List<Map<String, Object>> objectToMapList(List<?> objList) {
        if (CollectionUtils.isEmpty(objList)) {
            return new ArrayList<>(0);
        }
        return objList.stream().map(this::objectToMap).collect(Collectors.toList());
    }

    /**
     * 构造HBase PUT操作
     *
     * @param columnFamilyName 列族名
     * @param rowKey           rowKey
     * @param columnMap        列名与列值的集合
     * @return 构造好的HBase的PUT操作对象
     */
    private Put buildPutOperation(String columnFamilyName, String rowKey, Map<String, Object> columnMap) {
        Put put = new Put(Bytes.toBytes(rowKey));
        // 对给列族加入对应的列
        Set<String> columnMapKeySet = columnMap.keySet();
        for (String columnName : columnMapKeySet) {
            // 添加列名与列值到HBase列中
            put.addColumn(
                    Bytes.toBytes(columnFamilyName),
                    Bytes.toBytes(columnName),
                    getOriginColumnValue(columnMap, columnName)
            );
        }
        return put;
    }


    /**
     * 获取对应列的数据类型
     *
     * @param columnMap  列名与列值的map集合 列名为key 列值为value
     * @param columnName 要转换的列名
     * @return 获取列值的字节数组
     */
    private byte[] getOriginColumnValue(Map<String, Object> columnMap, String columnName) {
        Object columnValue = columnMap.get(columnName);
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
