package moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillDO;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillItemDO;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.mapper.OrderFulfillItemMapper;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.mapper.OrderFulfillMapper;
import org.springframework.stereotype.Service;

/**
 * 订单履约 Mapper 接口
 */
@Service
public class OrderFulfillMyBatisService extends ServiceImpl<OrderFulfillMapper, OrderFulfillDO> {
}
