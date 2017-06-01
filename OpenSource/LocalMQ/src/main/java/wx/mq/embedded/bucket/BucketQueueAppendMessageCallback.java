package wx.mq.embedded.bucket;

import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.common.message.append.AppendMessageCallback;
import wx.mq.common.message.status.AppendMessageResult;
import wx.mq.common.message.status.AppendMessageStatus;
import wx.mq.embedded.message.serialization.EmbeddedMessageEncoder;
import wx.mq.embedded.message.serialization.EmbeddedMessageSerializer;
import wx.mq.util.fs.nio.BufferUtil;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Logger;

import static wx.mq.common.message.serialization.MessageSerializer.BLANK_MAGIC_CODE;
import static wx.mq.common.message.serialization.MessageSerializer.END_FILE_MIN_BLANK_LENGTH;
import static wx.mq.util.ds.DateTimeUtil.now;

/**
 * Description 追加消息回复
 */
public class BucketQueueAppendMessageCallback implements AppendMessageCallback {

    // 日志记录
    private final static Logger log = Logger.getLogger(BucketQueueAppendMessageCallback.class.getName());

    // 指向 BucketQueue 对象
    private final BucketQueue bucketQueue;

    // 存放消息对象的内存
    private final ByteBuffer msgStoreItemMemory;

    /**
     * Description 带参构造函数
     *
     * @param bucketQueue
     */
    BucketQueueAppendMessageCallback(BucketQueue bucketQueue) {

        this.bucketQueue = bucketQueue;

        this.msgStoreItemMemory = ByteBuffer.allocate(EmbeddedMessageSerializer.MAX_MESSAGE_SIZE + END_FILE_MIN_BLANK_LENGTH);

    }

    // 构建 Message Key
    private final StringBuilder keyBuilder = new StringBuilder();

    @Override
    public AppendMessageResult doAppend(long fileOffset, ByteBuffer byteBuffer, int maxBlank, DefaultBytesMessage message) {
        return null;
    }

    @Override
    public AppendMessageResult doAppend(long fileOffset, ByteBuffer byteBuffer, int maxBlank, List<DefaultBytesMessage> messages) {

        // 起始时间记录
        final long beginTimeMills = now();

        // 当前文件的起始写入位置
        long startPosition = byteBuffer.position();

        // 当前第一条消息写入位置的全局物理偏移
        long wroteOffset = fileOffset + startPosition;

        // 标志当前已写入位置
        byteBuffer.mark();

        // 此时 QueueOffset 无实际意义，统一设置为 0
        Long queueOffset = 0l;

        // 总的消息长度
        int totalMessageLen = 0;

        // 剩余空白大小
        int leftBlank = maxBlank;

        // 总的消息数目
        int messageNum = 0;

        // 如果处理的消息数目小于了全部消息数目
        while (messageNum < messages.size()) {

            // 获取当前待处理的消息
            DefaultBytesMessage message = messages.get(messageNum);

            // 计算 headers
            byte[] headersByteArray = message.getHeadersByteArray();

            // 计算属性的字节数组
            byte[] propertiesByteArray = message.getPropertiesByteArray();

            // 判断消息长度
            int messageLength = message.getMessageLength();

            // 如果空间不够，则返回错误
            if ((messageLength + END_FILE_MIN_BLANK_LENGTH) > leftBlank) {

                try {
                    // 重置当前内部最大大小
                    BufferUtil.resetByteBuffer(this.msgStoreItemMemory, leftBlank);

                    // 1 存放消息总尺寸
                    this.msgStoreItemMemory.putInt(maxBlank);

                    // 2 MAGICCODE
                    this.msgStoreItemMemory.putInt(BLANK_MAGIC_CODE);

                    // 将当前文件的剩余内容填充为空
                    byteBuffer.put(this.msgStoreItemMemory.array(), 0, leftBlank);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                // 返回当前文件已满，请求写入下一个文件
                AppendMessageResult appendMessageResult = new AppendMessageResult(
                        AppendMessageStatus.END_OF_FILE,
                        wroteOffset,
                        maxBlank,
                        String.valueOf(wroteOffset),
                        message.getStoreTimestamp(),
                        queueOffset,
                        now() - beginTimeMills);

                // 返回本次已经处理完毕的消息数目
                appendMessageResult.setAppendedMessageNum(messageNum);

                return appendMessageResult;

            }

            // 消息足够的话，则开始写入当前消息
            // 逆向填充 msgStoreItemMemory
            EmbeddedMessageEncoder.encode(
                    message,
                    msgStoreItemMemory,
                    messageLength,
                    headersByteArray,
                    propertiesByteArray);

            // 将消息写入到缓存中
            byteBuffer.put(this.msgStoreItemMemory.array(), 0, messageLength);

            // 增加总消息长度
            totalMessageLen += messageLength;

            // 减去剩余空间长度
            leftBlank -= messageLength;

            // 消息数 + 1
            messageNum++;

        }


        // 返回正常结果
        return new AppendMessageResult(
                AppendMessageStatus.PUT_OK,
                wroteOffset,
                totalMessageLen,
                String.valueOf(wroteOffset),
                now(),
                queueOffset,
                now() - beginTimeMills
        );

    }


    /**
     * Description 计算某个消息对应的主题-QueueId 键
     *
     * @param message
     * @return
     */
    private String getTopicQueueIdKey(DefaultBytesMessage message) {
        keyBuilder.setLength(0);
        keyBuilder.append(message.getTopicOrQueueName());
        keyBuilder.append('-');
        keyBuilder.append(message.getQueueId());

        return keyBuilder.toString();
    }
}
