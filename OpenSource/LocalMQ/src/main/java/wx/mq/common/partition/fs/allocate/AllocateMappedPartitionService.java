package wx.mq.common.partition.fs.allocate;

import wx.mq.MessageQueue;
import wx.mq.common.partition.fs.MappedPartition;
import wx.mq.util.sys.ControlledService;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static wx.mq.local.LocalMessageQueueConfig.isWarmMappedFileEnable;
import static wx.mq.local.LocalMessageQueueConfig.mapedFileSizeCommitLog;

/**
 * Description 提前创建内存映射文件
 */
public class AllocateMappedPartitionService extends ControlledService {

    // 日志工具
    private final static Logger log = Logger.getLogger(MappedPartition.class.getName());

    // 默认等待时间
    private static int waitTimeOut = 1000 * 5;

    // 请求表
    private ConcurrentHashMap<String, AllocateRequest> requestTable =
            new ConcurrentHashMap<String, AllocateRequest>();

    // 请求队列
    private PriorityBlockingQueue<AllocateRequest> requestQueue =
            new PriorityBlockingQueue<AllocateRequest>();

    // 是否存在异常
    private volatile boolean hasException = false;

    private final MessageQueue messageQueue;

    /**
     * Description 构造函数
     *
     * @param messageQueue
     */
    public AllocateMappedPartitionService(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    @Override
    public String getServiceName() {
        return AllocateMappedPartitionService.class.getSimpleName();
    }

    @Override
    public void run() {

        log.info(this.getServiceName() + " service started");

        // 循环执行文件分配请求
        while (!this.isStopped() && this.mmapOperation()) {

        }
        log.info(this.getServiceName() + " service end");

    }

    /**
     * Description 发起请求并且创建新的内存映射文件
     *
     * @param nextFilePath
     * @param nextNextFilePath
     * @param fileSize
     * @return
     */
    public MappedPartition putRequestAndReturnMappedFile(String nextFilePath, String nextNextFilePath, int fileSize) {

        // 构建分配请求
        AllocateRequest nextReq = new AllocateRequest(nextFilePath, fileSize);

        // 判断是否存在重复的请求路径
        boolean nextPutOK = this.requestTable.putIfAbsent(nextFilePath, nextReq) == null;

        // 如果允许执行请求，则添加到执行队列中
        if (nextPutOK) {

            boolean offerOK = this.requestQueue.offer(nextReq);

            // 罕见异常处理
            if (!offerOK) {
                log.warning("never expected here, add a request to preallocate queue failed");
            }
        }

        // 为下下个文件构造创建请求
        AllocateRequest nextNextReq = new AllocateRequest(nextNextFilePath, fileSize);

        boolean nextNextPutOK = this.requestTable.putIfAbsent(nextNextFilePath, nextNextReq) == null;

        if (nextNextPutOK) {
            boolean offerOK = this.requestQueue.offer(nextNextReq);
            if (!offerOK) {
                log.warning("never expected here, add a request to preallocate queue failed");
            }
        }

        // 如果已经存在异常，则放弃创建
        if (hasException) {
            log.warning(this.getServiceName() + " service has exception. so return null");
            return null;
        }

        // 获取到下一个执行请求并且立刻执行
        // 注意，下下个文件会被提前创建
        AllocateRequest result = this.requestTable.get(nextFilePath);
        try {
            if (result != null) {

                // 阻塞直至文件创建完毕
                boolean waitOK = result.getCountDownLatch().await(waitTimeOut, TimeUnit.MILLISECONDS);

                // 如果创建失败则返回空
                if (!waitOK) {

                    log.warning("create mmap timeout " + result.getFilePath() + " " + result.getFileSize());

                    return null;
                } else {
                    this.requestTable.remove(nextFilePath);

                    // 否则返回刚才创建成功的文件
                    return result.getMappedPartition();
                }
            } else {
                log.warning("find preallocate mmap failed, this never happen");
            }
        } catch (InterruptedException e) {
            log.warning(this.getServiceName() + " service has exception. ");
        }

        return null;

    }

    /**
     * Description 循环执行映射文件预分配
     *
     * @Exception Only interrupted by the external thread, will return false
     */
    private boolean mmapOperation() {

        // 标识是否成功
        boolean isSuccess = false;

        // 存放当前分配请求
        AllocateRequest req = null;

        // 执行操作
        try {

            // 取出最新的执行对象
            req = this.requestQueue.take();

            // 取得待执行对象在请求表中的实例
            AllocateRequest expectedRequest = this.requestTable.get(req.getFilePath());

            // 如果不存在该请求体，说明该次创建请求已经超时，由主线程执行了创建
            if (null == expectedRequest) {
                log.warning("this mmap request expired, maybe cause timeout " + req.getFilePath() + " "
                        + req.getFileSize());
                return true;
            }

            // 如果创建对象发生了变化，则本次也放弃执行
            if (expectedRequest != req) {
                log.warning("never expected here,  maybe cause timeout " + req.getFilePath() + " "
                        + req.getFileSize() + ", req:" + req + ", expectedRequest:" + expectedRequest);
                return true;
            }

            // 判断是否已经存在创建好的对象
            if (req.getMappedPartition() == null) {

                // 记录起始创建时间
                long beginTime = System.currentTimeMillis();

                // 构建内存映射文件对象
                MappedPartition mappedPartition = new MappedPartition(req.getFilePath(), req.getFileSize());

                // 记录间隔时间
                long eclipseTime = System.currentTimeMillis() - beginTime;

                // 如果间隔时间已经超过 10ms
                if (eclipseTime > 10) {

                    // 则记录下当前的创建过长警告
                    int queueSize = this.requestQueue.size();
                    log.warning("create mappedPartition spent time(ms) " + eclipseTime + " queue size " + queueSize
                            + " " + req.getFilePath() + " " + req.getFileSize());
                }

                // 进行文件预热，仅预热 MessageStore
                if (mappedPartition.getFileSize() >= mapedFileSizeCommitLog && isWarmMappedFileEnable) {
                    mappedPartition.warmMappedFile();
                }

                // 将创建好的对象回写到请求中
                req.setMappedPartition(mappedPartition);

                // 异常设置为 false
                this.hasException = false;

                // 成功设置为 true
                isSuccess = true;
            }
        } catch (InterruptedException e) {
            log.warning(this.getServiceName() + " interrupted, possibly by shutdown.");
            this.hasException = true;
            return false;
        } catch (IOException e) {
            log.warning(this.getServiceName() + " service has exception. ");
            this.hasException = true;
            if (req != null) {
                requestQueue.offer(req);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {
                }
            }
        } finally {
            if (req != null && isSuccess)
                req.getCountDownLatch().countDown();
        }
        return true;
    }


}
