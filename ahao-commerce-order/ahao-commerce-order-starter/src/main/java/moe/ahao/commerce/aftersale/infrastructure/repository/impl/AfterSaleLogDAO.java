package moe.ahao.commerce.aftersale.infrastructure.repository.impl;

import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleInfoDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.data.AfterSaleLogDO;
import moe.ahao.commerce.aftersale.infrastructure.repository.impl.mybatis.mapper.AfterSaleLogMapper;
import moe.ahao.commerce.common.enums.AfterSaleStatusChangeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AfterSaleLogDAO {
    @Autowired
    private AfterSaleLogMapper afterSaleLogMapper;

    public void save(AfterSaleInfoDO afterSaleInfo, AfterSaleStatusChangeEnum statusChange) {
        String operateRemark = statusChange.getOperateType().getMsg();
        int preStatus = statusChange.getFromStatus().getCode();
        int currentStatus = statusChange.getToStatus().getCode();
        this.save(afterSaleInfo, preStatus, currentStatus, operateRemark);
    }

    public void save(AfterSaleInfoDO afterSaleInfo, Integer fromStatus, Integer toStatus, String remark) {
        AfterSaleLogDO log = new AfterSaleLogDO();
        log.setAfterSaleId(afterSaleInfo.getAfterSaleId());
        log.setOrderId(afterSaleInfo.getOrderId());
        log.setPreStatus(fromStatus);
        log.setCurrentStatus(toStatus);
        log.setRemark(remark);
        afterSaleLogMapper.insert(log);
    }
}
