package wx.mq.local.message.serialization;

import io.openmessaging.KeyValue;
import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.common.message.status.GetMessageResult;
import wx.mq.local.message.service.PostPutMessageRequest;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static wx.mq.util.ds.CollectionTransformer.string2KeyValue;

/**
 * Description 消息解码
 */
public class LocalMessageDecoder extends LocalMessageSerializer {

    // 日志记录器
    private final static Logger log = Logger.getLogger(LocalMessageDecoder.class.getName());


    /**
     * Description 检测 Message 是否有效并且返回 Message 尺寸
     *
     * @return 0 Come the end of the file // >0 Normal messages // -1 Message checksum failure
     */
    public static PostPutMessageRequest checkMessageAndReturnSize(java.nio.ByteBuffer byteBuffer) {
        try {
            // 1 TOTAL SIZE
            int totalSize = byteBuffer.getInt();

            // 2 MAGIC CODE
            int magicCode = byteBuffer.getInt();

            // 根据 MagicCode 判断是否为有效值
            switch (magicCode) {
                case MESSAGE_MAGIC_CODE:
                    break;
                case BLANK_MAGIC_CODE:
                    return new PostPutMessageRequest(0, true /* success */);
                default:
                    log.warning("found a illegal magic code 0x" + Integer.toHexString(magicCode));
                    return new PostPutMessageRequest(-1, false /* success */);
            }

            byte[] bytesContent = new byte[totalSize];

            // 3 BODYCRC
            int bodyCRC = byteBuffer.getInt();

            // 4 QUEUEID
            int queueId = byteBuffer.getInt();

            // 5 QUEUEOFFSET
            long queueOffset = byteBuffer.getLong();

            // 6 PHYSICALOFFSET
            long physicOffset = byteBuffer.getLong();

            // 7 STORETIMESTAMP
            long storeTimestamp = byteBuffer.getLong();

            // 8 BODY
            int bodyLen = byteBuffer.getInt();
            if (bodyLen > 0) {
                byteBuffer.position(byteBuffer.position() + bodyLen);
            }

            // 9 TOPIC
            byte topicLen = byteBuffer.get();
            byteBuffer.get(bytesContent, 0, topicLen);
            String topic = new String(bytesContent, 0, topicLen, LocalMessageDecoder.CHARSET_UTF8);

            // 10 HEADERS
            short headersLength = byteBuffer.getShort();
            if (headersLength > 0) {
                byteBuffer.position(byteBuffer.position() + headersLength);
            }

            // 11 PROPERTIES

            // 获取 properties 尺寸
            short propertiesLength = byteBuffer.getShort();
            if (propertiesLength > 0) {
                byteBuffer.position(byteBuffer.position() + propertiesLength);
            }

            // 重构当前请求
            return new PostPutMessageRequest(//
                    topic, // 1
                    queueId, // 2
                    physicOffset, // 3
                    totalSize, // 4
                    storeTimestamp, // 6
                    queueOffset
            );
        } catch (Exception e) {
            e.printStackTrace();
            return new PostPutMessageRequest(-1, false /* success */);
        }

    }


    /**
     * Description 从 GetMessageResult 中抓取全部的消息
     *
     * @param getMessageResult
     * @return
     */
    public static ArrayList<DefaultBytesMessage> readMessagesFromGetMessageResult(final GetMessageResult getMessageResult) {

        ArrayList<DefaultBytesMessage> messages = new ArrayList<>();

        try {
            List<ByteBuffer> messageBufferList = getMessageResult.getMessageBufferList();
            for (ByteBuffer bb : messageBufferList) {

                messages.add(readMessageFromByteBuffer(bb));
            }
        } finally {
            getMessageResult.release();
        }

        // 获取字节数组

        return messages;
    }



    /**
     * Description 从输入的 ByteBuffer 中反序列化消息对象
     *
     * @return 0 Come the end of the file // >0 Normal messages // -1 Message checksum failure
     */
    public static DefaultBytesMessage readMessageFromByteBuffer(java.nio.ByteBuffer byteBuffer) {

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
                log.warning("found a illegal magic code 0x" + Integer.toHexString(magicCode));
                return null;
        }

        byte[] bytesContent = new byte[totalSize];

        // 3 BODYCRC
        int bodyCRC = byteBuffer.getInt();

        // 4 QUEUEID
        int queueId = byteBuffer.getInt();

        // 5 QUEUEOFFSET
        long queueOffset = byteBuffer.getLong();

        // 6 PHYSICALOFFSET
        long physicOffset = byteBuffer.getLong();

        // 7 STORETIMESTAMP
        long storeTimestamp = byteBuffer.getLong();

        // 8 BODY
        int bodyLen = byteBuffer.getInt();
        byte[] body = new byte[bodyLen];

        if (bodyLen > 0) {
            // 读取并且校验消息体内容
            byteBuffer.get(body, 0, bodyLen);
        }

        // 9 TOPIC
        // 获取主题名长度
        byte topicLen = byteBuffer.get();
        byteBuffer.get(bytesContent, 0, topicLen);
        String topic = new String(bytesContent, 0, topicLen, LocalMessageDecoder.CHARSET_UTF8);

        // 10 HEADERS
        short headersLength = byteBuffer.getShort();
        KeyValue headers = null;
        if (headersLength > 0) {
            byteBuffer.get(bytesContent, 0, headersLength);
            String headersStr = new String(bytesContent, 0, headersLength, LocalMessageDecoder.CHARSET_UTF8);
            headers = string2KeyValue(headersStr);

        }

        // 11 PROPERTIES

        // 获取 properties 尺寸
        short propertiesLength = byteBuffer.getShort();
        KeyValue properties = null;
        if (propertiesLength > 0) {
            byteBuffer.get(bytesContent, 0, propertiesLength);
            String propertiesStr = new String(bytesContent, 0, propertiesLength, LocalMessageDecoder.CHARSET_UTF8);
            properties = string2KeyValue(propertiesStr);

        }

        // 返回读取到的消息
        return new DefaultBytesMessage(
                totalSize,
                topic,
                headers,
                properties,
                body,
                bodyCRC,
                storeTimestamp,
                queueId,
                queueOffset
        );


    }


}
