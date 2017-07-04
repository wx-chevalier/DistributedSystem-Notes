package wx.mq.embedded;

import io.openmessaging.Message;
import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.MessageQueue;
import wx.mq.MessageQueueConfig;
import wx.mq.common.message.status.*;
import wx.mq.embedded.bucket.BucketQueue;
import wx.mq.embedded.bucket.FlushAndUnmapPartitionService;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static wx.mq.embedded.EmbeddedMessageQueueConfig.bucketQueueNum;
import static wx.mq.embedded.EmbeddedMessageQueueConfig.bucketQueuePartitionSize;
import static wx.mq.util.ds.DateTimeUtil.now;

/**
 * Description 巨石型 MessageQueue，仅用于阿里云评测
 */
public class EmbeddedMessageQueue extends MessageQueue {

    private static EmbeddedMessageQueue instance;

    private static Random random = new Random();

    /**
     * Description 获取单例
     *
     * @param storePath
     * @return
     */
    public static synchronized EmbeddedMessageQueue getInstance(String storePath) {

        if (instance == null) {

            instance = new EmbeddedMessageQueue(new EmbeddedMessageQueueConfig(storePath));

            instance.load();

            try {
                instance.start();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return instance;
    }


    // 日志记录
    public final static Logger log = Logger.getLogger(EmbeddedMessageQueue.class.getName());

    // 客户端配置
    MessageQueueConfig messageQueueConfig;

    // 存放桶-队列-BQ 信息
    private ConcurrentHashMap<String/* bucket */, ConcurrentHashMap<Integer/* queueId */, BucketQueue>> bucketQueueTable = new ConcurrentHashMap<>();

    private final ArrayList<FlushAndUnmapPartitionService> flushAndUnmapPartitionServices = new ArrayList<>();

    /**
     * Description 带配置的构造函数
     *
     * @param config
     */
    private EmbeddedMessageQueue(EmbeddedMessageQueueConfig config) {

        // 初始化全局其他依赖
        this.messageQueueConfig = config;

        this.flushAndUnmapPartitionServices.add(new FlushAndUnmapPartitionService(this));

    }


    /**
     * Description 重启之后加载上一次保存的文件
     */
    private void load() {

        try {
            boolean lastExitOK = !this.isTempFileExist();

            // 打印上一次是否正常关闭
            log.info("last shutdown " + (lastExitOK ? "normally" : "abnormally"));

            // load Bucket Queue
            this.loadBucketQueue();

        } catch (Exception e) {
            log.warning("load exception");
        }

    }

    /**
     * Description 将所有的 BucketQueue 加载入内存中
     *
     * @return
     */
    private boolean loadBucketQueue() {

        // 获取对应文件目录
        File bucketQueueDir = new File(((EmbeddedMessageQueueConfig) this.messageQueueConfig).getBucketQueueStorePath());

        // 列举所有文件
        // 列举所有文件
        File[] files = bucketQueueDir.listFiles();

        // 如果非空
        if (files != null) {

            for (File file : files) {

                // 一级目录对应 Bucket 名称
                String bucket = file.getName();

                // 从文件名中获取到 QueueId
                File[] bucketQueueIdList = file.listFiles();

                if (bucketQueueIdList != null) {

                    for (File queueFile : bucketQueueIdList) {
                        int queueId;

                        try {
                            queueId = Integer.parseInt(queueFile.getName());
                        } catch (NumberFormatException e) {
                            continue;
                        }

                        BucketQueue bucketQueue = new BucketQueue(
                                bucket,
                                queueId,
                                bucketQueuePartitionSize,
                                this
                        );

                        // 将该 BucketQueue 加入到 Table 中
                        this.bucketQueueTable.putIfAbsent(bucket, new ConcurrentHashMap<>());

                        this.bucketQueueTable.get(bucket).putIfAbsent(queueId, bucketQueue);

                        // 调用 ConsumeQueue 中的加载函数
                        if (!bucketQueue.load()) {
                            return false;
                        }
                    }
                }
            }
        }

        log.info("load logics queue all over, OK");

        return true;

    }

    /**
     * Description 启动整个服务
     *
     * @throws IOException
     */
    private void start() throws IOException {

        // 创建临时文件
        createTempFile();

        for (FlushAndUnmapPartitionService flushAndUnmapPartitionService : flushAndUnmapPartitionServices) {
            flushAndUnmapPartitionService.start();
        }

        this.shutdown = false;
    }

    @Override
    public MessageQueueConfig getMessageQueueConfig() {
        return this.messageQueueConfig;
    }

    @Override
    public long getConfirmOffset() {
        return 0;
    }

    /**
     * Description 添加单个消息
     *
     * @param bucket
     * @param message
     * @return
     */
    @Override
    public PutMessageResult putMessage(String bucket, Message message) {
        return putMessages(bucket, new ArrayList<>(Arrays.asList((DefaultBytesMessage) message)));
    }


    /**
     * Description Consumer 从存储中读取数据的接口，设计是一次性返回一个 BucketQueue，该 Consumer 负责读完该 BucketQueue
     *
     * @param bucket      待抓取的桶名
     * @param queueId     队列名
     * @param queueOffset 下一个开始抓取的起始下标
     * @return
     */
    public GetMessageResult getMessageByteBuffer(final String bucket, final int queueId, final long queueOffset) {

        // 判断当前 MessageQueue 是否正在运行
        if (this.shutdown) {
            log.warning("message store has shutdown, so getMessage is forbidden");
            return null;
        }

        // 判断是否可读
        if (!this.runningFlags.isReadable()) {
            log.warning("message store is not readable, so getMessage is forbidden " + this.runningFlags.getFlagBits());
            return null;
        }


        long beginTime = now();

        GetMessageStatus status = GetMessageStatus.NO_MESSAGE_IN_QUEUE;

        // 设置下一个开始的偏移
        long nextBeginOffset = queueOffset;

        // 最小偏移
        long minOffset = 0;

        // 最大偏移
        long maxOffset = 0;

        // 查找的返回结果
        GetMessageResult getResult = new GetMessageResult();

        // 判断当前 BucketQueueTable 中是否存在对应的 bucket 以及 queueId
        if (!(this.bucketQueueTable.containsKey(bucket) && this.bucketQueueTable.get(bucket).containsKey(queueId))) {
            // 如果不存在则返回无消息存在
            getResult.setStatus(GetMessageStatus.NO_MATCHED_LOGIC_QUEUE);
            getResult.setNextBeginOffset(0);
            return getResult;
        }


        BucketQueue bucketQueue = this.bucketQueueTable.get(bucket).get(queueId);

        // 目前最大的物理偏移量，即 MessageStore 中最后的消息地址
        maxOffset = bucketQueue.getMaxOffset();

        if (queueOffset == maxOffset) {
            status = GetMessageStatus.OFFSET_OVERFLOW_ONE;
            nextBeginOffset = queueOffset;
        } else if (queueOffset > maxOffset) {
            status = GetMessageStatus.OFFSET_OVERFLOW_BADLY;
            nextBeginOffset = queueOffset;
        }

        // 获取当前待读取的 ByteBuffer
        ByteBuffer byteBuffer = bucketQueue.getMessagesByteBuffer(queueOffset);

        if (byteBuffer != null) {

            // 如果查找到了合适的 ByteBuffer，则进行返回
            status = GetMessageStatus.FOUND;

            // 设置下一轮开始读取的偏移
            nextBeginOffset = bucketQueue.getNextFileOffset(queueOffset);

            minOffset = queueOffset;

            // 设置当前最大的 MaxOffset，即为当前文件尾地址
            maxOffset = nextBeginOffset - 1;

            getResult.setReadByteBuffer(byteBuffer);

        } else {

            // 如果没有找到，则表示已经没有数据了
            status = GetMessageStatus.NO_MESSAGE_IN_QUEUE;
        }

        long eclipseTime = now() - beginTime;

        if (eclipseTime > 2000) {
            log.warning(String.format("getMessage eclipse time(ms)=%s", eclipseTime));
        }

        // 设置获取结果
        getResult.setStatus(status);
        getResult.setNextBeginOffset(nextBeginOffset);
        getResult.setMaxOffset(maxOffset);
        getResult.setMinOffset(minOffset);

        return getResult;
    }

    /**
     * Description 批量添加消息
     *
     * @param bucket
     * @param messages
     * @return
     */
    @Override
    public PutMessageResult putMessages(String bucket, List<DefaultBytesMessage> messages) {

        if (this.shutdown) {
            log.warning("message store has shutdown, so putMessage is forbidden");
            return new PutMessageResult(PutMessageStatus.SERVICE_NOT_AVAILABLE, null);
        }

        // 使用 MessageStore 提交日志以及真实数据
        // 这里还是设置 MessageStore 作为中间层，以保证 MessageQueue 更多关注于业务调度
        long beginTime = now();

        // 寻找合适的 BucketQueue
        BucketQueue bucketQueue = this.findOrCreateBucketQueue(bucket, messages.get(0).getQueueId());

        // 放置所有的消息
        PutMessageResult result = bucketQueue.putMessages(messages);

        long eclipseTime = now() - beginTime;

        if (eclipseTime > 2000) {
            log.warning(String.format("putMessage not in lock eclipse time(ms)=%s, bodyLength=%s, messageNum=%s", eclipseTime, messages.get(0).getBody().length, messages.size()));
        }

        return result;
    }

    /**
     * Description 根据 Bucket 名查找或者创建 BucketQueue
     *
     * @param bucket
     * @param queueId
     * @return
     */

    public synchronized BucketQueue findOrCreateBucketQueue(String bucket, int queueId) {

        ConcurrentHashMap<Integer, BucketQueue> bucketQueueMap = this.bucketQueueTable.get(bucket);

        // 如果为空，则创建新的队列
        if (bucketQueueMap == null) {
            bucketQueueMap = new ConcurrentHashMap<>(bucketQueueNum);
            this.bucketQueueTable.put(bucket, bucketQueueMap);
        }

        BucketQueue bucketQueue = bucketQueueMap.get(queueId);

        // 如果获取为空，则创建新的 BucketQueue
        if (bucketQueue == null) {
            // 创建新的 BucketQueue
            bucketQueue = new BucketQueue(
                    bucket, // 桶名
                    queueId, // 队列名
                    bucketQueuePartitionSize, // 文件大小
                    this
            );

            // 添加到列表中
            bucketQueueMap.put(queueId, bucketQueue);
        }

        return bucketQueue;

    }


    @Override
    public Message pullMessage(String queue, String bucket) {
        return null;
    }

    @Override
    public void flush() {

    }

    @Override
    public void shutdown() {

        // 将当前标志位设置为 true
        this.shutdown = true;

        // 强行关闭 FlushAndUnmap 线程
        for (FlushAndUnmapPartitionService flushAndUnmapPartitionService : flushAndUnmapPartitionServices) {
            flushAndUnmapPartitionService.stop(true);
        }

        // 强行调用 GC 释放全部内存
        System.gc();

        log.info("EmbeddedMessageQueue Shutdown!");

    }

    public ConcurrentHashMap<String, ConcurrentHashMap<Integer, BucketQueue>> getBucketQueueTable() {
        return bucketQueueTable;
    }

    public FlushAndUnmapPartitionService getFlushAndUnmapPartitionService() {
        return flushAndUnmapPartitionServices.get(random.nextInt(10) % this.flushAndUnmapPartitionServices.size());
    }
}
