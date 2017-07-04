package wx.mq.memory;

import io.openmessaging.Message;
import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.MessageQueueConfig;
import wx.mq.common.message.status.PutMessageResult;
import wx.mq.common.message.status.PutMessageStatus;
import wx.mq.MessageQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description 最简单的基于内存的 MessageQueue
 */
public class MemoryMessageQueue extends MessageQueue {

    private static final MessageQueue INSTANCE = new MemoryMessageQueue();

    public static MessageQueue getInstance() {
        return INSTANCE;
    }

    private Map<String, ArrayList<Message>> messageBuckets = new HashMap<>();

    private Map<String, HashMap<String, Integer>> queueOffsets = new HashMap<>();

    /**
     * Description 单条消息提交
     *
     * @param bucket
     * @param message
     * @return
     */
    public synchronized PutMessageResult putMessage(String bucket, Message message) {
        if (!messageBuckets.containsKey(bucket)) {
            messageBuckets.put(bucket, new ArrayList<>(1024));
        }
        ArrayList<Message> bucketList = messageBuckets.get(bucket);
        bucketList.add(message);

        return new PutMessageResult(PutMessageStatus.PUT_OK, null);
    }


    /**
     * Description 批量消息提交
     *
     * @param bucket
     * @param messages
     * @return
     */
    @Override
    public PutMessageResult putMessages(String bucket, List<DefaultBytesMessage> messages) {

        if (!messageBuckets.containsKey(bucket)) {
            messageBuckets.put(bucket, new ArrayList<>(1024));
        }
        ArrayList<Message> bucketList = messageBuckets.get(bucket);
        bucketList.addAll(messages);

        return new PutMessageResult(PutMessageStatus.PUT_OK, null);

    }

    /**
     * Description 进行消息拉取操作
     *
     * @param queue
     * @param bucket
     * @return
     */
    public synchronized Message pullMessage(String queue, String bucket) {
        ArrayList<Message> bucketList = messageBuckets.get(bucket);
        if (bucketList == null) {
            return null;
        }
        HashMap<String, Integer> offsetMap = queueOffsets.get(queue);
        if (offsetMap == null) {
            offsetMap = new HashMap<>();
            queueOffsets.put(queue, offsetMap);
        }
        int offset = offsetMap.getOrDefault(bucket, 0);
        if (offset >= bucketList.size()) {
            return null;
        }
        Message message = bucketList.get(offset);
        offsetMap.put(bucket, ++offset);
        return message;
    }

    @Override
    public void flush() {

        // 不进行任何操作

    }

    @Override
    public void shutdown() {

        // 情况申请的空间
        this.messageBuckets.clear();
        this.queueOffsets.clear();

    }

    @Override
    public MessageQueueConfig getMessageQueueConfig() {
        return null;
    }

    @Override
    public long getConfirmOffset() {
        return 0;
    }
}
