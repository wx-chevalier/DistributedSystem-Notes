package wx.mq.local.message.service;


import wx.mq.common.message.status.SelectMappedBufferResult;
import wx.mq.local.LocalMessageQueue;
import wx.mq.util.sys.ControlledService;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static wx.mq.local.message.serialization.LocalMessageDecoder.checkMessageAndReturnSize;
import static wx.mq.local.LocalMessageQueueConfig.duplicationEnable;
import static wx.mq.util.ds.DateTimeUtil.now;

/**
 * Description 在消息写入到 MessageStore 之后，将消息放置到 ConsumeQueue 持久化文件中，未来会更名为 BuildConsumeQueueService
 */
public class PostPutMessageService extends ControlledService {

    public static AtomicLong readSpendTime = new AtomicLong(0);

    public static AtomicLong putSpendTime = new AtomicLong(0);

    // 日志记录
    public final static Logger log = Logger.getLogger(LocalMessageQueue.class.getName());

    // 当前处理到的 MessageStore 偏移位置
    private volatile long reputFromOffset = 0;

    // 消息存储的指针
    LocalMessageQueue messageStore;

    /**
     * Description 默认构造函数
     *
     * @param messageStore
     */
    public PostPutMessageService(LocalMessageQueue messageStore) {
        this.messageStore = messageStore;
    }

    @Override
    public void shutdown() {

        if (this.isCommitLogAvailable()) {
            log.warning(String.format("shutdown PostPutMessageService, but commitlog have not finish to be dispatched, CL: %s reputFromOffset: %s",
                    this.messageStore.getMessageStore().getMaxOffset(), this.reputFromOffset));
        }

        super.shutdown();
    }

    public long behind() {
        return this.messageStore.getMessageStore().getMaxOffset() - this.reputFromOffset;
    }

    private boolean isCommitLogAvailable() {
        return this.reputFromOffset < this.messageStore.getMessageStore().getMaxOffset();
    }

    /**
     * Description 执行消息后操作
     */
    private void doReput() {

        for (boolean doNext = true; this.isCommitLogAvailable() && doNext; ) {

            if (duplicationEnable //
                    && this.reputFromOffset >= this.messageStore.getConfirmOffset()) {
                break;
            }

            long startTime = now();

            // 读取当前的消息
            SelectMappedBufferResult result = this.messageStore.getMessageStore().getData(reputFromOffset);


            // 如果消息不存在，则停止当前操作
            if (result == null) {
                doNext = false;
                continue;
            }
            try {

                // 获取当前消息的起始位置
                this.reputFromOffset = result.getStartOffset();

                // 顺序读取所有消息
                for (int readSize = 0; readSize < result.getSize() && doNext; ) {

                    // 读取当前位置的消息
                    PostPutMessageRequest postPutMessageRequest =
                            checkMessageAndReturnSize(result.getByteBuffer());

                    int size = postPutMessageRequest.getMsgSize();

                    readSpendTime.addAndGet(now() - startTime);

                    startTime = now();
                    // 如果处理成功
                    if (postPutMessageRequest.isSuccess()) {
                        if (size > 0) {

                            // 执行消息写入到 ConsumeQueue 的操作
                            this.messageStore.putMessagePositionInfo(postPutMessageRequest);

                            // 修正当前读取的位置
                            this.reputFromOffset += size;
                            readSize += size;

                        } else if (size == 0) {
                            this.reputFromOffset = this.messageStore.getMessageStore().rollNextFile(this.reputFromOffset);
                            readSize = result.getSize();
                        }

                        putSpendTime.addAndGet(now() - startTime);

                    } else if (!postPutMessageRequest.isSuccess()) {

                        if (size > 0) {
                            log.warning("[BUG]read total count not equals msg total size. reputFromOffset={}" + reputFromOffset);
                            this.reputFromOffset += size;
                        } else {
                            doNext = false;

                            log.warning("[BUG]the master dispatch message to consume queue error, COMMITLOG OFFSET: " +
                                    this.reputFromOffset);

                            this.reputFromOffset += result.getSize() - readSize;
                        }
                    }
                }

            } finally {
                result.release();
            }

        }
    }

    @Override
    public void run() {
        LocalMessageQueue.log.info(this.getServiceName() + " service started");

        while (!this.isStopped()) {
            try {
                Thread.sleep(1);
                this.doReput();
            } catch (Exception e) {
                e.printStackTrace();
                LocalMessageQueue.log.warning(this.getServiceName() + " service has exception. ");
            }
        }

        LocalMessageQueue.log.info(this.getServiceName() + " service end");
    }

    @Override
    public String getServiceName() {
        return PostPutMessageService.class.getSimpleName();
    }

    public long getReputFromOffset() {
        return reputFromOffset;
    }

    public void setReputFromOffset(long reputFromOffset) {
        this.reputFromOffset = reputFromOffset;
    }

}