package moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillDO;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.data.OrderFulfillLogDO;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.mapper.OrderFulfillLogMapper;
import moe.ahao.commerce.fulfill.infrastructure.repository.impl.mybatis.mapper.OrderFulfillMapper;
import org.springframework.stereotype.Service;

/**
 * 订单履约日志 Mapper 接口
 */
@Service
public class OrderFulfillLogMyBatisService extends ServiceImpl<OrderFulfillLogMapper, OrderFulfillLogDO> {
}
