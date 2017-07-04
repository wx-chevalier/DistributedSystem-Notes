package wx.mq.local;

import io.openmessaging.Message;
import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.MessageQueue;
import wx.mq.MessageQueueConfig;
import wx.mq.common.client.config.ConsumerScheduler;
import wx.mq.local.message.store.MessageStore;
import wx.mq.local.message.service.PostPutMessageRequest;
import wx.mq.local.consume.ConsumeQueue;
import wx.mq.local.consume.service.CleanConsumeQueueService;
import wx.mq.local.consume.service.FlushConsumeQueueService;
import wx.mq.common.message.status.*;
import wx.mq.local.message.store.MessageStoreRecover;
import wx.mq.local.message.service.PostPutMessageService;
import wx.mq.common.partition.fs.allocate.AllocateMappedPartitionService;
import wx.mq.util.sys.ThreadFactoryImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static wx.mq.util.ds.DateTimeUtil.now;
import static wx.mq.util.fs.FSExtra.*;

/**
 * Description 消息存储于持久化
 * <p>
 * 文件的存储格式是
 * messageStore
 * -- MapFile1
 * -- MapFile2
 * consumequeue
 * -- Topic1
 * ---- queueId1
 * ------ MapFile1
 * ------ MapFile2
 * ---- queueId2
 * ------ MapFile1
 * ------ MapFile2
 * -- Queue1
 * ---- queueId1
 * ------ MapFile1
 * ------ MapFile2
 * ---- queueId2
 * ------ MapFile1
 * ------ MapFile2
 */
public class LocalMessageQueue extends MessageQueue {

    private static ConcurrentHashMap<String, LocalMessageQueue> instances = new ConcurrentHashMap<>();

    MessageQueueConfig messageQueueConfig;

    public static synchronized LocalMessageQueue getInstance(String storePath) {

        if (instances.get(storePath) == null) {

            LocalMessageQueue INSTANCE = new LocalMessageQueue(new LocalMessageQueueConfig(storePath));

            INSTANCE.load();

            try {
                INSTANCE.start();
            } catch (Exception e) {
                e.printStackTrace();
            }

            instances.put(storePath, INSTANCE);
        }

        return instances.get(storePath);
    }

    // 日志记录
    public final static Logger log = Logger.getLogger(LocalMessageQueue.class.getName());

