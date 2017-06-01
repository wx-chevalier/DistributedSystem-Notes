package wx.mq.local.message.serialization;


import io.openmessaging.KeyValue;
import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.common.message.serialization.MessageSerializer;

import java.nio.charset.Charset;
import java.util.logging.Logger;

import static wx.mq.util.ds.CollectionTransformer.keyValue2String;
import static wx.mq.util.encode.CRC.crc32;

/*
| 序号   | 消息存储结构            | 备注                                       | 长度（字节数）              |
| ---- | ----------------- | ---------------------------------------- | -------------------- |
| 1    | TOTALSIZE         | 消息大小                                     | 4                    |
| 2    | MAGICCODE         | 消息的 MAGIC CODE                           | 4                    |
| 3    | BODYCRC           | 消息体 BODY CRC，用于重启时校验                     | 4                    |
| 4    | QUEUEID           | 队列编号，queueID                             | 4                    |
| 5    | QUEUEOFFSET       | 自增值，不是真正的 consume queue 的偏移量，可以代表这个队列中消息的个数，要通过这个值查找到 consume queue 中数据，QUEUEOFFSET * 12 才是偏移地址 | 8                    |
| 6    | PHYSICALOFFSET    | 消息在 commitLog 中的物理起始地址偏移量                | 8                    |
| 7    | STORETIMESTAMP    | 存储时间戳                                    | 8                    |
| 8    | BODY              | 前 4 个字节存放消息体大小值，后 bodyLength 大小的空间存储消息体内容 | 4 + bodyLength       |
| 9    | TOPICORQUEUENAME  | 前 1 个字节存放 Topic 大小，后存放 topicOrQueueNameLength 大小的主题名 | 1 + topicOrQueueNameLength    |
| 10   | headers*          | 前 2 个字节（short）存放头部大小，后存放 headersLength 大小的头部数据 | 2 + headersLength    |
| 11   | properties*       | 前 2 个字节（short）存放属性值大小，后存放 propertiesLength 大小的属性数据 | 2 + propertiesLength |
 */


/**
 * Description 消息序列化
 */
public class LocalMessageSerializer extends MessageSerializer {


    // 日志记录器
    private final static Logger log = Logger.getLogger(LocalMessageSerializer.class.getName());


    // 消息体最大长度 /Byte
    public final static int MAX_MESSAGE_SIZE = 257 * 1024;

    // File at the end of the minimum fixed length empty
    public final static int END_FILE_MIN_BLANK_LENGTH = 4 + 4;

    // 消息序号长度
    public final static int MSG_ID_LENGTH = 8 + 8;

    // 编码
    public final static Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    // MAGICCODE 起始位置
    public final static int MESSAGE_MAGIC_CODE_POSTION = 4;

    // 存放 MessageStore 中消息位置偏移量
    public final static int MESSAGE_PHYSIC_OFFSET_POSTION = 24;

    // 存储时间戳起始位置
    public final static int MESSAGE_STORE_TIMESTAMP_POSTION = 32;

    // 消息的标识
    public final static int MESSAGE_MAGIC_CODE = 0xAABBCCDD ^ 1880681586 + 8;

    // 用于标识文件的最后一个消息，即在空间不够的情况下标识最后一个消息
    public final static int BLANK_MAGIC_CODE = 0xBBCCDDEE ^ 1880681586 + 8;

    // 记录 1~11 项的大小
    private static final int BODY_SIZE_POSITION = 4 // 1 TOTALSIZE
            + 4 // 2 MAGICCODE
            + 4 // 3 BODYCRC
            + 4 // 4 QUEUEID
            + 8 // 5 QUEUEOFFSET
            + 8 // 6 PHYSICALOFFSET
            + 8; // 7 STORETIMESTAMP

    /**
     * Description 计算消息长度
     *
     * @param message
     * @return
     */
    public static int calMsgLength(DefaultBytesMessage message) {


        // 计算 headers
        byte[] headersByteArray = keyValue2String(message.headers()).getBytes();


        // 计算属性的字节数组
        byte[] propertiesByteArray = keyValue2String(message.properties()).getBytes();


        // 计算消息体总长度
        return calMsgLength(message, headersByteArray, propertiesByteArray);

    }


    public static int calMsgLength(DefaultBytesMessage message, byte[] headersByteArray, byte[] propertiesByteArray) {


        // 获取消息头
        KeyValue headers = message.headers();

        // 消息体
        byte[] body = message.getBody();

        int bodyLength = body == null ? 0 : body.length;

        // 获取主题名或者队列名
        byte[] bucket = message.getBucketByteArray();

        // 计算名长度
        byte bucketLength = (byte) bucket.length;

        // 计算头部长度
        short headersLength = (short) headersByteArray.length;

        // 计算属性长度
        short propertiesLength = (short) propertiesByteArray.length;

        // 计算消息体总长度
        return calMsgLength(bodyLength, bucketLength, headersLength, propertiesLength);

    }

    /**
     * @param bodyLength
     * @param topicOrQueueLength
     * @param headersLength
     * @param propertiesLength
     * @return
     * @function 计算当前消息体的总长度
     */
    public static int calMsgLength(int bodyLength, int topicOrQueueLength, int headersLength, int propertiesLength) {
        return BODY_SIZE_POSITION
                + 4 + (bodyLength > 0 ? bodyLength : 0) // 12 BODY
                + 1 + topicOrQueueLength // 13 TOPICORQUEUENAME
                + 2 + (headersLength > 0 ? headersLength : 0) // 14 headers
                + 2 + (propertiesLength > 0 ? propertiesLength : 0);
    }

}

