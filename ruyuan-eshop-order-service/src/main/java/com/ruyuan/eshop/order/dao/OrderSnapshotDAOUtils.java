package com.ruyuan.eshop.order.dao;

import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import com.ruyuan.eshop.order.domain.entity.OrderInfoDO;
import com.ruyuan.eshop.order.hbase.entity.OrderInfoExtJsonDTO;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.MD5Hash;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author zhonghuashishan
 * @version 1.0
 */
public class OrderSnapshotDAOUtils {

    /**
     * rowKey前缀截取的位数
     */
    private static final int ROW_KEY_LENGTH = 10;

    /**
     * 获取下单时，预分配好的订单快照rowKey列表
     * @param orderInfo 订单信息
     * @return rowKey列表
     */
    public static List<String> getRowKeyPrefixList(OrderInfoDO orderInfo) {
        if(null==orderInfo || StringUtils.isEmpty(orderInfo.getExtJson())) {
            return new ArrayList<>();
        }
        String extJsonText = orderInfo.getExtJson();
        OrderInfoExtJsonDTO orderInfoExtJsonDTO = JSONUtil.toBean(extJsonText, OrderInfoExtJsonDTO.class);
        return orderInfoExtJsonDTO.getOrderSnapshotRowKeyPrefixList();
    }

    /**
     * 批量生成rowKey前缀
     * @param rowKeyPrefixNum 生成rowKey前缀的数量
     * @return 生成rowKey前缀集合
     */
    public static List<String> batchGenerateRowKey(int rowKeyPrefixNum){
        if(rowKeyPrefixNum < 1){
            return new ArrayList<>();
        }
        List<String> rowKeyPrefixList = new ArrayList<>(rowKeyPrefixNum);
        for(int i=0; i < rowKeyPrefixNum;i++){
            rowKeyPrefixList.add(getRowKeyPrefix());
        }
        return rowKeyPrefixList;
    }

    /**
     * 获得一个rowKey前缀
     * @return rowKey前缀
     */
    public static String getRowKeyPrefix(){
        String uuid = UUID.randomUUID().toString();
        // 去掉 - 符号
        String rewrite = uuid.replaceAll("-", "");
        // 转为md5格式
        String md5AsHex = MD5Hash.getMD5AsHex(Bytes.toBytes(rewrite));
        // 获取前10位
        return md5AsHex.substring(0, ROW_KEY_LENGTH);
    }

}
