package wx.mq.local.consume.service;

import wx.mq.local.LocalMessageQueue;
import wx.mq.local.consume.ConsumeQueue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static wx.mq.local.LocalMessageQueueConfig.deleteConsumeQueueFilesInterval;

public class CleanConsumeQueueService {

    // 日志句柄
    private final static Logger log = Logger.getLogger(LocalMessageQueue.class.getName());

    // 当前处理到的消息的位移
    private long lastPhysicalMinOffset = 0;

    private final LocalMessageQueue messageStore;

    /**
     * Description 默认构造函数
     *
     * @param localMessageStore
     */
    public CleanConsumeQueueService(LocalMessageQueue localMessageStore) {
        this.messageStore = localMessageStore;
    }

    /**
     * Description 消息运行函数
     */
    public void run() {
        try {
            this.deleteExpiredFiles();
        } catch (Exception e) {
            log.warning(this.getServiceName() + " service has exception. ");
        }
    }

    /**
     * Description 删除当前队列中过期的文件
     */
    private void deleteExpiredFiles() {

        // 获取当前 MessageStore 最小的 minOffset
        long minOffset = this.messageStore.getMessageStore().getMinOffset();

        // 如果当前 MessageStore 的最小偏移值已经大于目前记录的处理偏移值
        if (minOffset > this.lastPhysicalMinOffset) {
            this.lastPhysicalMinOffset = minOffset;

            // 获取当前所有的 ConsumeQueue 队列
            ConcurrentHashMap<String, ConcurrentHashMap<Integer, ConsumeQueue>> tables = this.messageStore.getConsumeQueueTable();

            // 遍历队列中的所有值
            for (ConcurrentHashMap<Integer, ConsumeQueue> maps : tables.values()) {
                for (ConsumeQueue logic : maps.values()) {

                    // 根据目前最小偏移删除无用文件
                    int deleteCount = logic.deleteExpiredFile(minOffset);

                    if (deleteCount > 0 && deleteConsumeQueueFilesInterval > 0) {
                        try {
                            Thread.sleep(deleteConsumeQueueFilesInterval);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            }
        }
    }

    public String getServiceName() {
        return CleanConsumeQueueService.class.getSimpleName();
    }
}