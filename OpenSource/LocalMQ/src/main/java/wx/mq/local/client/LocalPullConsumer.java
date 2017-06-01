package wx.mq.local.client;

import io.openmessaging.KeyValue;
import io.openmessaging.Message;
import io.openmessaging.PullConsumer;
import io.openmessaging.exception.ClientOMSException;
import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.memory.MemoryMessageQueue;
import wx.mq.MessageQueue;
import wx.mq.local.LocalMessageQueue;
import wx.mq.common.message.status.GetMessageResult;
import wx.mq.common.message.status.GetMessageStatus;
import wx.mq.local.consume.ConsumeQueue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static wx.mq.local.LocalMessageQueueConfig.defaultReadQueueNums;
import static wx.mq.local.LocalMessageQueueConfig.mapedFileSizeConsumeQueue;
import static wx.mq.local.message.serialization.LocalMessageDecoder.readMessagesFromGetMessageResult;

public class LocalPullConsumer implements PullConsumer {

    private MessageQueue messageQueue;

    private KeyValue properties;

    private String queue;

    private Set<String> buckets = new HashSet<>();

    private List<String> bucketList = new ArrayList<>();

    private int lastIndex = 0;

    // 全局唯一的 Producer ID 统计
    private static final AtomicInteger atomicRefId = new AtomicInteger();

    // 对象对应的编号
    private transient int refId;

    // 当前 Consumer 唯一标识的偏移量，用于得到不同的 queueId
    private int refOffset = 0;

    // 当前处理到的某个队列的偏移量 Topic-Offset
    private final ConcurrentHashMap<String, AtomicLong> consumerOffsetTable = new ConcurrentHashMap<>();

    // 用来记录某个 Topic 或者 Queue 是否被抓取完毕 true - 表示抓取完毕 false - 表示尚未完毕
    private ConcurrentHashMap<String, Boolean> isFinishedTable = new ConcurrentHashMap<>();

    // Consumer 内置的消息缓存队列
    ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();

    public LocalPullConsumer(KeyValue properties) {
        this.refId = atomicRefId.getAndIncrement();
        this.properties = properties;

        // 这里根据传入的 properties 初始化 MessageQueue
        this.messageQueue = LocalMessageQueue.getInstance(properties.getString("STORE_PATH"));
    }

    /**
     * Description 自定义由外部传入的 refId
     *
     * @param properties
     * @param refId
     */
    public LocalPullConsumer(KeyValue properties, int refId) {
        this.refId = refId;

        this.properties = properties;

        // 这里根据传入的 properties 初始化 MessageQueue
        this.messageQueue = LocalMessageQueue.getInstance(properties.getString("STORE_PATH"));
    }

    @Override
    public KeyValue properties() {
        return properties;
    }

    /**
     * Description 批量抓取消息，注意，这里只进行预抓取，仅当消费者真正获取后才会修正读取偏移量
     */
    private void batchPoll() {
        // 如果是 LocalMessageQueue
        // 执行预抓取
        LocalMessageQueue localMessageStore = (LocalMessageQueue) this.messageQueue;

        // 获取当前待抓取的桶名
        String bucket = bucketList.get((lastIndex) % (bucketList.size()));

        // 首先获取待抓取的队列和偏移
        long offsetInQueue = localMessageStore.getConsumerScheduler().queryOffsetAndLock("127.0.0.1:" + this.refId, bucket, this.getQueueId());

        // 如果当前待抓取的 queueId 已经被占用，则直接切换到下一个主题
        if (offsetInQueue == -2) {
            // 将当前主题设置为 true
            this.isFinishedTable.put(bucket, true);

            // 重置当前的 LastIndex 或者 RefOffset，即 queueId
            this.resetLastIndexOrRefOffsetWhenNotFound();

        } else {

            // 获取到了有效的队列偏移量之后，开始尝试获取消息
            consumerOffsetTable.put(bucket, new AtomicLong(offsetInQueue));

            // 设置每次最多抓一个文件内包含的消息数，等价于变相的一次性读完，注意，这里的数目还受到单个文件尺寸的限制
            GetMessageResult getMessageResult = localMessageStore.getMessage(bucket, this.getQueueId(), this.consumerOffsetTable.get(bucket).get() + 1, mapedFileSizeConsumeQueue / ConsumeQueue.CQ_STORE_UNIT_SIZE);

            // 如果没有找到数据，则切换到下一个
            if (getMessageResult.getStatus() != GetMessageStatus.FOUND) {

                // 将当前主题设置为 true
                this.isFinishedTable.put(bucket, true);

                this.resetLastIndexOrRefOffsetWhenNotFound();

            } else {

                // 这里不考虑 Consumer 被恶意干掉的情况，因此直接更新远端的 Offset 值
                localMessageStore.getConsumerScheduler().updateOffset("127.0.0.1:" + this.refId, bucket, this.getQueueId(), consumerOffsetTable.get(bucket).addAndGet(getMessageResult.getMessageCount()));

                // 首先从文件系统中一次性读出所有的消息
                ArrayList<DefaultBytesMessage> messages = readMessagesFromGetMessageResult(getMessageResult);

                // 将消息添加到队列中
                this.messages.addAll(messages);

                // 本次抓取成功后才开始抓取下一个
                lastIndex++;

            }
        }

    }

