package wx.mq;

import io.openmessaging.Message;
import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.common.message.status.PutMessageResult;
import wx.mq.local.stats.RunningFlags;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static wx.mq.util.fs.FSExtra.ensureDirOK;

/**
 * Description Broker 的核心消息存储
 */
public abstract class MessageQueue {

    // 判断 MessageQueue 是否已经停止运行
    protected volatile boolean shutdown = true;

    // 当前 MessageQueue 运行状态
    public final RunningFlags runningFlags = new RunningFlags();

    public RunningFlags getRunningFlags() {
        return runningFlags;
    }

    public abstract MessageQueueConfig getMessageQueueConfig();

    public abstract long getConfirmOffset();

    public abstract PutMessageResult putMessage(String topicOrQueueName, Message message);

    public abstract PutMessageResult putMessages(String topicOrQueueName, List<DefaultBytesMessage> messages);

    public abstract Message pullMessage(String queue, String bucket);

    public boolean isShutdown() {
        return shutdown;
    }

    public abstract void flush();

    public abstract void shutdown();


    /**
     * Description 创建临时文件
     *
     * @throws IOException
     */
    public void createTempFile() throws IOException {
        File file = new File(this.getMessageQueueConfig().getStorePathPIDFile());
        ensureDirOK(file.getParent());
        boolean result = file.createNewFile();
    }

    /**
     * Description 判断临时文件是否存在
     *
     * @return
     */
    public boolean isTempFileExist() {
        return new File(this.getMessageQueueConfig().getStorePathPIDFile()).exists();
    }
}
