package wx.mq.embedded.message.serialization;

import wx.mq.common.message.DefaultBytesMessage;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import static wx.mq.util.fs.nio.BufferUtil.resetByteBuffer;

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
 * Description 消息编码类
 */
public class EmbeddedMessageEncoder extends EmbeddedMessageSerializer {

    // 日志记录
    private final static Logger log = Logger.getLogger(EmbeddedMessageEncoder.class.getName());

    /**
     * Description 执行消息的编码操作
     * @param message 消息对象
     * @param msgStoreItemMemory 内部缓存句柄
     * @param msgLen 计算的消息长度
     * @param headersByteArray 消息头字节序列
     * @param propertiesByteArray 消息属性字节序列
     */
    public static final void encode(
            DefaultBytesMessage message,
            final ByteBuffer msgStoreItemMemory,
            int msgLen,
            byte[] headersByteArray,
            byte[] propertiesByteArray
    ) {

        // 消息体
        byte[] body = message.getBody();

        int bodyLength = body == null ? 0 : body.length;

        // 计算头部长度
        short headersLength = (short) headersByteArray.length;

        // 计算属性长度
        short propertiesLength = (short) propertiesByteArray.length;

        // 初始化存储空间
        resetByteBuffer(msgStoreItemMemory, msgLen);

        // 1 TOTALSIZE
        msgStoreItemMemory.putInt(msgLen);

        // 2 MAGICCODE
        msgStoreItemMemory.putInt(MESSAGE_MAGIC_CODE);

        // 3 BODY
        msgStoreItemMemory.putInt(bodyLength);
        if (bodyLength > 0)
            msgStoreItemMemory.put(message.getBody());

        // 4 HEADERS
        msgStoreItemMemory.putShort((short) headersLength);
        if (headersLength > 0)
            msgStoreItemMemory.put(headersByteArray);

        // 5 PROPERTIES
        msgStoreItemMemory.putShort((short) propertiesLength);
        if (propertiesLength > 0)
            msgStoreItemMemory.put(propertiesByteArray);

    }

}
