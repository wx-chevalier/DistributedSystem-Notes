package wx.mq.local.message.store;

import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.common.message.append.AppendMessageCallback;
import wx.mq.common.message.status.AppendMessageResult;
import wx.mq.common.message.status.PutMessageResult;
import wx.mq.common.message.status.SelectMappedBufferResult;
import wx.mq.local.LocalMessageQueue;
import wx.mq.local.LocalMessageQueueConfig;
import wx.mq.local.message.service.FlushMessageStoreService;
import wx.mq.util.encode.CRC;
import wx.mq.common.partition.fs.MappedPartition;
import wx.mq.common.partition.fs.MappedPartitionQueue;
import wx.mq.common.partition.fs.MappedPartitionUtil;
import wx.mq.local.message.append.LocalAppendMessageCallback;
import wx.mq.common.message.status.PutMessageStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import static wx.mq.local.LocalMessageQueueConfig.*;
import static wx.mq.util.ds.DateTimeUtil.now;
import static wx.mq.common.partition.fs.MappedPartitionUtil.getFirstMappedFile;
import static wx.mq.common.partition.fs.MappedPartitionUtil.getLastMappedFile;

/**
 * Description 提交日志
 */
public class MessageStore {

    // 日志记录器
    private final static Logger log = Logger.getLogger(LocalMessageQueue.class.getName());

    // 存放全局 Store
    private LocalMessageQueue messageStore;

    // 追加信息的回调
    private final AppendMessageCallback appendMessageCallback;

    // 并发锁，避免多个 Producer 冲突
    private ReentrantLock putMessageNormalLock = new ReentrantLock();

    //true: Can lock, false : in lock.
    private AtomicBoolean putMessageSpinLock = new AtomicBoolean(true);

    // 记录上锁时间
    private volatile long beginTimeInLock = 0;

    // 定期将 FileChannel 中内容持久化存储到磁盘中的服务
    private final FlushMessageStoreService flushMessageStoreService;

    // 内存映射文件队列
    private final MappedPartitionQueue mappedPartitionQueue;

    // 存放主题-桶的相关信息
    /* topic-queueID - offset */
    public HashMap<String, Long> topicQueueTable = new HashMap<String, Long>(1024);

    // 可确认的偏移量
    private long confirmOffset;


    /**
     * Description 构造函数
     *
     * @param messageStore
     */
    public MessageStore(LocalMessageQueue messageStore) {

        this.messageStore = messageStore;

        // 初始化 Flush Commit Log 服务
        this.flushMessageStoreService = new FlushMessageStoreService(this);

        // 构造映射文件类
        this.mappedPartitionQueue = new MappedPartitionQueue(
                ((LocalMessageQueueConfig) this.messageStore.getMessageQueueConfig()).getStorePathCommitLog(),
                mapedFileSizeCommitLog,
                messageStore.getAllocateMappedPartitionService(),
                this.flushMessageStoreService
        );

        // 初始化追加回调
        this.appendMessageCallback = new LocalAppendMessageCallback(this);

    }

    /**
     * Description 重启之后完成数据加载
     */
    public boolean load() {

        // 调用加载 MappedPartitionQueue 数据
        boolean result = this.mappedPartitionQueue.load();

        log.info("load message log " + (result ? "OK" : "Failed"));

        return result;

    }

    /**
     * Description 启动内部服务
     */
    public void start() {

        // 启动定期 Flush 服务
        this.flushMessageStoreService.start();
    }

