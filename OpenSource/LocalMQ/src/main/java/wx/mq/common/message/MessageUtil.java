package wx.mq.common.message;

import io.openmessaging.BytesMessage;
import io.openmessaging.MessageHeader;
import io.openmessaging.exception.ClientOMSException;

/**
 * Description 常用的消息辅助工具
 */
public class MessageUtil {

    /**
     * Description 判断消息是否有效
     *
     * @param message
     */
    public static boolean validateMessage(BytesMessage message) {
        if (message == null) throw new ClientOMSException("Message should not be null");
        String topic = message.headers().getString(MessageHeader.TOPIC);
        String queue = message.headers().getString(MessageHeader.QUEUE);
        if ((topic == null && queue == null) || (topic != null && queue != null)) {
            throw new ClientOMSException(String.format("Queue:%s Topic:%s should put one and only one", true, queue));
        }

        return !(message.headers().getString("Topic") == null);
    }




}
