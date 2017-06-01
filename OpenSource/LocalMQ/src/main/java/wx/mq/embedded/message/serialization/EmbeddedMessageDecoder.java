package wx.mq.embedded.message.serialization;

import io.openmessaging.KeyValue;
import wx.mq.common.message.DefaultBytesMessage;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import static wx.mq.util.ds.CollectionTransformer.string2KeyValue;


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
 * Description 消息解码
 */
public class EmbeddedMessageDecoder extends EmbeddedMessageSerializer {

    // 日志记录器
    private final static Logger log = Logger.getLogger(EmbeddedMessageDecoder.class.getName());


    public static List<DefaultBytesMessage> readMessagesFromByteBuffer(final ByteBuffer readByteBuffer) {

        LinkedList<DefaultBytesMessage> messages = new LinkedList<>();

        DefaultBytesMessage message;

        // 这里如果读取到无法读取的位置，会抛出异常
        try {
            while ((message = readMessageFromByteBuffer(readByteBuffer)) != null) {
                messages.add(message);
            }
        } catch (Exception ignored) {

        }

        return messages;

    }

    /**
     * Description 从输入的 ByteBuffer 中反序列化消息对象
     *
     * @return 0 Come the end of the file // >0 Normal messages // -1 Message checksum failure
     */
    public static DefaultBytesMessage readMessageFromByteBuffer(ByteBuffer byteBuffer) {

        // 1 TOTAL SIZE
        int totalSize = byteBuffer.getInt();

        // 2 MAGIC CODE
        int magicCode = byteBuffer.getInt();

        switch (magicCode) {
            case MESSAGE_MAGIC_CODE:
                break;
            case BLANK_MAGIC_CODE:
                return null;
            default:
//                log.warning("found a illegal magic code 0x" + Integer.toHexString(magicCode));
                return null;
        }

        byte[] bytesContent = new byte[totalSize];

        // 3 BODY
        int bodyLen = byteBuffer.getInt();
        byte[] body = new byte[bodyLen];

        if (bodyLen > 0) {
            // 读取并且校验消息体内容
            byteBuffer.get(body, 0, bodyLen);
        }

        // 4 HEADERS
        short headersLength = byteBuffer.getShort();
        KeyValue headers = null;
        if (headersLength > 0) {
            byteBuffer.get(bytesContent, 0, headersLength);
            String headersStr = new String(bytesContent, 0, headersLength, EmbeddedMessageDecoder.CHARSET_UTF8);
            headers = string2KeyValue(headersStr);

        }

        // 5 PROPERTIES

        // 获取 properties 尺寸
        short propertiesLength = byteBuffer.getShort();
        KeyValue properties = null;
        if (propertiesLength > 0) {
            byteBuffer.get(bytesContent, 0, propertiesLength);
            String propertiesStr = new String(bytesContent, 0, propertiesLength, EmbeddedMessageDecoder.CHARSET_UTF8);
            properties = string2KeyValue(propertiesStr);

        }

        // 返回读取到的消息
        return new DefaultBytesMessage(
                totalSize,
                headers,
                properties,
                body
        );


    }


}