    /**
     * Description 在没有获取到消息时，重置当前的 Bucket 指针或者 queueId
     */
    public void resetLastIndexOrRefOffsetWhenNotFound() {

        // 判断当前是否存在没有全部抓完的主题
        if (this.isFinishedTable.containsValue(false)) {
            // 寻找到第一个仍存在值的 Bucket
            for (int index = 0; index < this.bucketList.size(); index++) {
                if (!this.isFinishedTable.get(bucketList.get(index))) {
                    lastIndex = index;

                    // 继续执行抓取
                    batchPoll();
                    break;
                }
            }
        } else {

            // 如果所有当前主题的所有内容全部加一了，则重置当前的队列数
            if (this.refOffset < defaultReadQueueNums) {
                this.refOffset++;
                batchPoll();
            }

            // 如果已经完成了一轮回转，则直接停止执行

        }
    }

    @Override
    public synchronized Message poll() {

        // 如果是 MemoryMessageQueue
        if (this.messageQueue instanceof MemoryMessageQueue) {
            if (buckets.size() == 0 || queue == null) {
                return null;
            }
            //use Round Robin
            int checkNum = 0;
            while (++checkNum <= bucketList.size()) {
                String bucket = bucketList.get((++lastIndex) % (bucketList.size()));
                Message message = messageQueue.pullMessage(queue, bucket);
                if (message != null) {
                    return message;
                }
            }
        } else {

            // 当缓存的 Messages 为空的时候，打印出来
            if (this.messages.isEmpty()) {
                this.batchPoll();
            }

            // 获取首个消息
            DefaultBytesMessage message = (DefaultBytesMessage) this.messages.poll();

            return message;

        }

        return null;
    }

    @Override
    public Message poll(KeyValue properties) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public void ack(String messageId) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public void ack(String messageId, KeyValue properties) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public synchronized void attachQueue(String queueName, Collection<String> topics) {
        if (queue != null && !queue.equals(queueName)) {
            throw new ClientOMSException("You have alreadly attached to a queue " + queue);
        }
        queue = queueName;
        buckets.add(queueName);
        consumerOffsetTable.put(queueName, new AtomicLong());
        isFinishedTable.put(queueName, false);

        for (String topic : topics) {
            consumerOffsetTable.put(topic, new AtomicLong());

            isFinishedTable.put(topic, false);
        }
        buckets.addAll(topics);
        bucketList.clear();
        bucketList.addAll(buckets);
    }

    public void setMessageQueue(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    /**
     * Description 根据 refId 获取 queueId
     *
     * @return
     */
    public int getQueueId() {
        return (refId + refOffset) % defaultReadQueueNums;
    }

    public void setRefOffset(int refOffset) {
        this.refOffset = refOffset;
    }
}
