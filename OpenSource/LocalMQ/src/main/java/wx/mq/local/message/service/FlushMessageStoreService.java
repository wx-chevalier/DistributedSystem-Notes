package wx.mq.local.message.service;

import wx.mq.local.LocalMessageQueueConfig;
import wx.mq.local.message.store.MessageStore;
import wx.mq.util.sys.ControlledService;

import java.util.logging.Logger;

/**
 * Description 定期实现 Flush 操作
 */
public class FlushMessageStoreService extends ControlledService {

    private final static Logger log = Logger.getLogger(FlushMessageStoreService.class.getName());

    private long lastFlushTimestamp = 0;

    private long printTimes = 0;

    private final int RETRY_TIMES_OVER = 10;

    private final MessageStore messageStore;

    /**
     * Description 默认构造函数
     */
    public FlushMessageStoreService(MessageStore messageStore) {
        super();
        this.messageStore = messageStore;
    }

    public void run() {
        log.info(this.getServiceName() + " service started");

        while (!this.isStopped()) {

            // 执行间隔
            int interval = LocalMessageQueueConfig.flushIntervalCommitLog;

            // 执行的最小页面数
            int flushPhysicQueueLeastPages = LocalMessageQueueConfig.flushCommitLogLeastPages;

            int flushPhysicQueueThoroughInterval = LocalMessageQueueConfig.flushCommitLogThoroughInterval;

            boolean printFlushProgress = LocalMessageQueueConfig.printFlushProgress;

            // 打印 Flush 进程
            long currentTimeMillis = System.currentTimeMillis();

            if (currentTimeMillis >= (this.lastFlushTimestamp + flushPhysicQueueThoroughInterval)) {
                this.lastFlushTimestamp = currentTimeMillis;
                flushPhysicQueueLeastPages = 0;
                printFlushProgress = (printTimes++ % 10) == 0;
            }

            try {

                // 定时进行 Flush 操作
                this.waitForRunning(interval);

                if (printFlushProgress) {
                    this.printFlushProgress();
                }

                long begin = System.currentTimeMillis();

                // 将 MappedPartitionQueue 写入到文件系统中
                this.messageStore.getMappedPartitionQueue().flush(flushPhysicQueueLeastPages);

                long past = System.currentTimeMillis() - begin;
                if (past > 500) {
                    log.info("Flush data to disk costs " + past + " ms");
                }
            } catch (Throwable e) {
                log.warning(this.getServiceName() + " service has exception. ");
                this.printFlushProgress();
            }
        }

        // Normal shutdown, to ensure that all the flush before exit
        boolean result = false;
        for (int i = 0; i < RETRY_TIMES_OVER && !result; i++) {
            result = this.messageStore.getMappedPartitionQueue().flush(0);
            log.info(this.getServiceName() + " service shutdown, retry " + (i + 1) + " times " + (result ? "OK" : "Not OK"));
        }

        this.printFlushProgress();

        log.info(this.getServiceName() + " service end");
    }

    @Override
    public String getServiceName() {
        return FlushMessageStoreService.class.getSimpleName();
    }

    private void printFlushProgress() {
        // MessageStore.log.info("how much disk fall behind memory, "
        // + MessageStore.this.mappedFileQueue.howMuchFallBehind());
    }

    @Override
    public long getJointime() {
        return 1000 * 60 * 5;
    }
}