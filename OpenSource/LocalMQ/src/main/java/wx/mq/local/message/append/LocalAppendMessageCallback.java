package wx.mq.local.message.append;

import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.common.message.append.AppendMessageCallback;
import wx.mq.local.message.serialization.LocalMessageEncoder;
import wx.mq.local.message.serialization.LocalMessageSerializer;
import wx.mq.common.message.status.AppendMessageResult;
import wx.mq.common.message.status.AppendMessageStatus;
import wx.mq.local.message.store.MessageStore;
import wx.mq.util.fs.nio.BufferUtil;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Logger;

import static wx.mq.local.message.serialization.LocalMessageSerializer.BLANK_MAGIC_CODE;
import static wx.mq.local.message.serialization.LocalMessageSerializer.END_FILE_MIN_BLANK_LENGTH;
import static wx.mq.util.ds.CollectionTransformer.keyValue2String;
import static wx.mq.util.ds.DateTimeUtil.now;

/**
 * Description 默认的
 */
public class LocalAppendMessageCallback implements AppendMessageCallback {

    // 日志记录
    private final static Logger log = Logger.getLogger(LocalAppendMessageCallback.class.getName());

    // 存放对应的 MessageStore 实例
    private MessageStore messageStore;

    // 存放消息对象的内存
    private final ByteBuffer msgStoreItemMemory;

    // 构建 Message Key
    private final StringBuilder keyBuilder = new StringBuilder();

    /**
     * Description 默认构造函数
     */
    public LocalAppendMessageCallback(MessageStore messageStore) {
        this.messageStore = messageStore;
        this.msgStoreItemMemory = ByteBuffer.allocate(LocalMessageSerializer.MAX_MESSAGE_SIZE + LocalMessageSerializer.END_FILE_MIN_BLANK_LENGTH);
    }


    /**
     * Description 在这里真实地执行对于消息的序列化操作
     *
     * @param fileFromOffset 当前可用文件的总偏移量
     * @param byteBuffer     写入文件的句柄
     * @param maxBlank       剩余空白
     * @param message
     * @return
     */
    public AppendMessageResult doAppend(final long fileFromOffset,
                                        final ByteBuffer byteBuffer,
                                        final int maxBlank,
                                        final DefaultBytesMessage message) {

        // 写入偏移
        long wroteOffset = fileFromOffset + byteBuffer.position();

        final long beginTimeMills = now();

        String key = this.getTopicQueueIdKey(message);

        // 获取当前消息在 TopicQueue 中的偏移
        Long queueOffset = this.messageStore.topicQueueTable.computeIfAbsent(key, k -> 0L);

        // 如果是新创建的值，则重新放置进去
        this.messageStore.topicQueueTable.putIfAbsent(key, queueOffset);

        // 计算 headers
        byte[] headersByteArray = keyValue2String(message.headers()).getBytes();

        // 计算属性的字节数组
        byte[] propertiesByteArray = keyValue2String(message.properties()).getBytes();

        // 判断消息长度
        int messageLength = LocalMessageSerializer.calMsgLength(message, headersByteArray, propertiesByteArray);

        // 判断消息长度是否超出预计
        if (messageLength > LocalMessageSerializer.MAX_MESSAGE_SIZE) {
            log.warning("message size exceeded, msg total size: " + messageLength + ", msg body size: " + message.getBody().length
                    + ", maxMessageSize: " + LocalMessageSerializer.MAX_MESSAGE_SIZE);
            return new AppendMessageResult(AppendMessageStatus.MESSAGE_SIZE_EXCEEDED);
        }

        // 判断是否存在足够空间,如果不存在则突出异常
        if ((messageLength + LocalMessageSerializer.END_FILE_MIN_BLANK_LENGTH) > maxBlank) {

            // 重置当前内部最大大小
            BufferUtil.resetByteBuffer(this.msgStoreItemMemory, maxBlank);

            // 1 存放消息总尺寸
            this.msgStoreItemMemory.putInt(maxBlank);

            // 2 MAGICCODE
            this.msgStoreItemMemory.putInt(BLANK_MAGIC_CODE);

            byteBuffer.put(this.msgStoreItemMemory.array(), 0, maxBlank);

            // 返回当前文件已满，请求写入下一个文件
            return new AppendMessageResult(
                    AppendMessageStatus.END_OF_FILE,
                    wroteOffset,
                    maxBlank,
                    String.valueOf(wroteOffset),
                    message.getStoreTimestamp(),
                    queueOffset,
                    now() - beginTimeMills);
        }

        // 逆向填充 msgStoreItemMemory
        LocalMessageEncoder.encode(
                msgStoreItemMemory,
                fileFromOffset,
                byteBuffer.position(),
                queueOffset,
                message
        );

        // 将消息写入到缓存中
        byteBuffer.put(this.msgStoreItemMemory.array(), 0, messageLength);

        // 将主题对应的 Queue + 1
        this.messageStore.topicQueueTable.put(key, ++queueOffset);

        return new AppendMessageResult(
                AppendMessageStatus.PUT_OK,
                wroteOffset,
                messageLength,
                String.valueOf(wroteOffset),
                message.getStoreTimestamp(),
                queueOffset,
                now() - beginTimeMills
        );

    }

    /**
     * Description 批量写入消息
     *
     * @param offsetFromCommitLogFileName MessageStore 文件名中包含的总体偏移量
     * @param commitLogMappedByteBuffer   来自 MessageStore 的写入句柄
     * @param maxBlank                    当前文件的最大剩余空间
     * @param messages                    需要写入的消息
     * @return
     */
    @Override
    public AppendMessageResult doAppend(
            long offsetFromCommitLogFileName,
            ByteBuffer commitLogMappedByteBuffer,
            int maxBlank,
            List<DefaultBytesMessage> messages) {

        // 起始时间记录
        final long beginTimeMills = now();

        // 获得第一条消息
        DefaultBytesMessage firstMessage = messages.get(0);

        // 当前文件的起始写入位置
        long startPosition = commitLogMappedByteBuffer.position();

        // 当前第一条消息写入位置的全局物理偏移
        long wroteOffset = offsetFromCommitLogFileName + startPosition;

        // 标志当前已写入位置
        commitLogMappedByteBuffer.mark();

        String key = this.getTopicQueueIdKey(firstMessage);

        // 获取当前消息在 TopicQueue 中的偏移
        Long queueOffset = this.messageStore.topicQueueTable.computeIfAbsent(key, k -> 0L);

        // 如果是新创建的值，则重新放置进去
        this.messageStore.topicQueueTable.putIfAbsent(key, queueOffset);

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
                    commitLogMappedByteBuffer.put(this.msgStoreItemMemory.array(), 0, leftBlank);

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
            LocalMessageEncoder.encode(
                    msgStoreItemMemory,
                    offsetFromCommitLogFileName,
                    commitLogMappedByteBuffer.position(),
                    queueOffset,
                    message
            );

            // 将消息写入到缓存中
            commitLogMappedByteBuffer.put(this.msgStoreItemMemory.array(), 0, messageLength);

            // 增加总消息长度
            totalMessageLen += messageLength;

            // 减去剩余空间长度
            leftBlank -= messageLength;

            // 将主题对应的 Queue + 1
            this.messageStore.topicQueueTable.put(key, ++queueOffset);

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
