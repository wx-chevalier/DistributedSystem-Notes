package wx.mq.local;

import wx.mq.MessageQueueConfig;
import wx.mq.local.consume.ConsumeQueue;

import java.io.File;

/**
 * Description 消息中心配置
 */
public class LocalMessageQueueConfig extends MessageQueueConfig {

    public LocalMessageQueueConfig(String storePathRootDir) {
        super(storePathRootDir);
    }

    // 设置 MessageStore 地址，保存所有的消息信息
    @Important
    public String getStorePathCommitLog() {
        return this.storePathRootDir + File.separator + "commitlog";
    }

    // 设置在消息创建阶段随机生成的 ConsumeQueue 地址
    @Important
    public String getStorePathConsumeQueue() {
        return this.storePathRootDir + File.separator + "consumequeue";
    }

    // 默认的读取队列数目
    public static int defaultReadQueueNums = 12;

    // 默认的写入队列数目
    public static int defaultWriteQueueNums = 12;


    /******************************************************
     *                 开始 系统相关 参数                   *
     ******************************************************/

    public static int accessMessageInMemoryMaxRatio = 40;

    public static int maxTransferBytesOnMessageInMemory = 300 * 1024;

    public static int maxTransferCountOnMessageInMemory = 32;

    public static int maxTransferBytesOnMessageInDisk = 64 * 1024;

    public static int maxTransferCountOnMessageInDisk = 8;

    // 文件删除的间隔
    public static int cleanResourceInterval = 10000;

    // 是否允许强制删除
    @Important
    public static boolean cleanFileForciblyEnable = true;

    public static boolean isWarmMappedFileEnable = false;

    /******************************************************
     *                 结束 系统相关 参数                   *
     ******************************************************/

    /******************************************************
     *               开始 MessageStore 参数                   *
     ******************************************************/

    // 设置 MessageStore 单文件大小，默认为 1G
    @Important
    public static int mapedFileSizeCommitLog = 1024 * 1024 * 1024;



    // 设置 Flush 的页数下限
    @Important
    public static int flushCommitLogLeastPages = 2;

    // 设置 Flush 全部强制存储间隔
    public static int flushCommitLogThoroughInterval = 15 * 1000;

    // 设置定时 Flush 的频次 / 毫秒
    public static int flushIntervalCommitLog = 500;

    public static boolean useReentrantLockWhenPutMessage = true;

    /******************************************************
     *               结束 MessageStore 参数                   *
     ******************************************************/

    /******************************************************
     *               开始 Producer 参数                *
     ******************************************************/

    /******************************************************
     *               结束 Producer 参数                *
     ******************************************************/


    /******************************************************
     *               开始 ConsumeQueue 参数                *
     ******************************************************/

    // 单个 ConsumeQueue 文件尺寸
    public static int mapedFileSizeConsumeQueue = 300000 * ConsumeQueue.CQ_STORE_UNIT_SIZE;

    // 定义当多少页时应该写入到磁盘
    public static final int flushConsumeQueueLeastPages = 2;

    // 定义写入到磁盘的最大间隔
    public static int flushConsumeQueueThoroughInterval = 5 * 1000;

    // 定义写入到磁盘的最小间隔
    public static int flushConsumeQueueInterval = 500;

    // 是否允许重复
    public static boolean duplicationEnable = false;

    // 删除 ConsumeQueue 的间隔
    public static int deleteConsumeQueueFilesInterval = 30 * 1000;

    // 写入 ConsumerOffset 间隔
    public static int flushConsumerOffsetInterval = 500;


    /******************************************************
     *               结束 ConsumeQueue 参数                *
     ******************************************************/

    // 设置系统页是否超时检测
    public static long osPageCacheBusyTimeOutMills = 1000;

    /**
     * @Region 调试区域
     */
    // 设置是否打印 Flush 进程
    @Debug
    public static boolean printFlushProgress = true;


    public static int getMapedFileSizeConsumeQueue() {

        int factor = (int) Math.ceil(mapedFileSizeConsumeQueue / (ConsumeQueue.CQ_STORE_UNIT_SIZE * 1.0));
        return (int) (factor * ConsumeQueue.CQ_STORE_UNIT_SIZE);
    }


}



