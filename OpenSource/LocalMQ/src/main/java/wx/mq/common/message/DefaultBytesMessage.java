package wx.mq.common.message;

import io.openmessaging.BytesMessage;
import io.openmessaging.KeyValue;
import io.openmessaging.Message;
import io.openmessaging.MessageHeader;
import wx.mq.common.message.MessageUtil;
import wx.mq.embedded.message.serialization.EmbeddedMessageSerializer;
import wx.mq.local.message.serialization.LocalMessageSerializer;
import wx.mq.util.ds.DefaultKeyValue;

import static wx.mq.local.message.serialization.LocalMessageSerializer.CHARSET_UTF8;
import static wx.mq.util.ds.CollectionTransformer.keyValue2String;
import static wx.mq.util.ds.DateTimeUtil.now;

public class DefaultBytesMessage implements BytesMessage {

    // 消息头
    private KeyValue headers = new DefaultKeyValue();

    // 消息属性
    private KeyValue properties = new DefaultKeyValue();

    // 消息体
    private byte[] body;

    // 消息体的 CRC 校验
    private int bodyCRC;

    // 消息存储时间戳
    private long storeTimestamp;

    // 消息编号
    private String msgId;

    // 消息被划分到队列编号
    private int queueId;

    // 消息被划分到 MessageStore 中的位移
    private long commitLogOffset;

    // 消息被划分到队列的位移
    private long queueOffset;

    // 消息的存储体积
    private int storeSize;

    // 主题或者队列名
    private String topicOrQueueName;

    // 仅做调试使用
    private String _bodyStr;


    /**
     * Description 设置 Body 的构造函数
     *
     * @param body
     */
    public DefaultBytesMessage(byte[] body) {

        this.body = body;

        // 其他设置为默认值
        this.queueId = 0;

        this.storeTimestamp = now();
    }

    public DefaultBytesMessage(int storeSize, String topicOrQueueName, KeyValue headers, KeyValue properties, byte[] body, int bodyCRC, long storeTimestamp, int queueId, long queueOffset) {

        this.messageLength = storeSize;
        this.storeSize = storeSize;
        this.topicOrQueueName = topicOrQueueName;
        this.headers = headers;
        this.properties = properties;
        this.body = body;
        this.bodyCRC = bodyCRC;
        this.storeTimestamp = storeTimestamp;
        this.queueId = queueId;
        this.queueOffset = queueOffset;

        // 仅做调试使用
        this._bodyStr = new String(body);
    }

    private byte[] headersByteArray;

    private byte[] propertiesByteArray;

    private int messageLength;

    private byte[] bucketByteArray;

    public DefaultBytesMessage(int totalSize, KeyValue headers, KeyValue properties, byte[] body) {
        this.messageLength = totalSize;
        this.headers = headers;
        this.properties = properties;
        this.body = body;
    }

    /**
     * Description 在消息发送之前构建各式各样的需要计算的值
     */
    public void build() {

        headersByteArray = keyValue2String(this.headers()).getBytes();

        propertiesByteArray = keyValue2String(this.properties()).getBytes();

        bucketByteArray = this.getBucket().getBytes(CHARSET_UTF8);

        messageLength = LocalMessageSerializer.calMsgLength(this, headersByteArray, propertiesByteArray);

    }

    public void buildInEmbedded() {

        headersByteArray = keyValue2String(this.headers()).getBytes();

        propertiesByteArray = keyValue2String(this.properties()).getBytes();

        bucketByteArray = this.getBucket().getBytes(CHARSET_UTF8);

        messageLength = EmbeddedMessageSerializer.calMsgLength(this, headersByteArray, propertiesByteArray);

    }

    public String getBucket() {
        String topic = this.headers().getString(MessageHeader.TOPIC);
        String queue = this.headers().getString(MessageHeader.QUEUE);

        if (topic != null) {
            return topic;
        } else {
            return queue;
        }
    }

