package wx.mq.local.consume.service;

import wx.mq.local.LocalMessageQueue;
import wx.mq.local.LocalMessageQueueConfig;
import wx.mq.local.consume.ConsumeQueue;
import wx.mq.util.sys.ControlledService;

import java.util.concurrent.ConcurrentHashMap;

import static wx.mq.local.LocalMessageQueueConfig.flushConsumeQueueInterval;
import static wx.mq.util.ds.DateTimeUtil.now;

/**
 * Description 默认将 ConsumeQueue 写入到磁盘的服务
 */
public class FlushConsumeQueueService extends ControlledService {

    private static final int RETRY_TIMES_OVER = 3;

    private long lastFlushTimestamp = 0;

    // 默认存储目录
    private final String storePath;

    // 指向消息核心存储
    LocalMessageQueue messageStore;

    /**
     * Description 默认构造函数
     *
     * @param localMessageStore
     */
    public FlushConsumeQueueService(LocalMessageQueue localMessageStore) {
        this.messageStore = localMessageStore;
        this.storePath = ((LocalMessageQueueConfig) localMessageStore.getMessageQueueConfig()).getStorePathCommitLog();
    }

    /**
     * Description 实际进行将消息索引写入到磁盘操作
     *
     * @param retryTimes
     */
    private void doFlush(int retryTimes) {

        // 取值默认的写入到磁盘数目
        int flushConsumeQueueLeastPages = LocalMessageQueueConfig.flushConsumeQueueLeastPages;

        // 当重试数目完毕后，强行 Flush
        if (retryTimes == RETRY_TIMES_OVER) {
            flushConsumeQueueLeastPages = 0;
        }

        int flushConsumeQueueThoroughInterval = LocalMessageQueueConfig.flushConsumeQueueThoroughInterval;

        long currentTimeMillis = now();

        // 如果已经超过了时间间隔，则也强行写入
        if (currentTimeMillis >= (this.lastFlushTimestamp + flushConsumeQueueThoroughInterval)) {
            this.lastFlushTimestamp = currentTimeMillis;
            flushConsumeQueueLeastPages = 0;
        }

        ConcurrentHashMap<String, ConcurrentHashMap<Integer, ConsumeQueue>> tables = this.messageStore.getConsumeQueueTable();

        // 遍历目前所有的 ConsumeQueue 并且执行写入
        for (ConcurrentHashMap<Integer, ConsumeQueue> maps : tables.values()) {
            for (ConsumeQueue cq : maps.values()) {
                boolean result = false;
                for (int i = 0; i < retryTimes && !result; i++) {
                    result = cq.flush(flushConsumeQueueLeastPages);
                }
            }
        }
    }

    /**
     * Description 启动时执行
     */
    public void run() {
        LocalMessageQueue.log.info(this.getServiceName() + " service started");

        while (!this.isStopped()) {
            try {
                this.waitForRunning(flushConsumeQueueInterval);
                this.doFlush(1);
            } catch (Exception e) {
                LocalMessageQueue.log.warning(this.getServiceName() + " service has exception. ");
            }
        }

        // 如果出现异常则进行重试
        this.doFlush(RETRY_TIMES_OVER);

        LocalMessageQueue.log.info(this.getServiceName() + " service end");
    }

    @Override
    public String getServiceName() {
        return FlushConsumeQueueService.class.getSimpleName();
    }

    @Override
    public long getJointime() {
        return 1000 * 60;
    }
}