package wx.mq.local.consume;

import wx.mq.local.LocalMessageQueue;
import wx.mq.common.message.status.SelectMappedBufferResult;
import wx.mq.local.LocalMessageQueueConfig;
import wx.mq.common.partition.fs.MappedPartition;
import wx.mq.common.partition.fs.MappedPartitionQueue;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Logger;

import static wx.mq.local.LocalMessageQueueConfig.mapedFileSizeCommitLog;
import static wx.mq.common.partition.fs.MappedPartitionUtil.getFirstMappedFile;
import static wx.mq.common.partition.fs.MappedPartitionUtil.getLastMappedFile;

/**
 * Description 面向消费者的队列
 */
public class ConsumeQueue {

    // 日志记录
    public final static Logger log = Logger.getLogger(ConsumeQueue.class.getName());

    // ConsumeQueue 文件内存放的单条 Message 尺寸
    // 1 | MessageStore Offset | int 8 Byte
    // 2 | Size | short 8 Byte
    public static final int CQ_STORE_UNIT_SIZE = 12;

    // 核心存储
    private LocalMessageQueue messageStore;

    // 单文件尺寸
    private int mappedFileSize;

    // 存储文件路径
    private String storePath;

    // 主题名或者队列名
    private String topic;

    // 指向当前的队列编号
    private final int queueId;

    // 内存映射文件序列
    private final MappedPartitionQueue mappedPartitionQueue;

    // 缓存
    private final ByteBuffer byteBufferIndex;

    // 最大物理存储偏移量
    private long maxPhysicOffset = -1;

    // 并发变量
    private volatile long minLogicOffset = 0;


    /**
     * Description 主要构造函数
     *
     * @param topic
     * @param queueId
     * @param mappedFileSize
     * @param localMessageStore
     */
    public ConsumeQueue(
            final String topic,
            final int queueId,
            final int mappedFileSize,
            final LocalMessageQueue localMessageStore) {

        this.mappedFileSize = mappedFileSize;

        this.messageStore = localMessageStore;

        this.topic = topic;

        this.queueId = queueId;

        this.storePath = ((LocalMessageQueueConfig) localMessageStore.getMessageQueueConfig()).getStorePathConsumeQueue();

        // 当前队列的路径
        String queueDir = this.storePath
                + File.separator + topic
                + File.separator + queueId;

        // 初始化内存映射队列
        this.mappedPartitionQueue = new MappedPartitionQueue(queueDir, mappedFileSize, null);

        this.byteBufferIndex = ByteBuffer.allocate(CQ_STORE_UNIT_SIZE);

    }

    /**
     * Description 加载当前目录下的所有内存映射文件
     *
     * @return
     */
    public boolean load() {
        boolean result = this.mappedPartitionQueue.load();
        log.info("load consume queue " + this.topic + "-" + this.queueId + " " + (result ? "OK" : "Failed"));
        return result;
    }


    /**
     * Description 将消息放置到某个 ConsumeQueue 中合适的位置
     *
     * @param commitLogOffset
     * @param messageSize
     * @param consumeQueueOffset
     */
    public void putMessagePositionInfoWrapper(final long commitLogOffset, final int messageSize, final long consumeQueueOffset) {

        final int maxRetries = 30;

        // 判断消息是否能写入
        boolean canWrite = this.messageStore.getRunningFlags().isCQWriteable();

        // 重复尝试 N 次
        for (int i = 0; i < maxRetries && canWrite; i++) {

            boolean result = this.putMessagePositionInfo(
                    commitLogOffset,
                    messageSize,
                    consumeQueueOffset
            );

            if (result) {
                return;
            } else {

                log.warning("[Error]put message log position info to " + topic + ":" + queueId + " " + commitLogOffset
                        + " failed, retry " + i + " times");

                // 睡眠 1s 后重试
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.warning("");
                }
            }
        }

