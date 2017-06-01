package wx.mq.embedded;

import wx.mq.MessageQueueConfig;

import java.io.File;

/**
 * Description 嵌入式的 MessageQueue
 */
public class EmbeddedMessageQueueConfig extends MessageQueueConfig {

    public EmbeddedMessageQueueConfig(String storePathRootDir) {
        super(storePathRootDir);
    }

    // 设置 BucketQueue 单文件大小，默认为 16MB
    @Important
    public static int bucketQueuePartitionSize = 1024 * 1024 * 1024;

    public static int bucketQueueNum = 10;

    // 设置在消息创建阶段随机生成的 ConsumeQueue 地址
    @Important
    public String getBucketQueueStorePath() {
        return this.storePathRootDir + File.separator + "bucketqueue";
    }

}
