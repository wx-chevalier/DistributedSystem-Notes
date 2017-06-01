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

/**
 * A {@code Producer} is a simple object used to send messages on behalf
 * of a {@code MessagingAccessPoint}. An instance of {@code Producer} is
 * created by calling the {@link MessagingAccessPoint#createProducer()} method.
 * It provides various {@code send} methods to send a message to a specified destination.
 * A destination can be a {@link MessageHeader#TOPIC} or a {@link MessageHeader#QUEUE}.
 * <p>
 *
 * {@link Producer#send(Message)} means send a message to destination synchronously,
 * the calling thread will block until the send request complete.
 * <p>
 * {@link Producer#sendAsync(Message)} means send a message to destination asynchronously,
 * the calling thread won't block and will return immediately. Since the send call is asynchronous
 * it returns a {@link Promise} for the send result.
 * <p>
 * {@link Producer#sendOneway(Message)} means send a message to destination in one way,
 * the calling thread won't block and will return immediately. The caller won't care about
 * the send result, while the server has no responsibility for returning the result.
 *
 * @author vintagewang@apache.org
 * @author yukon@apache.org
 *
 * @version OMS 1.0
 * @since OMS 1.0
 */
public interface Producer extends MessageFactory, ServiceLifecycle {
    /**
     * Returns the properties of this {@code Producer} instance.
     * Changes to the return {@code KeyValue} are not reflected in physical {@code Producer},
     * and use {@link ResourceManager#setProducerProperties(String, KeyValue)} to modify.
     *
     * @return the properties
     */
    KeyValue properties();

    /**
     * Sends a message to the specified destination synchronously, the destination should be preset to
     * {@link MessageHeader}, other header fields as well.
     *
     * @param message a message will be sent
     * @throws OMSRuntimeException if the {@code Producer} fails to send the message due to some internal error.
     */
    void send(Message message);

    /**
     * Sends a message to the specified destination synchronously, using the specified properties, the destination
     * should be preset to {@link MessageHeader}, other header fields as well.
     *
     * @param message a message will be sent
     * @param properties the specified properties
     * @throws OMSRuntimeException if the {@code Producer} fails to send the message due to some internal error.
     */
    void send(Message message, KeyValue properties);

    /**
     * Sends a message to the specified destination asynchronously, the destination should be preset to
     * {@link MessageHeader}, other header fields as well.
     * <p>
     * The returned {@code Promise} will have the result once the operation completes, and the registered
     * {@code PromiseListener} will be notified, either because the operation was successful or because of an error.
     *
     * @param message a message will be sent
     * @return the {@code Promise} of an asynchronous message send operation.
     * @see Promise
     * @see PromiseListener
     */
    Promise<Void> sendAsync(Message message);

    /**
     * Sends a message to the specified destination asynchronously, using the specified properties, the destination
     * should be preset to {@link MessageHeader}, other header fields as well.
     * <p>
     * The returned {@code Promise} will have the result once the operation completes, and the registered
     * {@code PromiseListener} will be notified, either because the operation was successful or because of an error.
     *
     * @param message a message will be sent
     * @param properties the specified properties
     * @return the {@code Promise} of an asynchronous message send operation.
     * @see Promise
     * @see PromiseListener
     */
    Promise<Void> sendAsync(Message message, KeyValue properties);

    /**
     * Sends a message to the specified destination in one way, the destination should be preset to
     * {@link MessageHeader}, other header fields as well.
     * <p>
     * There is no {@code Promise} related or {@code RuntimeException} thrown. The calling thread doesn't
     * care about the send result and also have no context to get the result.
     *
     * @param message a message will be sent
     */
    void sendOneway(Message message);

    /**
     * Sends a message to the specified destination in one way, using the specified properties, the destination
     * should be preset to {@link MessageHeader}, other header fields as well.
     * <p>
     * There is no {@code Promise} related or {@code RuntimeException} thrown. The calling thread doesn't
     * care about the send result and also have no context to get the result.
     *
     * @param message a message will be sent
     * @param properties the specified properties
     */
    void sendOneway(Message message, KeyValue properties);

    BatchToPartition createBatchToPartition(String partitionName);

    BatchToPartition createBatchToPartition(String partitionName, KeyValue properties);

    void flush();
}