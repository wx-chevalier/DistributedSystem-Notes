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

import io.openmessaging.exception.OMSRuntimeException;
import java.util.Collection;

/**
 * A {@code PullConsumer} object can poll messages from the specified queue,
 * and supports submit the consume result by acknowledgement.
 *
 * @author vintagewang@apache.org
 * @author yukon@apache.org
 * @version OMS 1.0
 * @see MessagingAccessPoint#createPullConsumer(String)
 * @since OMS 1.0
 */
public interface PullConsumer {
    /**
     * Returns the properties of this {@code PullConsumer} instance.
     * Changes to the return {@code KeyValue} are not reflected in physical {@code PullConsumer},
     * and use {@link ResourceManager#setConsumerProperties(String, KeyValue)} to modify.
     *
     * @return the properties
     */
    KeyValue properties();

    /**
     * Polls the next message produced for this {@code PullConsumer}.
     * <p>
     * This call blocks indefinitely until a message is produced or until this {@code PullConsumer} is shut down.
     *
     * @return the next message produced for this {@code PullConsumer}, or null if this {@code PullConsumer} is
     * concurrently shut down
     * @throws OMSRuntimeException if this {@code PullConsumer} fails to pull the next message due to some internal
     * error.
     */
    /**
     * 规范要求实现阻塞的接口，由properties来设置阻塞时间，但本赛题不需要用到该特性，请实现一个非阻塞(也即阻塞时间为0)调用, 也即没有消息则返回null
     * @return
     */
    Message poll();

    /**
     * Polls the next message produced for this {@code PullConsumer}, using the specified properties.
     * <p>
     * This call blocks indefinitely until a message is produced or until this {@code PullConsumer} is shut down.
     *
     * @param properties the specified properties
     * @return the next message produced for this {@code PullConsumer}, or null if this {@code PullConsumer} is
     * concurrently shut down
     * @throws OMSRuntimeException if this {@code PullConsumer} fails to pull the next message due to some internal
     * error.
     */
    Message poll(final KeyValue properties);

    /**
     * Acknowledges the specified and consumed message, with unique message id.
     * <p>
     * Messages that have been received but not acknowledged may be redelivered.
     *
     * @throws OMSRuntimeException if the consumer fails to acknowledge the messages due to some internal error.
     */
    void ack(String messageId);

    /**
     * Acknowledges the specified and consumed message with the specified properties.
     * <p>
     * Messages that have been received but not acknowledged may be redelivered.
     *
     * @throws OMSRuntimeException if the consumer fails to acknowledge the messages due to some internal error.
     */
    void ack(String messageId, final KeyValue properties);

    /**
     * 绑定到一个Queue，并订阅topics，即从这些topic读取消息
     * @param queueName
     * @param topics
     */
    void attachQueue(String queueName, Collection<String> topics);
}