package wx.mq.memory.client;


import io.openmessaging.BatchToPartition;
import io.openmessaging.BytesMessage;
import io.openmessaging.KeyValue;
import io.openmessaging.Message;
import io.openmessaging.MessageFactory;
import io.openmessaging.MessageHeader;
import io.openmessaging.Producer;
import io.openmessaging.Promise;
import io.openmessaging.exception.ClientOMSException;
import wx.mq.MessageQueue;
import wx.mq.common.message.DefaultMessageFactory;
import wx.mq.memory.MemoryMessageQueue;

import java.util.concurrent.atomic.AtomicLong;

public class MemoryProducer implements Producer {

    // 全局唯一的 Producer ID 统计
    private static final AtomicLong atomicRefId = new AtomicLong();

    // 对象对应的编号
    private transient long refId;

    private MessageFactory messageFactory = new DefaultMessageFactory();
    private MessageQueue messageQueue = MemoryMessageQueue.getInstance();

    private KeyValue properties;

    public MemoryProducer(KeyValue properties) {

        this.refId = atomicRefId.incrementAndGet();

        this.properties = properties;
    }


    @Override
    public BytesMessage createBytesMessageToTopic(String topic, byte[] body) {

        BytesMessage message = messageFactory.createBytesMessageToTopic(topic, body);

        // 存放 Producer 的唯一标识
        message.putHeaders("ProducerRef", this.refId);

        return message;
    }

    @Override
    public BytesMessage createBytesMessageToQueue(String queue, byte[] body) {
        BytesMessage message = messageFactory.createBytesMessageToQueue(queue, body);

        // 存放 Producer 的唯一标识
        message.putHeaders("ProducerRef", this.refId);

        return message;
    }

    @Override
    public void start() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public KeyValue properties() {
        return properties;
    }

    @Override
    public void send(Message message) {
        if (message == null) throw new ClientOMSException("Message should not be null");
        String topic = message.headers().getString(MessageHeader.TOPIC);
        String queue = message.headers().getString(MessageHeader.QUEUE);
        if ((topic == null && queue == null) || (topic != null && queue != null)) {
            throw new ClientOMSException(String.format("Queue:%s Topic:%s should put one and only one", true, queue));
        }

        messageQueue.putMessage(topic != null ? topic : queue, message);
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

    @Override
    public void flush() {

    }
}
