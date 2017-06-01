package wx.mq.local.client;

import io.openmessaging.*;
import io.openmessaging.exception.ClientOMSException;
import wx.mq.common.message.DefaultBytesMessage;
import wx.mq.util.ds.DefaultKeyValue;
import wx.mq.MessageQueue;
import wx.mq.local.LocalMessageQueue;
import wx.mq.common.message.DefaultMessageFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static wx.mq.local.LocalMessageQueueConfig.batchSentMessageNums;
import static wx.mq.local.LocalMessageQueueConfig.defaultReadQueueNums;

public class LocalProducer implements Producer {

    // 日志工具
    private final static Logger log = Logger.getLogger(Producer.class.getName());

    // 全局唯一的 Producer ID 统计
    private static final AtomicInteger atomicRefId = new AtomicInteger(0);

    private static final AtomicInteger flushedProducer = new AtomicInteger(0);

    // 对象对应的编号
    private transient int refId;

    private int queueId;

    private DefaultMessageFactory messageFactory = new DefaultMessageFactory();

    // 默认为内存中的 MessageQueue
    private MessageQueue messageQueue;

    private KeyValue properties;

    // 存放批量提交的消息队列
    private ConcurrentHashMap<String, ArrayList<DefaultBytesMessage>> messagesBatchCache = new ConcurrentHashMap<>();

    public LocalProducer(KeyValue properties) {

        this.refId = atomicRefId.getAndIncrement();

        // 判断 MessageQueue 是否启动，否则休眠
        this.queueId = this.refId % defaultReadQueueNums;

        this.properties = properties != null ? properties : new DefaultKeyValue();

        // 这里根据传入的 properties 初始化 MessageQueue
        this.messageQueue = LocalMessageQueue.getInstance(properties.getString("STORE_PATH"));

    }


    @Override
    public BytesMessage createBytesMessageToTopic(String topic, byte[] body) {

        // 构建消息
        DefaultBytesMessage message = (DefaultBytesMessage) messageFactory.createBytesMessageToTopic(topic, body);

        return message;
    }

    @Override
    public BytesMessage createBytesMessageToQueue(String queue, byte[] body) {
        BytesMessage message = messageFactory.createBytesMessageToQueue(queue, body);

        return message;
    }

    @Override
    public void start() {

    }

    @Override
    public void shutdown() {

    }

    public void flush() {

        // 执行 Flush 操作
        this.flushInner();

        // 如果已经到了最后一个 Producer，则执行 Shutdown
        if (atomicRefId.get() == flushedProducer.incrementAndGet()) {
            this.messageQueue.shutdown();
        }
    }

    public void flush(boolean shutdown) {

        this.flushInner();

        // 如果已经到了最后一个 Producer，则执行 Shutdown
        if (atomicRefId.get() == flushedProducer.incrementAndGet()) {

            if (shutdown) {
                this.messageQueue.shutdown();

            }
        }
    }

    private void flushInner() {
        // 将全部剩余消息发送
        for (Map.Entry<String, ArrayList<DefaultBytesMessage>> entry : this.messagesBatchCache.entrySet()) {

            String bucket = entry.getKey();

            ArrayList messages = entry.getValue();

            // 强制发送剩余消息
            if (messages.size() > 0) {
                messageQueue.putMessages(bucket, messages);
            }

        }

        this.messageQueue.flush();
    }


    @Override
    public KeyValue properties() {
        return properties;
    }

    /**
     * Description 实际发送消息函数，注意，按照 refId 对总的对列数取余
     *
     * @param msg a message will be sent
     */
    @Override
    public void send(Message msg) {

        DefaultBytesMessage message = (DefaultBytesMessage) msg;

        LocalMessageQueue localMessageStore = (LocalMessageQueue) messageQueue;

        if (message == null) throw new ClientOMSException("Message should not be null");
        String topic = message.headers().getString(MessageHeader.TOPIC);
        String queue = message.headers().getString(MessageHeader.QUEUE);

        if ((topic == null && queue == null) || (topic != null && queue != null)) {
            throw new ClientOMSException(String.format("Queue:%s Topic:%s should put one and only one", true, queue));
        }

        // 设置队列编号
        message.setQueueId(this.queueId);

        // 进行消息构建
        message.build();

        // 将消息添加到待发送队列中
        this.messagesBatchCache.putIfAbsent(message.getBucket(), new ArrayList<>());

        ArrayList messages = this.messagesBatchCache.get(message.getBucket());

        // 将消息添加到 messages 中
        messages.add(message);

        // 如果已经达到了发送数目，则立刻发送
        if (messages.size() > batchSentMessageNums) {

            // 唯有获得成功之后才进行消息写入，否则继续进行消息添加
            messageQueue.putMessages(message.getBucket(), messages);

            // 移除原有的消息队列
            this.messagesBatchCache.remove(message.getBucket());

        }

    }

    @Override
    public void send(Message message, KeyValue properties) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public Promise<Void> sendAsync(Message message) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public Promise<Void> sendAsync(Message message, KeyValue properties) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public void sendOneway(Message message) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public void sendOneway(Message message, KeyValue properties) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public BatchToPartition createBatchToPartition(String partitionName) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public BatchToPartition createBatchToPartition(String partitionName, KeyValue properties) {
        throw new UnsupportedOperationException("Unsupported");
    }

    public int getRefId() {
        return refId;
    }

    public io.openmessaging.MessageFactory getMessageFactory() {
        return messageFactory;
    }

    public MessageQueue getMessageQueue() {
        return messageQueue;
    }

    public KeyValue getProperties() {
        return properties;
    }

    public void setMessageQueue(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }
}
