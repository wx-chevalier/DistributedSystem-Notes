package wx.mq.common.client.config;

import wx.mq.MessageQueue;
import wx.mq.local.consume.ConsumeQueue;
import wx.mq.util.sys.ThreadFactoryImpl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static wx.mq.local.LocalMessageQueueConfig.flushConsumerOffsetInterval;

/**
 * Description 消费者端调度器
 */
public class ConsumerScheduler {

    // 日志记录
    public final static Logger log = Logger.getLogger(ConsumeQueue.class.getName());

    // 统一定期执行调度服务
    private final ScheduledExecutorService scheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("LocalStoreScheduledThread"));

    // 当前 ConsumerOffsetManager
    private final ConsumerOffsetManager consumerOffsetManager;

    private final MessageQueue messageQueue;

    public ConsumerScheduler(MessageQueue messageQueue) {

        this.messageQueue = messageQueue;

        this.consumerOffsetManager = new ConsumerOffsetManager(messageQueue);
    }

    /**
     * Description 加载调度器的数据
     */
    public void load() {

        // 启动 ConsumerOffsetManager
        consumerOffsetManager.load();

    }

    /**
     * Description 启动 Scheduler 中的定时任务
     */
    public void start() {

        log.info("ConsumerScheduler 启动");

        // 定时执行 ConsumerScheduler 的持久化操作
        this.scheduledExecutorService.scheduleAtFixedRate(() -> {

            try {
                consumerOffsetManager.persist();
            } catch (Throwable e) {
                log.warning("【Error】schedule persist consumerOffset error.");
                e.printStackTrace();
            }
        }, 10, flushConsumerOffsetInterval, TimeUnit.MILLISECONDS);


    }

    /**
     * Description 允许 Consumer 根据主题以及队列查看可用的下标地址
     *
     * @param topic
     * @param queueId
     * @return
     */
    public synchronized long queryOffsetAndLock(final String clientHostAndPort, final String topic, final int queueId) {

        return consumerOffsetManager.queryOffsetAndLock(clientHostAndPort, topic, queueId);

    }

    /**
     * Description 更新某个 Consumer 对应的 Offset
     *
     * @param clientHostAndPort
     * @param topic
     * @param queueId
     * @param offset
     */
    public void updateOffset(final String clientHostAndPort, final String topic, final int queueId, final long offset) {

        this.consumerOffsetManager.commitOffset(clientHostAndPort, topic, queueId, offset);

    }

    public void updateOffset(final String topic, final int queueId, final long offset) {

        this.consumerOffsetManager.commitOffset("Broker Inner", topic, queueId, offset);

    }

    public void shutdown() {

        this.consumerOffsetManager.persist();

        this.scheduledExecutorService.shutdown();
    }


}
