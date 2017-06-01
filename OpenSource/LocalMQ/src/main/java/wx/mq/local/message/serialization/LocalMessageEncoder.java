package wx.mq.local.message.serialization;

import wx.mq.common.message.DefaultBytesMessage;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import static wx.mq.util.fs.nio.BufferUtil.resetByteBuffer;

/**
 * Description 消息编码类
 */
public class LocalMessageEncoder extends LocalMessageSerializer {


    // 日志记录
    private final static Logger log = Logger.getLogger(LocalMessageEncoder.class.getName());

    /**
     * Description 将消息内容编码写入到 Buffer 中
     *
     * @param byteBuffer
     * @param offsetFromFile
     * @param positionInFile
     * @param queueOffset
     * @param message
     */
    public static void encode(
            final ByteBuffer byteBuffer,
            final long offsetFromFile,
            final long positionInFile,
            Long queueOffset,
            DefaultBytesMessage message
    ) {

        // 消息体
        byte[] body = message.getBody();

        int bodyLength = body == null ? 0 : body.length;

        // 获取主题名或者队列名
        byte[] bucket = message.getBucketByteArray();

        // 计算名长度
        byte bucketLength = (byte) bucket.length;

        // 计算头部长度
        short headersLength = (short) message.getHeadersByteArray().length;

        // 计算属性长度
        short propertiesLength = (short) message.getPropertiesByteArray().length;

        // 初始化存储空间
        resetByteBuffer(byteBuffer, message.getMessageLength());

        // 1 TOTALSIZE
        byteBuffer.putInt(message.getMessageLength());

        // 2 MAGICCODE
        byteBuffer.putInt(MESSAGE_MAGIC_CODE);

        // 3 BODYCRC
        byteBuffer.putInt(message.getBodyCRC());

        // 4 QUEUEID
        byteBuffer.putInt(message.getQueueId());

        // 5 QUEUEOFFSET
        byteBuffer.putLong(queueOffset);

        message.setQueueOffset(queueOffset);

        // 6 PHYSICALOFFSET
        byteBuffer.putLong(offsetFromFile + positionInFile);

        message.setCommitLogOffset(offsetFromFile + positionInFile);

        // 7 STORETIMESTAMP
        byteBuffer.putLong(message.getStoreTimestamp());

        // 8 BODY
        byteBuffer.putInt(bodyLength);
        if (bodyLength > 0)
            byteBuffer.put(message.getBody());

        // 9 TOPICORQUEUENAME
        byteBuffer.put((byte) bucketLength);

        byteBuffer.put(bucket);

        // 10 HEADERS
        byteBuffer.putShort((short) headersLength);
        if (headersLength > 0)
            byteBuffer.put(message.getHeadersByteArray());

        // 11 PROPERTIES
        byteBuffer.putShort((short) propertiesLength);
        if (propertiesLength > 0)
            byteBuffer.put(message.getPropertiesByteArray());

    }

}