    public byte[] getHeadersByteArray() {
        return headersByteArray;
    }

    public byte[] getPropertiesByteArray() {
        return propertiesByteArray;
    }

    public int getMessageLength() {
        return messageLength;
    }

    public byte[] getBucketByteArray() {
        return bucketByteArray;
    }

    /**
     * Description 完全构造函数
     *
     * @param body
     */
    public DefaultBytesMessage(byte[] body,
                               int queueId, String msgId,
                               long storeTimestamp
    ) {
        this.body = body;
        this.queueId = queueId;
        this.msgId = msgId;
        this.storeTimestamp = storeTimestamp;
    }

    /**
     * Description 获取当前消息发往的主题或者队列名
     *
     * @return
     */
    public String getTopicOrQueueName() {
        Boolean isTopic = MessageUtil.validateMessage(this);

        return isTopic ?
                headers.getString(MessageHeader.TOPIC) :
                headers.getString(MessageHeader.QUEUE);

    }

    @Override
    public byte[] getBody() {
        return body;
    }

    @Override
    public BytesMessage setBody(byte[] body) {
        this.body = body;
        return this;
    }

    @Override
    public KeyValue headers() {
        return headers;
    }

    @Override
    public KeyValue properties() {
        return properties;
    }

    @Override
    public Message putHeaders(String key, int value) {
        headers.put(key, value);
        return this;
    }

    @Override
    public Message putHeaders(String key, long value) {
        headers.put(key, value);
        return this;
    }

    @Override
    public Message putHeaders(String key, double value) {
        headers.put(key, value);
        return this;
    }

    @Override
    public Message putHeaders(String key, String value) {
        headers.put(key, value);
        return this;
    }

    @Override
    public Message putProperties(String key, int value) {
        if (properties == null) properties = new DefaultKeyValue();
        properties.put(key, value);
        return this;
    }

    @Override
    public Message putProperties(String key, long value) {
        if (properties == null) properties = new DefaultKeyValue();
        properties.put(key, value);
        return this;
    }

    @Override
    public Message putProperties(String key, double value) {
        if (properties == null) properties = new DefaultKeyValue();
        properties.put(key, value);
        return this;
    }

    @Override
    public Message putProperties(String key, String value) {
        if (properties == null) properties = new DefaultKeyValue();
        properties.put(key, value);
        return this;
    }


    /**
     * Description 获取属性对应的字符串
     *
     * @return
     */
    public String getPropertiesString() {
        return keyValue2String(this.properties());
    }

    /**
     * Description 获取头部对应的字符串
     *
     * @return
     */
    public String getHeadersString() {

        return keyValue2String(this.headers);
    }

    public long getStoreTimestamp() {
        return storeTimestamp;
    }

    public void setStoreTimestamp(long storeTimestamp) {
        this.storeTimestamp = storeTimestamp;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public int getQueueId() {
        return queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    public long getQueueOffset() {
        return queueOffset;
    }

    public void setQueueOffset(long queueOffset) {
        this.queueOffset = queueOffset;
    }

    public int getStoreSize() {
        return storeSize;
    }

    public void setStoreSize(int storeSize) {
        this.storeSize = storeSize;
    }

    public int getBodyCRC() {
        return bodyCRC;
    }

    public void setBodyCRC(int bodyCRC) {
        this.bodyCRC = bodyCRC;
    }

    public long getCommitLogOffset() {
        return commitLogOffset;
    }

    public void setCommitLogOffset(long commitLogOffset) {
        this.commitLogOffset = commitLogOffset;
    }

    @Override
    public String toString() {
        return "DefaultBytesMessage{" +
                ", headers=" + headers + "\n" +
                ", properties=" + properties + "\n" +
                ", body=" + new String(body) + "\n" +
                ", bodyCRC=" + bodyCRC + "\n" +
                ", queueId=" + queueId + "\n" +
                '}';
    }
}
