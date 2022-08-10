package moe.ahao.commerce.tms.application;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.tms.api.command.SendOutCommand;
import moe.ahao.commerce.tms.api.dto.SendOutDTO;
import moe.ahao.commerce.tms.infrastructure.exception.TmsExceptionEnum;
import moe.ahao.commerce.tms.infrastructure.gateway.impl.demo.data.PlaceLogisticOrderDTO;
import moe.ahao.commerce.tms.infrastructure.repository.impl.mybatis.data.LogisticOrderDO;
import moe.ahao.commerce.tms.infrastructure.repository.impl.mybatis.mapper.LogisticOrderMapper;
import moe.ahao.util.commons.lang.RandomHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SendOutAppService {
    @Autowired
    private LogisticOrderMapper logisticOrderMapper;

    @Transactional(rollbackFor = Exception.class)
    public SendOutDTO sendOut(SendOutCommand command) {
        String tmsException = command.getTmsException();
        if (StringUtils.isNotBlank(tmsException) && tmsException.equals("true")) {
            throw TmsExceptionEnum.EXCEPTION.msg();
        }

        // 1. 调用三方物流系统，下物流单子单
        PlaceLogisticOrderDTO placeLogisticOrderDTO = this.thirdPartyLogisticApi(command);

        // 2. 生成物流单
        LogisticOrderDO logisticOrder = new LogisticOrderDO();
        logisticOrder.setBusinessIdentifier(command.getBusinessIdentifier());
        logisticOrder.setOrderId(command.getOrderId());
        logisticOrder.setSellerId(command.getSellerId());
        logisticOrder.setUserId(command.getUserId());
        logisticOrder.setLogisticCode(placeLogisticOrderDTO.getLogisticCode());
        logisticOrder.setContent(placeLogisticOrderDTO.getContent());
        logisticOrderMapper.insert(logisticOrder);

        // 3. 申请配送员
        SendOutDTO sendOutDTO = new SendOutDTO();
        sendOutDTO.setOrderId(command.getOrderId());
        sendOutDTO.setLogisticsCode(logisticOrder.getLogisticCode());
        sendOutDTO.setDelivererName("test-delivererName");
        sendOutDTO.setDelivererNo("test-delivererNo");
        sendOutDTO.setDelivererPhone("13711111111");
        return sendOutDTO;
    }

    /**
     * 调用三方物流系统接口，下物流单子单
     */
    private PlaceLogisticOrderDTO thirdPartyLogisticApi(SendOutCommand command) {
        //模拟调用了第三方物流系统
        String logisticCode = RandomHelper.getString(11, RandomHelper.DIST_NUMBER);
        String content = "测试物流单内容";
        return new PlaceLogisticOrderDTO(logisticCode, content);
    }
}
