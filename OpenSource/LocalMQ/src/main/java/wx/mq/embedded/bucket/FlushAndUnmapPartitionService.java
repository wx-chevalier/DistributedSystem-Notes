package wx.mq.embedded.bucket;

import wx.mq.embedded.EmbeddedMessageQueue;
import wx.mq.common.partition.fs.MappedPartition;
import wx.mq.embedded.client.EmbeddedProducer;
import wx.mq.util.sys.ControlledService;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import static wx.mq.util.ds.DateTimeUtil.now;

/**
 * Description 进行定期的 Flush 与 Unmap 操作
 */
public class FlushAndUnmapPartitionService extends ControlledService {

    private final ConcurrentLinkedQueue<MappedPartition> mappedPartitions = new ConcurrentLinkedQueue<>();

    private final EmbeddedMessageQueue messageQueue;

    private final static Logger log = Logger.getLogger(FlushAndUnmapPartitionService.class.getName());

    public FlushAndUnmapPartitionService(EmbeddedMessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    public void putPartition(MappedPartition mappedPartition) {
        mappedPartitions.add(mappedPartition);
    }

    @Override
    public String getServiceName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void run() {

        while (!this.isStopped()) {

            int interval = 100;

            try {

                if (this.mappedPartitions.size() > 0) {

                    long startTime = now();

                    // 取出待处理的 MappedPartition
                    MappedPartition mappedPartition = this.mappedPartitions.poll();

                    // 将当前内容写入到磁盘
                    mappedPartition.flush(0);

                    // 释放当前不需要使用的空间
                    mappedPartition.cleanup();

                    long past = now() - startTime;

//                    EmbeddedProducer.flushEclipseTime.addAndGet(past);

                    if (past > 500) {
                        log.info("Flush data to disk and unmap MappedPartition costs " + past + " ms:" + mappedPartition.getFileName());
                    }
                } else {
                    // 定时进行 Flush 操作
                    this.waitForRunning(interval);
                }


            } catch (Throwable e) {
                log.warning(this.getServiceName() + " service has exception. ");
            }

        }

    }
}