    // 内部的定期任务执行调度器
    private final ScheduledExecutorService scheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("LocalMessageStoreScheduledThread"));

    // 提交者
    private MessageStore messageStore;

    // 记录 ConsumeQueue 对应信息
    private ConcurrentHashMap<String/* topic */, ConcurrentHashMap<Integer/* queueId */, ConsumeQueue>> consumeQueueTable;

    // 定义服务

    // 预分配 MappedPartition 服务
    private final AllocateMappedPartitionService allocateMappedPartitionService;

    // 定期将 ConsumeQueue 写入到磁盘
    private final FlushConsumeQueueService flushConsumeQueueService;

    // 定期清理过期的 ConsumeQueue 的服务
    private final CleanConsumeQueueService cleanConsumeQueueService;

    // 定期执行的消息后处理服务
    private final PostPutMessageService postPutMessageService;

    // 客户端调度管理器
    private final ConsumerScheduler consumerScheduler;

    /**
     * Description 默认构造函数
     */
    private LocalMessageQueue(LocalMessageQueueConfig messageQueueConfig) {

        // 初始化全局其他依赖
        this.messageQueueConfig = messageQueueConfig;

        // 初始化预分配内存映射文件服务
        this.allocateMappedPartitionService = new AllocateMappedPartitionService(this);

        // MessageStore
        this.messageStore = new MessageStore(this);

        // 初始化内部变量

        // 初始化 ConsumeQueue 表
        this.consumeQueueTable = new ConcurrentHashMap<>(32);

        // 初始化内部服务

        // 初始化定期将 ConsumeQueue 写入到磁盘服务
        this.flushConsumeQueueService = new FlushConsumeQueueService(this);

        // 初始化定期清理 ConsumeQueue 中过期内容的服务
        this.cleanConsumeQueueService = new CleanConsumeQueueService(this);

        // 初始化消息后处理服务
        this.postPutMessageService = new PostPutMessageService(this);

        // 客户端调度管理器
        this.consumerScheduler = new ConsumerScheduler(this);

        // 启动预分配服务
        this.allocateMappedPartitionService.start();

    }


    /**
     * Description 加载 MessageStore 以及 ConsumeQueue 文件
     * <p>
     * load 函数会在初始化对象时执行
     *
     * @return
     * @throws IOException
     */
    public boolean load() {

        boolean result = true;

        try {
            boolean lastExitOK = !this.isTempFileExist();

            // 打印上一次是否正常关闭
            log.info("last shutdown " + (lastExitOK ? "normally" : "abnormally"));

            // 加载 Consumer Scheduler Consumer Scheduler Config
            this.consumerScheduler.load();

            // 加载 MessageStore
            result = result && this.messageStore.load();

            // 加载 Consume Queue
            result = result && this.loadConsumeQueue();

            // 如果全局加载成功，则继续恢复
            if (result) {

                this.recover(lastExitOK);

                // 打印目前最大的物理偏移量
                log.info("加载完毕, 目前最大物理偏移量 = " + this.getMaxPhyOffset());
            }
        } catch (Exception e) {
            log.warning("load exception");
            result = false;
        }

        if (!result) {
            this.allocateMappedPartitionService.shutdown();
        }

        return result;
    }

    /**
     * Description 手动启动服务
     */
    public void start() throws Exception {

        // 启动 ConsumerScheduler
        this.consumerScheduler.start();

        // 进行 MessageService 处理
        this.postPutMessageService.start();

        // 启动 Flush 服务
        this.flushConsumeQueueService.start();

        // 启动 MessageStore
        this.messageStore.start();

        // 根据是否允许消息重复设置不同的起始位置
        if (LocalMessageQueueConfig.duplicationEnable) {
            this.postPutMessageService.setReputFromOffset(this.messageStore.getConfirmOffset());
        } else {
            this.postPutMessageService.setReputFromOffset(this.messageStore.getMaxOffset());
        }

        // 创建临时文件
        createTempFile();

        this.shutdown = false;

    }

    /**
     * Description 强制将 MessageQueue 中的消息持久化到磁盘
     *
     * @throws Exception
     */
    @Override
    public void flush() {

        // 强制将 MessageStore 中数据持久化到磁盘
        this.messageStore.getMappedPartitionQueue().flush(0);

        // 依次将 ConsumeQueue 持久化到磁盘
        for (ConcurrentHashMap<Integer, ConsumeQueue> maps : this.consumeQueueTable.values()) {
            for (ConsumeQueue cq : maps.values()) {
                // 强制进行 Flush 操作
                cq.flush(0);
            }
        }


    }

    /**
     * Description 存放信息
     *
     * @param topicOrQueueName
     * @param msg              需要存储的消息，注意，在 Message Headers 的 Flag 属性中存放了 Producer 的唯一标识
     * @return
     */
    @Override
    public PutMessageResult putMessage(String topicOrQueueName, Message msg) {

        DefaultBytesMessage message = (DefaultBytesMessage) msg;

        if (this.shutdown) {
            log.warning("message store has shutdown, so putMessage is forbidden");
            return new PutMessageResult(PutMessageStatus.SERVICE_NOT_AVAILABLE, null);
        }

        // 使用 MessageStore 提交日志以及真实数据
        // 这里还是设置 MessageStore 作为中间层，以保证 MessageQueue 更多关注于业务调度
        long beginTime = now();

        // 使用 MessageStore 进行提交
        PutMessageResult result = this.messageStore.putMessage(message);

        long eclipseTime = now() - beginTime;

        if (eclipseTime > 500) {
            log.warning(String.format("putMessage not in lock eclipse time(ms)=%s, bodyLength=%s", eclipseTime, message.getBody().length));
        }

        return result;
    }

    @Override
    public PutMessageResult putMessages(String topicOrQueueName, List<DefaultBytesMessage> messages) {

        if (this.shutdown) {
            log.warning("message store has shutdown, so putMessage is forbidden");
            return new PutMessageResult(PutMessageStatus.SERVICE_NOT_AVAILABLE, null);
        }

        // 使用 MessageStore 提交日志以及真实数据
        // 这里还是设置 MessageStore 作为中间层，以保证 MessageQueue 更多关注于业务调度
        long beginTime = now();

        // 使用 MessageStore 进行提交
        PutMessageResult result = this.messageStore.putMessages(messages);

        long eclipseTime = now() - beginTime;

        if (eclipseTime > 2000) {
            log.warning(String.format("putMessage not in lock eclipse time(ms)=%s, bodyLength=%s, messageNum=%s", eclipseTime, messages.get(0).getBody().length, messages.size()));
        }

        return result;
    }


    /**
     * Description 将消息的位置放置到 ConsumeQueue 中
     *
     * @param postPutMessageRequest
     */
    public void putMessagePositionInfo(PostPutMessageRequest postPutMessageRequest) {

        // 寻找或者创建 ConsumeQueue
        ConsumeQueue cq = this.findConsumeQueue(postPutMessageRequest.getTopic(), postPutMessageRequest.getQueueId());

        // 将消息放置到 ConsumeQueue 中合适的位置
        cq.putMessagePositionInfoWrapper(postPutMessageRequest.getCommitLogOffset(), postPutMessageRequest.getMsgSize(), postPutMessageRequest.getConsumeQueueOffset());

    }

    @Override
    public Message pullMessage(String queue, String bucket) {
        return null;
    }

    /**
     * Description Consumer 从存储中读取数据的接口
     *
     * @param topic
     * @param queueId
     * @param offset     下一个开始抓取的起始下标
     * @param maxMsgNums
     * @return
     */
    public GetMessageResult getMessage(final String topic, final int queueId, final long offset, final int maxMsgNums) {

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
        long nextBeginOffset = offset;

        // 最小偏移
        long minOffset = 0;

        // 最大偏移
        long maxOffset = 0;

        // 查找的返回结果
        GetMessageResult getResult = new GetMessageResult();

        // 目前最大的物理偏移量，即 MessageStore 中最后的消息地址
        final long maxOffsetPy = this.messageStore.getMaxOffset();

        // 根据 Topic 与 queueId 构建消费者队列
        ConsumeQueue consumeQueue = findConsumeQueue(topic, queueId);


        // 保证当前 ConsumeQueue 存在
        if (consumeQueue != null) {

            // 获取当前 ConsumeQueue 中包含的最小的消息在 MessageStore 中的位移
            minOffset = consumeQueue.getMinOffsetInQueue();

            // 注意，最大的位移地址即是不可达地址，是当前所有消息的下一个消息的下标
            maxOffset = consumeQueue.getMaxOffsetInQueue();

            // 如果 maxOffset 为零，则表示没有可用消息
            if (maxOffset == 0) {
                status = GetMessageStatus.NO_MESSAGE_IN_QUEUE;
                nextBeginOffset = 0;
            } else if (offset < minOffset) {
                status = GetMessageStatus.OFFSET_TOO_SMALL;
                nextBeginOffset = minOffset;
            } else if (offset == maxOffset) {
                status = GetMessageStatus.OFFSET_OVERFLOW_ONE;
                nextBeginOffset = offset;
            } else if (offset > maxOffset) {
                status = GetMessageStatus.OFFSET_OVERFLOW_BADLY;
                if (0 == minOffset) {
                    nextBeginOffset = minOffset;
                } else {
                    nextBeginOffset = maxOffset;
                }
            } else {

                // 根据偏移量获取当前 ConsumeQueue 的缓存
                SelectMappedBufferResult bufferConsumeQueue = consumeQueue.getIndexBuffer(offset);

                if (bufferConsumeQueue != null) {
                    try {
                        status = GetMessageStatus.NO_MATCHED_MESSAGE;

                        long nextPhyFileStartOffset = Long.MIN_VALUE;
                        long maxPhyOffsetPulling = 0;

                        int i = 0;

                        // 设置每次获取的最大消息数
                        final int maxFilterMessageCount = Math.max(16000, maxMsgNums * ConsumeQueue.CQ_STORE_UNIT_SIZE);

                        // 遍历所有的 Consume Queue 中的消息指针
                        for (; i < bufferConsumeQueue.getSize() && i < maxFilterMessageCount; i += ConsumeQueue.CQ_STORE_UNIT_SIZE) {
                            long offsetPy = bufferConsumeQueue.getByteBuffer().getLong();
                            int sizePy = bufferConsumeQueue.getByteBuffer().getInt();

                            maxPhyOffsetPulling = offsetPy;

                            if (nextPhyFileStartOffset != Long.MIN_VALUE) {
                                if (offsetPy < nextPhyFileStartOffset)
                                    continue;
                            }

                            boolean isInDisk = checkInDiskByCommitOffset(offsetPy, maxOffsetPy);

                            if (isTheBatchFull(sizePy, maxMsgNums, getResult.getBufferTotalSize(), getResult.getMessageCount(),
                                    isInDisk)) {
                                break;
                            }

                            // 从 MessageStore 中获取消息
                            SelectMappedBufferResult selectResult = this.messageStore.getMessage(offsetPy, sizePy);

                            // 如果没有获取到数据，则切换到下一个文件继续
                            if (null == selectResult) {
                                if (getResult.getBufferTotalSize() == 0) {
                                    status = GetMessageStatus.MESSAGE_WAS_REMOVING;
                                }

                                nextPhyFileStartOffset = this.messageStore.rollNextFile(offsetPy);
                                continue;
                            }

                            // 如果获取到了，则返回结果
                            getResult.addMessage(selectResult);
                            status = GetMessageStatus.FOUND;
                            nextPhyFileStartOffset = Long.MIN_VALUE;
                        }

                        nextBeginOffset = offset + (i / ConsumeQueue.CQ_STORE_UNIT_SIZE);

                        long diff = maxOffsetPy - maxPhyOffsetPulling;

                        // 获取当前内存情况
                        long memory = (long) (getTotalPhysicalMemorySize()
                                * (LocalMessageQueueConfig.accessMessageInMemoryMaxRatio / 100.0));

                        getResult.setSuggestPullingFromSlave(diff > memory);

                    } finally {

                        bufferConsumeQueue.release();
                    }
                } else {
                    status = GetMessageStatus.OFFSET_FOUND_NULL;
                    nextBeginOffset = consumeQueue.rollNextFile(offset);
                    log.warning("consumer request topic: " + topic + "offset: " + offset + " minOffset: " + minOffset + " maxOffset: "
                            + maxOffset + ", but access logic queue failed.");
                }
            }
        } else {
            status = GetMessageStatus.NO_MATCHED_LOGIC_QUEUE;
            nextBeginOffset = 0;
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
     * Description 添加定时任务
     */
    private void addScheduleTask() {

        // 定期清理 ConsumeQueue
        this.scheduledExecutorService.scheduleAtFixedRate(() -> {
            this.cleanConsumeQueueService.run();
        }, 1000 * 60, LocalMessageQueueConfig.cleanResourceInterval, TimeUnit.MILLISECONDS);

    }

    /**
     * Description 恢复之前的 ConsumeQueue 数据
     *
     * @param lastExitOK
     */
    private void recover(final boolean lastExitOK) {

        // 恢复所有的 ConsumeQueue
        for (ConcurrentHashMap<Integer, ConsumeQueue> maps : this.consumeQueueTable.values()) {
            for (ConsumeQueue logic : maps.values()) {
                logic.recover();
            }
        }

        // 根据上一次是否正常退出来判断是否按照正常恢复还是异常恢复
        if (lastExitOK) {
            MessageStoreRecover.recoverNormally(this.messageStore);
        } else {
            MessageStoreRecover.recoverAbnormally(this.messageStore);
        }

        // 恢复 TopicQueueTable
        this.recoverTopicQueueTable();
    }

    /**
     * Description 恢复 TopicQueueTable
     */
    private void recoverTopicQueueTable() {

        // 临时的主题队列编号与偏移
        HashMap<String/* topic-queueid */, Long/* offset */> table = new HashMap<String, Long>(1024);
        long minPhyOffset = this.messageStore.getMinOffset();
        for (ConcurrentHashMap<Integer, ConsumeQueue> maps : this.consumeQueueTable.values()) {
            for (ConsumeQueue logic : maps.values()) {
                String key = logic.getTopic() + "-" + logic.getQueueId();
                table.put(key, logic.getMaxOffsetInQueue());
                logic.correctMinOffset(minPhyOffset);
            }
        }

        this.messageStore.setTopicQueueTable(table);
    }


    /**
     * Description 加载 ConsumeQueue 文件
     *
     * @return
     */
    private boolean loadConsumeQueue() {
        File dirLogic = new File(((LocalMessageQueueConfig) this.messageQueueConfig).getStorePathConsumeQueue());

        // 列举所有文件
        File[] fileTopicList = dirLogic.listFiles();

        // 如果非空
        if (fileTopicList != null) {

            for (File fileTopic : fileTopicList) {
                String topic = fileTopic.getName();

                // 从文件名中获取到 QueueId
                File[] fileQueueIdList = fileTopic.listFiles();

                if (fileQueueIdList != null) {

                    for (File fileQueueId : fileQueueIdList) {
                        int queueId;
                        try {
                            queueId = Integer.parseInt(fileQueueId.getName());
                        } catch (NumberFormatException e) {
                            continue;
                        }

                        ConsumeQueue logic = new ConsumeQueue(
                                topic,
                                queueId,
                                LocalMessageQueueConfig.getMapedFileSizeConsumeQueue(),
                                this);

                        this.putConsumeQueue(topic, queueId, logic);

                        // 调用 ConsumeQueue 中的加载函数
                        if (!logic.load()) {
                            return false;
                        }
                    }
                }
            }
        }

        log.info("load logics queue all over, OK");

        return true;
    }

    public AllocateMappedPartitionService getAllocateMappedPartitionService() {
        return allocateMappedPartitionService;
    }

    /**
     * Description 判断当前系统页缓存是否繁忙
     *
     * @return
     */
    public boolean isOSPageCacheBusy() {

        // 记录当前写入锁的时间，如果写入锁超过设置时间尚未释放，则认为繁忙
        long begin = this.messageStore.getBeginTimeInLock();
        long diff = now() - begin;

        if (diff < 10000000 //
                && diff > LocalMessageQueueConfig.osPageCacheBusyTimeOutMills) {
            return true;
        }

        return false;
    }

    /**
     * Description 根据主题与 QueueId 查找 ConsumeQueue，如果不存在则创建
     *
     * @param topic
     * @param queueId
     * @return
     */
    public ConsumeQueue findConsumeQueue(String topic, int queueId) {
        ConcurrentHashMap<Integer, ConsumeQueue> map = consumeQueueTable.get(topic);

        // 判断是否存在该主题对应的 queue，不存在则创建
        if (null == map) {
            ConcurrentHashMap<Integer, ConsumeQueue> newMap = new ConcurrentHashMap<Integer, ConsumeQueue>(128);
            ConcurrentHashMap<Integer, ConsumeQueue> oldMap = consumeQueueTable.putIfAbsent(topic, newMap);
            if (oldMap != null) {
                map = oldMap;
            } else {
                map = newMap;
            }
        }

        // 判断该主题下是否存在 queueId，不存在则创建
        ConsumeQueue logic = map.get(queueId);

        // 如果获取为空，则创建新的 ConsumeQueue
        if (null == logic) {

            ConsumeQueue newLogic = new ConsumeQueue(//
                    topic, // 主题
                    queueId, // queueId
                    LocalMessageQueueConfig.mapedFileSizeConsumeQueue, // 映射文件尺寸
                    this);


            ConsumeQueue oldLogic = map.putIfAbsent(queueId, newLogic);

            // 再做一次判断，避免误创
            if (oldLogic != null) {
                logic = oldLogic;
            } else {

                // 初始化当前 ConsumeQueue 对应的下标
                // 注意，初始化下标值为 -1
                this.consumerScheduler.updateOffset(topic, queueId, -1);
                logic = newLogic;
            }
        }

        return logic;
    }

    /**
     * Description 将 Topic、QueueId 以及生成的 ConsumeQueue 添加到列表中
     *
     * @param topic
     * @param queueId
     * @param consumeQueue
     */
    private void putConsumeQueue(final String topic, final int queueId, final ConsumeQueue consumeQueue) {
        ConcurrentHashMap<Integer/* queueId */, ConsumeQueue> map = this.consumeQueueTable.get(topic);
        if (null == map) {
            map = new ConcurrentHashMap<Integer/* queueId */, ConsumeQueue>();
            map.put(queueId, consumeQueue);
            this.consumeQueueTable.put(topic, map);
        } else {
            map.put(queueId, consumeQueue);
        }
    }

    public MessageStore getMessageStore() {
        return messageStore;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public void setShutdown(boolean shutdown) {
        this.shutdown = shutdown;
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<Integer, ConsumeQueue>> getConsumeQueueTable() {
        return consumeQueueTable;
    }

    public void setConsumeQueueTable(ConcurrentHashMap<String, ConcurrentHashMap<Integer, ConsumeQueue>> consumeQueueTable) {
        this.consumeQueueTable = consumeQueueTable;
    }

    /**
     * Description 获取最大的物理位移
     *
     * @return
     */
    public long getMaxPhyOffset() {
        return this.messageStore.getMaxOffset();
    }

    @Override
    public long getConfirmOffset() {
        return this.messageStore.getConfirmOffset();
    }

    public void setConfirmOffset(long phyOffset) {
        this.messageStore.setConfirmOffset(phyOffset);
    }

    /**
     * Description 清除过期的 ConsumeQueue 文件
     *
     * @param phyOffset
     */
    public void truncateDirtyLogicFiles(long phyOffset) {
        ConcurrentHashMap<String, ConcurrentHashMap<Integer, ConsumeQueue>> tables = LocalMessageQueue.this.consumeQueueTable;

        for (ConcurrentHashMap<Integer, ConsumeQueue> maps : tables.values()) {
            for (ConsumeQueue logic : maps.values()) {
                logic.truncateDirtyLogicFiles(phyOffset);
            }
        }
    }

    public void destroyLogics() {
        for (ConcurrentHashMap<Integer, ConsumeQueue> maps : this.consumeQueueTable.values()) {
            for (ConsumeQueue logic : maps.values()) {
                logic.destroy();
            }
        }
    }

    public ConsumerScheduler getConsumerScheduler() {
        return consumerScheduler;
    }

    public void shutdown() {


        long startTime = now();

        // 自旋锁，等 PostPutMessageService 处理完毕之后执行
        while (this.postPutMessageService.getReputFromOffset() < this.getMaxPhyOffset()) {

        }

        System.out.println("等待 PostPutMessageService :" + (now() - startTime) + "；读取耗时：" + PostPutMessageService.readSpendTime.get() + "放置耗时：" + PostPutMessageService.putSpendTime.get());

        // 判断 PostPutMessageService 是否执行完毕
        if (!this.shutdown) {

            this.shutdown = true;

            // 停止定时服务
            this.scheduledExecutorService.shutdown();

            // 关闭文件分配
            this.allocateMappedPartitionService.shutdown(true);

            // 关闭消息重放
            this.postPutMessageService.shutdown();

            // 关闭 Flush
            this.flushConsumeQueueService.shutdown();

            // 关闭 ConsumerScheduler
            this.consumerScheduler.shutdown();

            // 关闭 MessageStore
            this.messageStore.shutdown();

            // 强行调用 GC 释放全部内存
            System.gc();

            log.info("LocalMessageQueue Shutdown!");

            System.exit(0);

        }
    }

    public MessageQueueConfig getMessageQueueConfig() {
        return messageQueueConfig;
    }

    public void setLocalMessageQueueConfig(LocalMessageQueueConfig localMessageQueueConfig) {
        this.messageQueueConfig = localMessageQueueConfig;
    }


}



