package moe.ahao.commerce.common.infrastructure.rocketmq;

import moe.ahao.commerce.common.infrastructure.utils.CoreConstant;
import moe.ahao.commerce.common.infrastructure.utils.MdcUtil;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.core.RocketMQListener;

import java.nio.charset.StandardCharsets;

/**
 * 抽象的消费者MessageListener组件
 * 实现RocktMQ原生的RocketMQListener
 */
public abstract class AbstractRocketMqListener implements RocketMQListener<MessageExt> {

    @Override
    public void onMessage(MessageExt message) {
        try {
            String traceId = message.getProperty(CoreConstant.TRACE_ID);
            if (traceId != null && !"".equals(traceId)) {
                MdcUtil.setTraceId(traceId);
            }
            this.onMessage(new String(message.getBody(), StandardCharsets.UTF_8));
        } finally {
            MdcUtil.removeTraceId();
        }
    }

    public abstract void onMessage(String message);
}