        log.warning("[BUG]consume queue can not write," + this.topic + " " + this.queueId);
        this.messageStore.getRunningFlags().makeLogicsQueueError();
    }

    /**
     * Description 将消息位置写入到 ConsumeQueue
     *
     * @param messageOffset MessageStore 中文件的偏移
     * @param size          消息的尺寸
     * @param cqOffset      当前 ConsumeQueue 中的偏移值
     * @return
     */
    private boolean putMessagePositionInfo(final long messageOffset, final int size, final long cqOffset) {


        if (messageOffset <= this.maxPhysicOffset) {
            return true;
        }

        // 重置当前的写入对象
        this.byteBufferIndex.flip();
        this.byteBufferIndex.limit(CQ_STORE_UNIT_SIZE);
        this.byteBufferIndex.putLong(messageOffset);
        this.byteBufferIndex.putInt(size);

        // 计算待写入位置的逻辑偏移量
        final long expectLogicOffset = cqOffset * CQ_STORE_UNIT_SIZE;

        // 获取文件句柄或者
        MappedPartition mappedPartition = this.mappedPartitionQueue.getLastMappedFileOrCreate(expectLogicOffset);

        if (mappedPartition != null) {

            // 如果是首个创建的 ConsumeQueue 文件，但是预期的 cqOffset 不为零，则进行预先填充
            if (mappedPartition.isFirstCreateInQueue() && cqOffset != 0 && mappedPartition.getWrotePosition() == 0) {
                this.minLogicOffset = expectLogicOffset;
                this.mappedPartitionQueue.setFlushedWhere(expectLogicOffset);
                this.mappedPartitionQueue.setCommittedWhere(expectLogicOffset);
                this.fillPreBlank(mappedPartition, expectLogicOffset);
                log.info("fill pre blank space " + mappedPartition.getFileName() + " " + expectLogicOffset + " "
                        + mappedPartition.getWrotePosition());
            }

            if (cqOffset != 0) {
                long currentLogicOffset = mappedPartition.getWrotePosition() + mappedPartition.getFileFromOffset();
                if (expectLogicOffset != currentLogicOffset) {
                    log.warning(
                            String.format(
                                    "[Error] logic queue offset maybe wrong, " +
                                            "expectLogicOffset: %s " +
                                            "currentLogicOffset: %s " +
                                            "Topic: %s " +
                                            "queueId: %s " +
                                            "Diff: %s",
                                    expectLogicOffset,
                                    currentLogicOffset,
                                    this.topic,
                                    this.queueId,
                                    expectLogicOffset - currentLogicOffset)
                    );
                }
            }
            this.maxPhysicOffset = messageOffset;

            // 执行文件追加操作
            return mappedPartition.appendMessage(this.byteBufferIndex.array());
        }
        return false;
    }

    /**
     * Description 在加载完毕后恢复到内存中
     */
    public void recover() {

        // 获取当前映射文件列表
        final List<MappedPartition> mappedPartitions = this.mappedPartitionQueue.getMappedPartitions();

        // 如果为空，则直接返回
        if (mappedPartitions.isEmpty()) {
            return;
        }

        // 判断当前下标
        int index = mappedPartitions.size() - 3;

        if (index < 0)
            index = 0;

        // 当前映射文件尺寸
        int mappedFileSizeLogics = this.mappedFileSize;

        MappedPartition mappedPartition = mappedPartitions.get(index);

        // 创建共享序列
        ByteBuffer byteBuffer = mappedPartition.sliceByteBuffer();

        // 当前文件的整体偏移量，从文件名中获取到的
        long processOffset = mappedPartition.getFileFromOffset();

        // 当前文件内偏移
        long mappedFileOffset = 0;
        while (true) {

            // 遍历
            for (int i = 0; i < mappedFileSizeLogics; i += CQ_STORE_UNIT_SIZE) {

                // 获取消息对应的偏移量
                long offset = byteBuffer.getLong();

                // 获取当前消息尺寸
                int size = byteBuffer.getInt();

                // 如果偏移量与尺寸皆有实际意义
                if (offset >= 0 && size > 0) {
                    mappedFileOffset = i + CQ_STORE_UNIT_SIZE;
                    this.maxPhysicOffset = offset;
                } else {
                    // 否则表示回复失败
                    log.info("recover current consume queue file over,  " + mappedPartition.getFileName() + " "
                            + offset + " " + size);
                    break;
                }
            }

            // 如果正好恢复到了文件尾，则表示恢复成功
            if (mappedFileOffset == mappedFileSizeLogics) {
                index++;
                if (index >= mappedPartitions.size()) {

                    log.info("recover last consume queue file over, last maped file "
                            + mappedPartition.getFileName());
                    break;
                } else {
                    mappedPartition = mappedPartitions.get(index);
                    byteBuffer = mappedPartition.sliceByteBuffer();
                    processOffset = mappedPartition.getFileFromOffset();
                    mappedFileOffset = 0;
                    log.info("recover next consume queue file, " + mappedPartition.getFileName());
                }
            } else {
                log.info("recover current consume queue queue over " + mappedPartition.getFileName() + " "
                        + (processOffset + mappedFileOffset));
                break;
            }
        }

        processOffset += mappedFileOffset;

        // 设置文件的 Flush 位置
        this.mappedPartitionQueue.setFlushedWhere(processOffset);

        // 设置文件的提交位置
        this.mappedPartitionQueue.setCommittedWhere(processOffset);

        // 判断是否需要清除文件
        this.mappedPartitionQueue.truncateDirtyFiles(processOffset);

    }

    /**
     * Description 将 ConsumeQueue 中数据写入到磁盘中
     *
     * @param flushLeastPages
     * @return
     */
    public boolean flush(final int flushLeastPages) {

        return this.mappedPartitionQueue.flush(flushLeastPages);
    }


    /**
     * Description 修正最小偏移量，即修正 ConsumeQueue 包含的在 MessageStore 中偏移量最小的消息
     *
     * @param phyMinOffset
     */
    public void correctMinOffset(long phyMinOffset) {

        // 获取当前内存映射文件队列中的首个文件
        MappedPartition mappedPartition = getFirstMappedFile(this.mappedPartitionQueue);

        // 只要存在映射文件
        if (mappedPartition != null) {

            // 查找第一个消息
            SelectMappedBufferResult result = mappedPartition.selectMappedBuffer(0);
            if (result != null) {
                try {

                    // 遍历获取到的所有 ConsumeQueue 信息
                    for (int i = 0; i < result.getSize(); i += ConsumeQueue.CQ_STORE_UNIT_SIZE) {
                        long offsetPy = result.getByteBuffer().getLong();
                        result.getByteBuffer().getInt();

                        if (offsetPy >= phyMinOffset) {
                            this.minLogicOffset = result.getMappedPartition().getFileFromOffset() + i;
                            log.info(String.format("Compute logical min offset: %s, topic: %s, queueId: %s",
                                    this.getMinOffsetInQueue(), this.topic, this.queueId));

                            break;
                        }
                    }
                } catch (Exception e) {
                    log.warning("Exception thrown when correctMinOffset");
                } finally {
                    result.release();
                }
            }
        }

    }

    private void fillPreBlank(final MappedPartition mappedPartition, final long untilWhere) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(CQ_STORE_UNIT_SIZE);
        byteBuffer.putLong(0L);
        byteBuffer.putInt(Integer.MAX_VALUE);

        int until = (int) (untilWhere % this.mappedPartitionQueue.getMappedFileSize());
        for (int i = 0; i < until; i += CQ_STORE_UNIT_SIZE) {
            mappedPartition.appendMessage(byteBuffer.array());
        }
    }

    public long getMinOffsetInQueue() {
        return this.minLogicOffset / CQ_STORE_UNIT_SIZE;
    }

    public String getTopic() {
        return topic;
    }

    public int getQueueId() {
        return queueId;
    }

    public long getMaxOffsetInQueue() {
        return this.mappedPartitionQueue.getMaxOffset() / CQ_STORE_UNIT_SIZE;
    }


    public SelectMappedBufferResult getIndexBuffer(final long startIndex) {
        int mappedFileSize = this.mappedFileSize;
        long offset = startIndex * CQ_STORE_UNIT_SIZE;
        if (offset >= this.getMinLogicOffset()) {

            // 根据全局偏移量寻找合适的内存映射文件
            MappedPartition mappedPartition = this.mappedPartitionQueue.findMappedFileByOffset(offset);
            if (mappedPartition != null) {
                SelectMappedBufferResult result = mappedPartition.selectMappedBuffer((int) (offset % mappedFileSize));
                return result;
            }
        }
        return null;
    }

    public long getMinLogicOffset() {
        return minLogicOffset;
    }

    public long rollNextFile(final long offset) {
        int mappedFileSize = mapedFileSizeCommitLog;
        return offset + mappedFileSize - offset % mappedFileSize;
    }

    /**
     * Description 根据 MessageStore 中的物理偏移，清除过期的 ConsumeQueue 文件
     *
     * @param phyOffet
     */
    public void truncateDirtyLogicFiles(long phyOffet) {

        int logicFileSize = this.mappedFileSize;

        this.maxPhysicOffset = phyOffet - 1;

        while (true) {

            // 获取最后一个内存映射文件
            MappedPartition mappedPartition = getLastMappedFile(this.mappedPartitionQueue);

            // 如果存在内存映射文件
            if (mappedPartition != null) {
                ByteBuffer byteBuffer = mappedPartition.sliceByteBuffer();

                mappedPartition.setWrotePosition(0);
                mappedPartition.setFlushedPosition(0);

                // 依次遍历读取所有的消息
                for (int i = 0; i < logicFileSize; i += CQ_STORE_UNIT_SIZE) {

                    // 在 MessageStore 中的偏移
                    long offset = byteBuffer.getLong();

                    // 消息尺寸
                    int size = byteBuffer.getInt();

                    // 如果是当前文件中的第一个消息
                    if (0 == i) {

                        // 如果当前偏移量超过了物理文件中的偏移量
                        if (offset >= phyOffet) {
                            this.mappedPartitionQueue.deleteLastMappedFile();
                            break;
                        } else {

                            // 重置当前 ConsumeQueue 对应的 MessageStore 中的偏移
                            int pos = i + CQ_STORE_UNIT_SIZE;
                            mappedPartition.setWrotePosition(pos);
                            mappedPartition.setFlushedPosition(pos);
                            this.maxPhysicOffset = offset;

                        }
                    } else {

                        // 如果不是第一个消息
                        if (offset >= 0 && size > 0) {

                            if (offset >= phyOffet) {
                                return;
                            }

                            // 不断更新当前内存映射文件对应的操作地址
                            int pos = i + CQ_STORE_UNIT_SIZE;
                            mappedPartition.setWrotePosition(pos);
                            mappedPartition.setFlushedPosition(pos);
                            this.maxPhysicOffset = offset;


                            if (pos == logicFileSize) {
                                return;
                            }
                        } else {
                            return;
                        }
                    }
                }
            } else {
                break;
            }
        }

    }

    public void destroy() {
        this.maxPhysicOffset = -1;
        this.minLogicOffset = 0;
        this.mappedPartitionQueue.destroy();
    }

    public int deleteExpiredFile(long offset) {
        int cnt = this.mappedPartitionQueue.deleteExpiredFileByOffset(offset, CQ_STORE_UNIT_SIZE);
        this.correctMinOffset(offset);
        return cnt;
    }

}
