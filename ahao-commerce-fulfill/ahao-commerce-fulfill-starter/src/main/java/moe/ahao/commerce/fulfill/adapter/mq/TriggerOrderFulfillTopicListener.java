package moe.ahao.commerce.fulfill.adapter.mq;

import lombok.extern.slf4j.Slf4j;
import moe.ahao.commerce.common.infrastructure.rocketmq.AbstractMessageListenerConcurrently;
import moe.ahao.commerce.fulfill.api.command.ReceiveFulfillCommand;
import moe.ahao.commerce.fulfill.application.ReceiveFulfillAppService;
import moe.ahao.util.commons.io.JSONHelper;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class TriggerOrderFulfillTopicListener extends AbstractMessageListenerConcurrently {
    @Autowired
    private ReceiveFulfillAppService receiveFulfillAppService;

    @Override
    public ConsumeConcurrentlyStatus onMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
        try {
            for (MessageExt messageExt : list) {
                String message = new String(messageExt.getBody());
                ReceiveFulfillCommand command = JSONHelper.parse(message, ReceiveFulfillCommand.class);

                Boolean result = receiveFulfillAppService.fulfill(command);
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        } catch (Exception e) {
            log.error("consumer error", e);
            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
        }
    }

}
