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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.openmessaging;

/**
 * The {@code MessageHeader} class describes each OMS message header field.
 * A message's complete header is transmitted to all OMS clients that receive the
 * message.
 *
 * @author vintagewang@apache.org
 * @author yukon@apache.org
 *
 * @version OMS 1.0
 * @since OMS 1.0
 */
public class MessageHeader {
    /**
     * The {@code MESSAGE_ID} header field contains a value that uniquely identifies
     * each message sent by a {@code Producer}.
     * <p>
     * When a message is sent, MESSAGE_ID is ignored.
     * <p>
     * When the send method returns it contains a producer-assigned value.
     */
    public static final String MESSAGE_ID = "MessageId";

    /**
     * The {@code TOPIC} header field is the destination which the message is being sent.
     * <p>
     * When a message is sent this value is should be set properly.
     * <p>
     * When a message is received, its {@code TOPIC} value must be equivalent to the
     * value assigned when it was sent.
     */
    public static final String TOPIC = "Topic";

    /**
     * The {@code QUEUE} header field is the destination which the message is being sent.
     * <p>
     * When a message is sent this value is should be set properly.
     * <p>
     * When a message is received, its {@code QUEUE} value must be equivalent to the
     * value assigned when it was sent.
     */
    public static final String QUEUE = "Queue";

    /**
     * The {@code BORN_TIMESTAMP} header field contains the time a message was handed
     * off to a {@code Producer} to be sent.
     * <p>
     * When a message is sent, BORN_TIMESTAMP will be set with current timestamp as the born
     * timestamp of a message in client side, on return from the send method, the message's
     * BORN_TIMESTAMP header field contains this value. When a message is received its
     * BORN_TIMESTAMP header field contains this same value.
     * <p>
     * This filed is a {@code long} value, measured in milliseconds.
     */
    public static final String BORN_TIMESTAMP = "BornTimestamp";

    /**
     * The {@code BORN_HOST} header field contains the born host info of a message in client side.
     * <p>
     * When a message is sent, BORN_HOST will be set with the local host info,
     * on return from the send method, the message's BORN_HOST header field contains this value.
     * When a message is received its BORN_HOST header field contains this same value.
     */
    public static final String BORN_HOST = "BornHost";

    /**
     * The {@code STORE_TIMESTAMP} header field contains the store timestamp of a message in server side.
     * <p>
     * When a message is sent, STORE_TIMESTAMP is ignored.
     * <p>
     * When the send method returns it contains a server-assigned value.
     * <p>
     * This filed is a {@code long} value, measured in milliseconds.
     */
    public static final String STORE_TIMESTAMP = "StoreTimestamp";

    /**
     * The {@code STORE_HOST} header field contains the store host info of a message in server side.
     * <p>
     * When a message is sent, STORE_HOST is ignored.
     * <p>
     * When the send method returns it contains a server-assigned value.
     */
    public static final String STORE_HOST = "StoreHost";

    /**
     * The {@code START_TIME} header field contains the start timestamp that a message
     * can be delivered to consumer client.
     * <p>
     * If START_TIME field isn't set explicitly, use BORN_TIMESTAMP as the start timestamp.
     * <p>
     * This filed is a {@code long} value, measured in milliseconds.
     */
    public static final String START_TIME = "StartTime";

    /**
     * The {@code STOP_TIME} header field contains the stop timestamp that a message
     * should be discarded after this timestamp, and no consumer can consume this message.
     * <p>
     * {@code (START_TIME ~ STOP_TIME)} represents a absolute valid interval that a message
     * can be delivered in it.
     * <p>
     * If a earlier timestamp is set than START_TIME, that means the message does not expire.
     * <p>
     * This filed is a {@code long} value, measured in milliseconds.
     * <p>
     * When an undelivered message's expiration time is reached, the message should be destroyed.
     * OMS does not define a notification of message expiration.
     */
    public static final String STOP_TIME = "StopTime";

    /**
     * The {@code TIMEOUT} header field contains the expiration time, it represents a
     * time-to-live value.
     * <p>
     * {@code (BORN_TIMESTAMP ~ BORN_TIMESTAMP + TIMEOUT)} represents a relative valid interval
     * that a message can be delivered in it.
     * If the TIMEOUT field is specified as zero, that indicates the message does not expire.
     * <p>
     * The TIMEOUT header field has higher priority than START_TIME/STOP_TIME header fields.
     * <p>
     * When an undelivered message's expiration time is reached, the message should be destroyed.
     * OMS does not define a notification of message expiration.
     */
    public static final String TIMEOUT = "Timeout";

    /**
     * The {@code PRIORITY} header field contains the priority level of a message,
     * a message with higher priority values should be delivered preferentially.
     * <p>
     * OMS defines a ten level priority value with 0 as the lowest priority and 9 as the highest.
     * OMS does not require that a provider strictly implement priority ordering of messages;
     * however, it should do its best to deliver expedited messages ahead of normal messages.
     * <p>
     * If PRIORITY field isn't set explicitly, use {@code 4} as the default priority.
     */
    public static final String PRIORITY = "Priority";

    /**
     * The {@code RELIABILITY} header field contains the reliability level of a message.
     * A MOM server should guarantee the reliability level for a message.
     */
    public static final String RELIABILITY = "Reliability";

    /**
     * The {@code SEARCH_KEY} header field contains index search key of a message.
     * Clients can query similar messages by search key, and have a quick response.
     */
    public static final String SEARCH_KEY = "SearchKey";

    /**
     * The {@code SCHEDULE_EXPRESSION} header field contains schedule expression of a message.
     * <p>
     * The message will be delivered by the specified SCHEDULE_EXPRESSION, which is a CRON expression.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Cron#CRON_expression">https://en.wikipedia.org/wiki/Cron#CRON_expression</a>
     */
    public static final String SCHEDULE_EXPRESSION = "ScheduleExpression";

    /**
     * The {@code SHARDING_KEY} header field contains the sharding key a message.
     */
    public static final String SHARDING_KEY = "ShardingKey";

    /**
     * The {@code SHARDING_PARTITION} header field contains the sharding partition key a message.
     * The messages with same SHARDING_PARTITION should be sent to the same partition of a destination.
     */
    public static final String SHARDING_PARTITION = "ShardingPartition";

    /**
     * The {@code TRACE_ID} header field contains the trace id a message, which represents a global and unique
     * identification, and can be used in distributed system to trace the whole call link.
     */
    public static final String TRACE_ID = "TraceId";
}
