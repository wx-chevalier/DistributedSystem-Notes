package wx.mq;

import java.io.File;
import java.lang.annotation.*;

public class MessageQueueConfig {

    // 存放数据的根目录
    @Important
    public String storePathRootDir = System.getProperty("user.home") + File.separator + "lstore";

    public MessageQueueConfig(String storePathRootDir) {
        this.storePathRootDir = storePathRootDir;
    }

    @Important
    public String getStorePathConsumerOffset() {
        return this.storePathRootDir + File.separator + "config" + File.separator + "consumerOffset.json";
    }

    public String getStorePathRootDir() {
        return this.storePathRootDir;
    }

    // 设置 PID 文件，用来标识程序是否正确退出
    @Important
    public String getStorePathPIDFile() {
        return this.storePathRootDir + File.separator + "mqpid";
    }

    // 批量发送的消息数量
    public static int batchSentMessageNums = 512;

    // 设置是否开启 CRC 校验
    public static boolean isCRCEnabled = false;

    /**
     * Description 重要属性
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
    protected @interface Important {
    }

    /**
     * Description 调试属性
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
    protected @interface Debug {
    }
}
