package wx.mq.embedded.bucket;

import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.common.message.append.AppendMessageCallback;
import wx.mq.common.message.status.AppendMessageResult;
import wx.mq.common.message.status.PutMessageResult;
import wx.mq.common.message.status.PutMessageStatus;
import wx.mq.common.message.status.SelectMappedBufferResult;
import wx.mq.embedded.EmbeddedMessageQueue;
import wx.mq.embedded.EmbeddedMessageQueueConfig;
import wx.mq.util.encode.CRC;
import wx.mq.common.partition.fs.MappedPartition;
import wx.mq.common.partition.fs.MappedPartitionQueue;
import wx.mq.common.partition.fs.MappedPartitionUtil;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import static wx.mq.MessageQueueConfig.isCRCEnabled;
import static wx.mq.util.ds.DateTimeUtil.now;

public class BucketQueue {

    // 日志记录
    public final static Logger log = Logger.getLogger(BucketQueue.class.getName());

    // 核心存储
    private EmbeddedMessageQueue messageQueue;

    // 桶名
    private final String bucket;

    // 队列编号
    private final int queueId;

    // 单文件尺寸
    private final int mappedFileSize;

    // 内存映射文件序列
    private final MappedPartitionQueue mappedPartitionQueue;

    // 写锁
    private final ReentrantLock writeLock = new ReentrantLock();

    // 开始上锁时间
    private long beginTimeInLock = now();

    private AppendMessageCallback appendMessageCallback;

    public BucketQueue(
            String bucket,
            int queueId,
            int bucketQueuePartitionSize,
            EmbeddedMessageQueue embeddedMessageQueue
    ) {

        this.bucket = bucket;

        this.queueId = queueId;

        this.mappedFileSize = bucketQueuePartitionSize;

        this.messageQueue = embeddedMessageQueue;

        // 当前队列的路径
        String queueDir = ((EmbeddedMessageQueueConfig) messageQueue.getMessageQueueConfig()).getBucketQueueStorePath()
                + File.separator + bucket
                + File.separator + queueId;

        // 初始化内存映射队列
        this.mappedPartitionQueue = new MappedPartitionQueue(queueDir, mappedFileSize, null);

        this.appendMessageCallback = new BucketQueueAppendMessageCallback(this);

    }

    /**
     * Description 批量添加消息操作
     *
     * @param messages
     * @return
     */
    public PutMessageResult putMessages(List<DefaultBytesMessage> messages) {

        // 对消息进行额外属性设置
        for (DefaultBytesMessage message : messages) {
            // 设置消息体的 CRC 验证
            if (isCRCEnabled) {
                message.setBodyCRC(CRC.crc32(message.getBody()));
            } else {
                message.setBodyCRC(0);
            }
        }

        // 添加消息的结果
        AppendMessageResult result = null;

        // 获取最后一个内存映射文件
        MappedPartition mappedPartition = MappedPartitionUtil.getLastMappedFile(this.mappedPartitionQueue);

        this.writeLock.lock();

        try {

            // 上锁时间
            this.beginTimeInLock = now();

            // 如果不存在有效映射文件或者当前映射文件已满，则创建
            if (null == mappedPartition || mappedPartition.isFull()) {
                mappedPartition = this.mappedPartitionQueue.getLastMappedFileOrCreate(0);
            }

            // 创建失败则返回异常
            if (null == mappedPartition) {
                log.warning("创建 MappedPartition 错误, topic: " + messages.get(0).getBucket());
                beginTimeInLock = 0;
                return new PutMessageResult(PutMessageStatus.CREATE_MAPEDFILE_FAILED, null);
            }

            // 调用对应的 MappedPartition 追加消息
            // 注意，这里经过填充之后，会逆向地将消息在 MessageStore 中的偏移与 QueueOffset 中偏移添加进去
            result = mappedPartition.appendMessages(messages, this.appendMessageCallback);

            // 根据追加结果进行不同的操作
            switch (result.getStatus()) {
                case PUT_OK:
                    break;
                case END_OF_FILE:

                    this.messageQueue.getFlushAndUnmapPartitionService().putPartition(mappedPartition);

                    // 如果已经到了文件最后，则创建新文件
                    mappedPartition = this.mappedPartitionQueue.getLastMappedFileOrCreate(0);

                    if (null == mappedPartition) {
                        // XXX: warn and notify me
                        log.warning("创建 MappedPartition 错误, topic: " + messages.get(0).getTopicOrQueueName());
                        beginTimeInLock = 0;
                        return new PutMessageResult(PutMessageStatus.CREATE_MAPEDFILE_FAILED, result);
                    }
                    // 否则重新进行添加操作
                    // 从结果中获取处理完毕的消息数
                    int appendedMessageNum = result.getAppendedMessageNum();

                    // 创建临时的 LeftMessages
                    ArrayList<DefaultBytesMessage> leftMessages = new ArrayList<>();

                    // 添加所有未消费的消息
                    for (int i = appendedMessageNum; i < messages.size(); i++) {
                        leftMessages.add(messages.get(i));
                    }

                    result = mappedPartition.appendMessages(leftMessages, this.appendMessageCallback);

                    break;
                case MESSAGE_SIZE_EXCEEDED:
                case PROPERTIES_SIZE_EXCEEDED:
                    beginTimeInLock = 0;
                    return new PutMessageResult(PutMessageStatus.MESSAGE_ILLEGAL, result);
                case UNKNOWN_ERROR:
                    beginTimeInLock = 0;
                    return new PutMessageResult(PutMessageStatus.UNKNOWN_ERROR, result);
                default:
                    beginTimeInLock = 0;
                    return new PutMessageResult(PutMessageStatus.UNKNOWN_ERROR, result);
            }

            beginTimeInLock = 0;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.writeLock.unlock();
        }

        PutMessageResult putMessageResult = new PutMessageResult(PutMessageStatus.PUT_OK, result);

        return putMessageResult;
    }

    /**
     * Description 加载当前目录下的所有内存映射文件
     *
     * @return
     */
    public boolean load() {
        boolean result = this.mappedPartitionQueue.load();
//        log.info("load consume queue " + this.bucket + "-" + this.queueId + " " + (result ? "OK" : "Failed"));
        return result;
    }

    public ByteBuffer getMessagesByteBuffer(final long queueOffset) {

        // 查找该 queueOffset 对应的 MappedPartition
        // 如果 offset 为零的话，则在没有返回值的情况下返回第一个有效值
        MappedPartition mappedPartition = this.mappedPartitionQueue.findMappedFileByOffset(queueOffset, queueOffset == 0);
        if (mappedPartition != null) {
            int pos = (int) (queueOffset % this.mappedFileSize);

            SelectMappedBufferResult selectMappedBufferResult = mappedPartition.selectMappedBuffer(pos);

            if (selectMappedBufferResult != null) {
                return selectMappedBufferResult.getByteBuffer();
            } else {
                return null;
            }
        }

        return null;
    }

    /**
     * Description 获取当前位置开始的下一个位置
     *
     * @param offset
     * @return
     */
    public long getNextFileOffset(final long offset) {
        return offset + this.mappedFileSize - offset % this.mappedFileSize;
    }

    /**
     * Description 返回当前 MappedPartitionQueue 包含的最大的位移
     *
     * @return
     */
    public long getMaxOffset() {
        return this.mappedPartitionQueue.getMaxOffset();
    }

    public void flush() {
        this.mappedPartitionQueue.flush(0);
    }
}
