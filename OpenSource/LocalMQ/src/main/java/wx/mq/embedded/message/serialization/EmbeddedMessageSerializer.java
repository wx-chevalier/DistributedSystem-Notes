package wx.mq.embedded.message.serialization;


import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.common.message.serialization.MessageSerializer;

import java.util.logging.Logger;

/*
| 序号   | 消息存储结构            | 备注                                       | 长度（字节数）              |
| ---- | ----------------- | ---------------------------------------- | -------------------- |
| 1    | TOTALSIZE         | 消息大小                                     | 4                    |
| 2    | MAGICCODE         | 消息的 MAGIC CODE                           | 4                    |
| 3    | BODY              | 前 4 个字节存放消息体大小值，后 bodyLength 大小的空间存储消息体内容 | 4 + bodyLength       |
| 4    | headers*          | 前 2 个字节（short）存放头部大小，后存放 headersLength 大小的头部数据 | 2 + headersLength    |
| 5    | properties*       | 前 2 个字节（short）存放属性值大小，后存放 propertiesLength 大小的属性数据 | 2 + propertiesLength |
 */


/**
 * Description 消息序列化
 */
public class EmbeddedMessageSerializer extends MessageSerializer {

    // 日志记录器
    private final static Logger log = Logger.getLogger(EmbeddedMessageSerializer.class.getName());

    // 记录 1~11 项的大小
    private static final int BODY_SIZE_POSITION = 4 // 1 TOTALSIZE
            + 4; // 2 MAGICCODE

    /**
     * Description 计算某个消息的长度，注意，headersByteArray 与 propertiesByteArray 在发送消息时完成转换
     * @param message
     * @param headersByteArray
     * @param propertiesByteArray
     * @return
     */
    public static int calMsgLength(DefaultBytesMessage message, byte[] headersByteArray, byte[] propertiesByteArray) {

        // 消息体
        byte[] body = message.getBody();

        int bodyLength = body == null ? 0 : body.length;

        // 计算头部长度
        short headersLength = (short) headersByteArray.length;

        // 计算属性长度
        short propertiesLength = (short) propertiesByteArray.length;

        // 计算消息体总长度
        return calMsgLength(bodyLength, headersLength, propertiesLength);

    }

    /**
     * @param bodyLength
     * @param headersLength
     * @param propertiesLength
     * @return
     * @function 计算当前消息体的总长度
     */
    public static int calMsgLength(int bodyLength, int headersLength, int propertiesLength) {
        return BODY_SIZE_POSITION
                + 4 + (bodyLength > 0 ? bodyLength : 0) // 3 BODY
                + 2 + (headersLength > 0 ? headersLength : 0) // 4 headers
                + 2 + (propertiesLength > 0 ? propertiesLength : 0); // 5 properties
    }

}

