/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wx.mq.local.message.service;

/*
| 序号   | 消息存储结构            | 备注                                       | 长度（字节数）              |
| ---- | ----------------- | ---------------------------------------- | -------------------- |
| 1    | TOTALSIZE         | 消息大小                                     | 4                    |
| 2    | MAGICCODE         | 消息的 MAGIC CODE                           | 4                    |
| 3    | BODYCRC           | 消息体 BODY CRC，用于重启时校验                     | 4                    |
| 4    | QUEUEID           | 队列编号，queueID                             | 4                    |
| 5    | QUEUEOFFSET       | 自增值，不是真正的 consume queue 的偏移量，可以代表这个队列中消息的个数，要通过这个值查找到 consume queue 中数据，QUEUEOFFSET * 12 才是偏移地址 | 8                    |
| 6    | PHYSICALOFFSET    | 消息在 commitLog 中的物理起始地址偏移量                | 8                    |
| 7    | STORETIMESTAMP    | 存储时间戳                                    | 8                    |
| 8    | BODY              | 前 4 个字节存放消息体大小值，后 bodyLength 大小的空间存储消息体内容 | 4 + bodyLength       |
| 9    | TOPICORQUEUENAME  | 前 1 个字节存放 Topic 大小，后存放 topicOrQueueNameLength 大小的主题名 | 1 + topicOrQueueNameLength    |
| 10   | headers*          | 前 2 个字节（short）存放头部大小，后存放 headersLength 大小的头部数据 | 2 + headersLength    |
| 11   | properties*       | 前 2 个字节（short）存放属性值大小，后存放 propertiesLength 大小的属性数据 | 2 + propertiesLength |
 */
public class PostPutMessageRequest {


    // 主题名
    private final String topic;

    // 队列名
    private final int queueId;

    // 在 MessageStore 中位移
    private final long commitLogOffset;

    // 消息尺寸
    private final int msgSize;

    // 存储时间戳
    private final long storeTimestamp;

    // 在 ConsumeQueue 中的偏移
    private final long consumeQueueOffset;

    // 消息是否成功
    private final boolean success;

    // 位映射
    private byte[] bitMap;

    public PostPutMessageRequest(
            final String topic,
            final int queueId,
            final long commitLogOffset,
            final int msgSize,
            final long storeTimestamp,
            final long consumeQueueOffset
    ) {
        this.topic = topic;

        this.queueId = queueId;

        this.commitLogOffset = commitLogOffset;

        this.msgSize = msgSize;

        this.storeTimestamp = storeTimestamp;

        this.consumeQueueOffset = consumeQueueOffset;

        this.success = true;
    }

    public PostPutMessageRequest(int size) {

        this.topic = "";

        this.queueId = 0;

        this.commitLogOffset = 0;

        this.msgSize = size;

        this.storeTimestamp = 0;

        this.consumeQueueOffset = 0;

        this.success = false;
    }

    public PostPutMessageRequest(int size, boolean success) {

        this.topic = "";

        this.queueId = 0;

        this.commitLogOffset = 0;

        this.msgSize = size;

        this.storeTimestamp = 0;

        this.consumeQueueOffset = 0;

        this.success = success;

    }

    public String getTopic() {
        return topic;
    }

    public int getQueueId() {
        return queueId;
    }

    public long getCommitLogOffset() {
        return commitLogOffset;
    }

    public int getMsgSize() {
        return msgSize;
    }

    public long getStoreTimestamp() {
        return storeTimestamp;
    }

    public long getConsumeQueueOffset() {
        return consumeQueueOffset;
    }

    public boolean isSuccess() {
        return success;
    }

    public byte[] getBitMap() {
        return bitMap;
    }

    public void setBitMap(byte[] bitMap) {
        this.bitMap = bitMap;
    }
}
