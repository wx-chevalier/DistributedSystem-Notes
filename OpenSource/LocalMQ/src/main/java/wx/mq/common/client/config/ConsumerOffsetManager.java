package wx.mq.common.client.config;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import wx.mq.MessageQueue;
import wx.mq.local.consume.ConsumeQueue;
import wx.mq.util.fs.serialization.JSONPersister;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Description 用于管理文件中
 */
public class ConsumerOffsetManager extends JSONPersister {

    // 日志记录
    public final static Logger log = Logger.getLogger(ConsumeQueue.class.getName());

    // 存放映射到内存中
    private ConcurrentHashMap<String/* topic */, ConcurrentHashMap<Integer/*queueId*/, Long>> offsetTable =
            new ConcurrentHashMap<String, ConcurrentHashMap<Integer, Long>>(512);

    // 存放某个 Topic 下面的某个 Queue 被某个 Consumer 占用的信息
    private ConcurrentHashMap<String/* topic */, ConcurrentHashMap<Integer/*queueId*/, String/*refId*/>> queueIdOccupiedByConsumerTable =
            new ConcurrentHashMap<String, ConcurrentHashMap<Integer, String>>(512);

    private final MessageQueue messageQueue;

    public ConsumerOffsetManager(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    /**
     * Description 某个 Consumer 读取了某个消息之后，将该 Offset 提交到 ConsumerOffsetManager 中
     *
     * @param clientHostAndPort
     * @param topic
     * @param queueId
     * @param offset
     */
    public void commitOffset(final String clientHostAndPort, final String topic, final int queueId, final long offset) {

        // topic
        String key = topic;

        // 获取 OffsetTable 中该主题对应的偏移表
        ConcurrentHashMap<Integer, Long> map = this.offsetTable.get(key);

        // 如果 map 不存在，则创建新的实例并且插入
        if (null == map) {

            map = new ConcurrentHashMap<Integer, Long>(32);

            map.put(queueId, offset);

            this.offsetTable.put(key, map);
        } else {

            Long storeOffset = map.get(queueId);

            if (storeOffset == null || offset > storeOffset) {
                map.put(queueId, offset);
            }

            if (storeOffset != null && offset < storeOffset) {
                log.warning(String.format("[NOTIFYME]update consumer offset less than store. clientHostAndPort=%s, key=%s, queueId=%s, requestOffset=%s, storeOffset=%s", clientHostAndPort, key, queueId, offset, storeOffset));
            }
        }

    }

    /**
     * Description 修正某个 ConsumerOffset 队列中的值
     *
     * @param topic
     * @param queueId
     * @return
     */
    public long queryOffsetAndLock(final String clientHostAndPort, final String topic, final int queueId) {

        String key = topic;

        // 首先判断该 Topic-queueId 是否被占用
        if (this.queueIdOccupiedByConsumerTable.containsKey(topic)) {

            // 如果该值已经存在，则表示被占用
            if (this.queueIdOccupiedByConsumerTable.get(topic).containsKey(queueId)) {

                // 注意，这里还需要判断下是否为同一个 Consumer 请求的，避免自旋
                if (!this.queueIdOccupiedByConsumerTable.get(topic).get(queueId).equals(clientHostAndPort)) {
                    return -2;
                }
            }
        }

        // 如果没有被占用，则此时宣告占用
        ConcurrentHashMap<Integer, String> consumerQueueIdMap = this.queueIdOccupiedByConsumerTable.get(key);

        if (null == consumerQueueIdMap) {
            consumerQueueIdMap = new ConcurrentHashMap<Integer, String>(32);
            consumerQueueIdMap.put(queueId, clientHostAndPort);
            this.queueIdOccupiedByConsumerTable.put(key, consumerQueueIdMap);
        } else {
            consumerQueueIdMap.put(queueId, clientHostAndPort);
        }

        // 真实进行查找操作
        ConcurrentHashMap<Integer, Long> map = this.offsetTable.get(key);
        if (null != map) {
            Long offset = map.get(queueId);
            if (offset != null)
                return offset;
        }

        // 默认返回值为 -1
        return -1;
    }

    // @Todo 将当前对象序列化为字符串并且传递给远端
    @Override
    public String encode() {
        return encode(false);
    }

    @Override
    public String getPersistFilePath() {
        return messageQueue.getMessageQueueConfig().getStorePathConsumerOffset();
    }

    /**
     * Description 将 JSON 字符串转化为当前对象
     *
     * @param jsonString
     */
    @Override
    public void decode(String jsonString) throws ParseException {

        JSONParser parser = new JSONParser();

        JSONObject jsonObject = (JSONObject) parser.parse(jsonString);

        // 首先解析第一层 topic-queueOffsetTable
        for (Object topic : jsonObject.keySet()) {

            // 填入当前 offsetTable 中
            this.offsetTable.putIfAbsent(String.valueOf(topic), new ConcurrentHashMap<Integer, Long>());

            ConcurrentHashMap<Integer, Long> queueOffsetTable = this.offsetTable.get(String.valueOf(String.valueOf(topic)));

            JSONObject queueOffsetTableJSONObject = (JSONObject) jsonObject.get(topic);

            // 抽取第二层
            for (Object queueId : queueOffsetTableJSONObject.keySet()) {

                queueOffsetTable.put(Integer.valueOf(String.valueOf(queueId)), Long.valueOf(String.valueOf(queueOffsetTableJSONObject.get(String.valueOf(queueId)))));


            }

        }

    }

    /**
     * Description 将当前对象转化为 JSON 字符串
     *
     * @param prettyFormat
     * @return
     */
    @Override
    public String encode(boolean prettyFormat) {


        JSONObject topicTable = new JSONObject();

        // 遍历所有的 Topic
        for (Map.Entry<String, ConcurrentHashMap<Integer, Long>> topicEntry : this.offsetTable.entrySet()) {

            JSONObject queueOffsetTable = new JSONObject();

            for (Map.Entry<Integer, Long> queueEntry : topicEntry.getValue().entrySet()) {

                // 依次放置所有的数据
                queueOffsetTable.put(queueEntry.getKey(), queueEntry.getValue());

            }

            topicTable.put(topicEntry.getKey(), queueOffsetTable);

        }

        // 返回 JSON 字符串
        return topicTable.toJSONString();


    }


    public ConcurrentHashMap<String, ConcurrentHashMap<Integer, Long>> getOffsetTable() {
        return offsetTable;
    }

    public void setOffsetTable(ConcurrentHashMap<String, ConcurrentHashMap<Integer, Long>> offsetTable) {
        this.offsetTable = offsetTable;
    }

    public Map<Integer, Long> queryOffsetMap(final String topic) {
        String key = topic;
        return this.offsetTable.get(key);
    }
}