    /**
     * Description 实际持久化存储消息
     *
     * @param message
     * @return
     */
    public PutMessageResult putMessage(DefaultBytesMessage message) {

        // 对消息进行额外属性设置
        // 设置消息所有的存储时间

        // 设置消息体的 CRC 验证
        if (isCRCEnabled) {
            message.setBodyCRC(CRC.crc32(message.getBody()));
        } else {
            message.setBodyCRC(0);
        }

        // 添加消息的结果
        AppendMessageResult result = null;

        long eclipseTimeInLock = 0;

        // 获取最后一个内存映射文件
        MappedPartition mappedPartition = MappedPartitionUtil.getLastMappedFile(this.mappedPartitionQueue);

        // 闭锁
        this.lockForPutMessage();

        try {
            // 上锁时间
            this.beginTimeInLock = now();

            message.setStoreTimestamp(this.beginTimeInLock);

            // 如果不存在有效映射文件或者当前映射文件已满，则创建
            if (null == mappedPartition || mappedPartition.isFull()) {
                mappedPartition = this.mappedPartitionQueue.getLastMappedFileOrCreate(0);
            }

            // 创建失败则返回异常
            if (null == mappedPartition) {
                log.warning("创建 MappedPartition 错误, topic: " + message.getTopicOrQueueName());
                beginTimeInLock = 0;
                return new PutMessageResult(PutMessageStatus.CREATE_MAPEDFILE_FAILED, null);
            }

            // 调用对应的 MappedPartition 追加消息
            result = mappedPartition.appendMessage(message, this.appendMessageCallback);

            // 根据追加结果进行不同的操作
            switch (result.getStatus()) {
                case PUT_OK:
                    break;
                case END_OF_FILE:

                    // 如果已经到了文件最后，则创建新文件
                    mappedPartition = this.mappedPartitionQueue.getLastMappedFileOrCreate(0);

                    if (null == mappedPartition) {
                        // XXX: warn and notify me
                        log.warning("创建 MappedPartition 错误, topic: " + message.getTopicOrQueueName());
                        beginTimeInLock = 0;
                        return new PutMessageResult(PutMessageStatus.CREATE_MAPEDFILE_FAILED, result);
                    }
                    // 否则重新进行添加操作
                    result = mappedPartition.appendMessage(message, this.appendMessageCallback);
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

            // 记录执行时间
            eclipseTimeInLock = now() - beginTimeInLock;
            beginTimeInLock = 0;

        } finally {
            // 避免死锁
            // 开锁
            this.releasePutMessageLock();
        }

        PutMessageResult putMessageResult = new PutMessageResult(PutMessageStatus.PUT_OK, result);

        return putMessageResult;

    }


    public PutMessageResult putMessages(List<DefaultBytesMessage> messages) {

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

        // 闭锁
        this.lockForPutMessage();

        try {
            // 上锁时间
            this.beginTimeInLock = now();

            // 如果不存在有效映射文件或者当前映射文件已满，则创建
            if (null == mappedPartition || mappedPartition.isFull()) {
                mappedPartition = this.mappedPartitionQueue.getLastMappedFileOrCreate(0);
            }

            // 创建失败则返回异常
            if (null == mappedPartition) {
                log.warning("创建 MappedPartition 错误, topic: " + messages.get(0).getTopicOrQueueName());
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
                    result = mappedPartition.appendMessages(messages, this.appendMessageCallback);

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

        } catch (Exception exception) {

            // 这里线上环境不应该出现异常
            exception.printStackTrace();

        } finally {
            // 避免死锁
            // 开锁
            this.releasePutMessageLock();
        }

        PutMessageResult putMessageResult = new PutMessageResult(PutMessageStatus.PUT_OK, result);

        return putMessageResult;
    }

    public MappedPartitionQueue getMappedPartitionQueue() {
        return mappedPartitionQueue;
    }

    public LocalMessageQueue getMessageStore() {
        return messageStore;
    }

    public long getBeginTimeInLock() {
        return beginTimeInLock;
    }

    public long getMaxOffset() {
        return this.mappedPartitionQueue.getMaxOffset();
    }

    /**
     * Description 获取当前 MessageStore 包含的多个文件中的最小位移值
     *
     * @return
     */
    public long getMinOffset() {

        // 获取当前队列中的首个文件
        MappedPartition mappedPartition = getFirstMappedFile(mappedPartitionQueue);

        // 如果文件不为空
        if (mappedPartition != null) {

            // 当当前文件可用时，直接获取其文件名
            if (mappedPartition.isAvailable()) {
                return mappedPartition.getFileFromOffset();

            } else {

                // 否则获取下一个文件的偏移值
                return this.rollNextFile(mappedPartition.getFileFromOffset());
            }
        }

        // 如果文件为空，则表示最小偏移为 -1
        return -1;
    }

    /**
     * @param offset
     * @return
     * @function 滚动到下一个文件
     */
    public long rollNextFile(final long offset) {
        int mappedFileSize = mapedFileSizeCommitLog;
        return offset + mappedFileSize - offset % mappedFileSize;
    }

    public void setTopicQueueTable(HashMap<String, Long> topicQueueTable) {
        this.topicQueueTable = topicQueueTable;
    }

    public long getConfirmOffset() {
        return confirmOffset;
    }

    /**
     * Description 根据总的偏移量从多个文件中获取读取的句柄
     * Read MessageStore data, use data replication
     */
    public SelectMappedBufferResult getData(final long offset) {
        return this.getData(offset, offset == 0);
    }

    /**
     * Description 根据 MessageStore 中总体偏移量获取对应的内存映射文件
     *
     * @param offset
     * @param returnFirstOnNotFound
     * @return
     */
    public SelectMappedBufferResult getData(final long offset, final boolean returnFirstOnNotFound) {
        MappedPartition mappedPartition = this.mappedPartitionQueue.findMappedFileByOffset(offset, returnFirstOnNotFound);
        if (mappedPartition != null) {
            int pos = (int) (offset % mapedFileSizeCommitLog);
            return mappedPartition.selectMappedBuffer(pos);
        }

        return null;
    }


    /**
     * Description 根据设定的偏移量与消息尺寸，返回待读取的消息的 ByteBuffer
     *
     * @param messageOffsetInCommitLog 待读取的消息在全局的偏移值
     * @param size
     * @return
     */
    public SelectMappedBufferResult getMessage(final long messageOffsetInCommitLog, final int size) {

        // 设置当前 MessageStore 文件尺寸
        int mappedFileSize = mapedFileSizeCommitLog;

        // 如果 offset 为零的话，则在没有返回值的情况下返回第一个有效值
        MappedPartition mappedPartition = this.mappedPartitionQueue.findMappedFileByOffset(messageOffsetInCommitLog, messageOffsetInCommitLog == 0);

        // 如果返回了有效的 MappedPartition
        if (mappedPartition != null) {

            // 获取单个文件中的位置
            int pos = (int) (messageOffsetInCommitLog % mappedFileSize);

            // 在该文件中寻找对应的读取的 ByteBuffer 句柄
            return mappedPartition.selectMappedBuffer(pos, size);
        }
        return null;
    }

    public void setConfirmOffset(long confirmOffset) {
        this.confirmOffset = confirmOffset;
    }

    public void shutdown() {

        this.flushMessageStoreService.shutdown();

        this.mappedPartitionQueue.shutdown(0);

    }


    /**
     * Description 为不同的 Producer 写入 MessageStore 加锁
     */

    private void lockForPutMessage() {
        if (useReentrantLockWhenPutMessage) {
            putMessageNormalLock.lock();
        } else {
            boolean flag;
            do {
                flag = this.putMessageSpinLock.compareAndSet(true, false);
            }
            while (!flag);
        }
    }

    private void releasePutMessageLock() {
        if (useReentrantLockWhenPutMessage) {
            putMessageNormalLock.unlock();
        } else {
            this.putMessageSpinLock.compareAndSet(false, true);
        }
    }

    public FlushMessageStoreService getFlushMessageStoreService() {
        return flushMessageStoreService;
    }

}
