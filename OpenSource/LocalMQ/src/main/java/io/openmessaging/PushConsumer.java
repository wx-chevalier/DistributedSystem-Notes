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

import io.openmessaging.exception.OMSResourceNotExistException;
import io.openmessaging.exception.OMSRuntimeException;

/**
 * A {@code PushConsumer} object to receive messages from multiple queue, these messages are pushed from
 * MOM server to {@code PushConsumer} client.
 *
 * @author vintagewang@apache.org
 * @author yukon@apache.org
 *
 * @version OMS 1.0
 * @since OMS 1.0
 * @see MessagingAccessPoint#createPushConsumer()
 */
public interface PushConsumer extends ServiceLifecycle {
    /**
     * Resumes the {@code PushConsumer} after a suspend.
     * <p>
     * This method resumes the {@code PushConsumer} instance after it was suspended.
     * The instance will not receive new messages between the suspend and resume calls.
     *
     * @throws OMSRuntimeException if the instance has not been suspended.
     * @see PushConsumer#suspend()
     */
    void resume();

    /**
     * Suspends the {@code PushConsumer} for later resumption.
     * <p>
     * This method suspends the {@code PushConsumer} instance until it is resumed.
     * The instance will not receive new messages between the suspend and resume calls.
     *
     * @throws OMSRuntimeException if the instance is not currently running.
     * @see PushConsumer#resume()
     */
    void suspend();

    /**
     * This method is used to find out whether the {@code PushConsumer} is suspended.
     *
     * @return true if this {@code PushConsumer} is suspended, false otherwise
     */
    boolean isSuspended();

    /**
     * Returns the properties of this {@code PushConsumer} instance.
     * Changes to the return {@code KeyValue} are not reflected in physical {@code PushConsumer},
     * and use {@link ResourceManager#setConsumerProperties(String, KeyValue)} to modify.
     *
     * @return the properties
     */
    KeyValue properties();

    /**
     * Attaches the {@code PushConsumer} to a specified queue, with a {@code MessageListener}.
     * {@link MessageListener#onMessage(Message, OnMessageContext)} will be called when new
     * delivered message is coming.
     *
     * @param queueName a specified queue
     * @param listener a specified listener to receive new message
     *
     * @throws OMSResourceNotExistException if the specified queue is not exists
     */
    PushConsumer attachQueue(String queueName, MessageListener listener) throws OMSResourceNotExistException;
}