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
import java.util.List;

/**
 * The {@code ResourceManager} is responsible for providing a unified interface of resource management,
 * allows the user to manage the topic, queue, namespace resources.
 * <p>
 * Create, fetch, update and destroy are the four basic functions of {@code ResourceManager}.
 * <p>
 * And the {@code ResourceManager} also supports fetch and update resource properties dynamically.
 * <p>
 * The properties of consumer and producer also are treated as {@code Resource}. {@code ResourceManager}
 * allows the user to fetch producer and consumer list in a specified topic or queue,
 * and update their resource properties dynamically.
 * <p>
 * {@link MessagingAccessPoint#createResourceManager()} is the unique method to obtain a {@code ResourceManager}
 * instance, any changes made by this instance will reflect to the message-oriented middleware (MOM) or
 * other product behind the {@code MessagingAccessPoint}.
 *
 * @author vintagewang@apache.org
 * @author yukon@apache.org
 *
 * @version OMS 1.0
 * @since OMS 1.0
 */
public interface ResourceManager extends ServiceLifecycle {
    /**
     * Creates a {@code Namespace} resource for the specified {@code MessagingAccessPoint} with some preset properties,
     * updates if it already exists.
     * <p>
     * Note that this method will simply create the physical Namespace in the specified {@code MessagingAccessPoint}.
     *
     * @param nsName a namespace name
     * @param properties the preset properties
     */
    void createAndUpdateNamespace(String nsName, KeyValue properties);

    /**
     * Creates a {@code Topic} resource for the specified {@code MessagingAccessPoint} with some preset properties,
     * updates if it already exists.
     * <p>
     * Note that this method will simply create the physical topic in the specified {@code MessagingAccessPoint}.
     *
     * @param topicName a topic name
     * @param properties the preset properties
     */
    void createAndUpdateTopic(String topicName, KeyValue properties);

    /**
     * Creates a {@code Queue} resource for the specified {@code MessagingAccessPoint} with some preset properties,
     * updates if it already exists.
     * <p>
     * Note that this method will simply create the physical queue in the specified {@code MessagingAccessPoint}.
     *
     * @param queueName a queue name
     * @param filter a specified filter
     * @param properties the preset properties
     */
    void createAndUpdateQueue(String queueName, Filters filter, KeyValue properties);

    /**
     * Destroys a physical namespace in the specified {@code MessagingAccessPoint}.
     * <p>
     * All this namespace related physical resources may be deleted immediately.
     * @param nsName a namespace name to be destroyed
     */
    void destroyNamespace(String nsName);

    /**
     * Destroys a physical topic in the specified {@code MessagingAccessPoint}.
     * <p>
     * All this topic related physical resources may be deleted immediately.
     * @param topicName a namespace name to be destroyed
     */
    void destroyTopic(String topicName);

    /**
     * Destroys a physical queue in the specified {@code MessagingAccessPoint}.
     * <p>
     * All this queue related physical resources may be deleted immediately.
     * @param queueName a namespace name to be destroyed
     */
    void destroyQueue(String queueName);

    /**
     * Fetches the resource properties of a specified namespace.
     *
     * @param nsName a namespace name
     * @return the properties of this specified namespace
     * @throws OMSResourceNotExistException if the specified namespace is not exists
     */
    KeyValue getNamespaceProperties(String nsName) throws OMSResourceNotExistException;

    /**
     * Fetches the resource properties of a specified topic.
     *
     * @param topicName a topic name
     * @return the properties of this specified topic
     * @throws OMSResourceNotExistException if the specified topic is not exists
     */
    KeyValue getTopicProperties(String topicName) throws OMSResourceNotExistException;

    /**
     * Fetches the resource properties of a specified queue.
     *
     * @param queueName a queue name
     * @return the properties of this specified queue
     * @throws OMSResourceNotExistException if the specified queue is not exists
     */
    KeyValue getQueueProperties(String queueName) throws OMSResourceNotExistException;

    /**
     * Each consumer has a unique id, this method is to fetch the consumer id list in a specified queue.
     *
     * @param queueName a queue name
     * @return the consumer id list in this queue
     * @throws OMSResourceNotExistException if the specified queue is not exists
     */
    List<String> consumerIdListInQueue(String queueName) throws OMSResourceNotExistException;

    /**
     * Returns the properties of the specified consumer instance with the given consumer id.
     * If no such consumer id exists, {@code OMSResourceNotExistException} will be thrown.
     *
     * @param consumerId The unique consumer id for an consumer instance
     * @return the properties of the matching consumer instance
     * @throws OMSResourceNotExistException if the specified consumer is not exists
     */
    KeyValue getConsumerProperties(String consumerId) throws OMSResourceNotExistException;

    /**
     * Sets the consumer properties to the specified consumer instance.
     * <p>
     * The new {@code KeyValue} becomes the current set of consumer properties, and the {@link
     * ResourceManager#getConsumerProperties(String)} will observe this change soon. If the argument is null, then the
     * current set of consumer properties will stay the same.
     *
     * @param consumerId the specified consumer id
     * @param properties the new consumer properties
     * @throws OMSResourceNotExistException if the specified consumer is not exists
     */
    void setConsumerProperties(String consumerId, KeyValue properties) throws OMSResourceNotExistException;

    /**
     * Each producer has a unique id, this method is to fetch the producer id list in a specified queue.
     *
     * @param queueName a queue name
     * @return the producer id list in this queue
     * @throws OMSResourceNotExistException if the specified queue is not exists
     */
    List<String> producerIdListInQueue(String queueName) throws OMSResourceNotExistException;

    /**
     * Each producer has a unique id, this method is to fetch the producer id list in a specified topic.
     *
     * @param topicName a topic name
     * @return the producer id list in this topic
     * @throws OMSResourceNotExistException if the specified topic is not exists
     */
    List<String> producerIdListInTopic(String topicName) throws OMSResourceNotExistException;

    /**
     * Returns the properties of the specified producer instance with the given producer id.
     * If no such producer id exists, {@code OMSResourceNotExistException} will be thrown.
     *
     * @param producerId The unique consumer id for an producer instance
     * @return the properties of the matching producer instance
     * @throws OMSResourceNotExistException if the specified producer is not exists
     */
    KeyValue getProducerProperties(String producerId) throws OMSResourceNotExistException;

    /**
     * Sets the producer properties to the specified producer instance.
     * <p>
     * The new {@code KeyValue} becomes the current set of producer properties, and the {@link
     * ResourceManager#getProducerProperties(String)} will observe this change soon. If the argument is null, then the
     * current set of producer properties will stay the same.
     *
     * @param producerId the specified producer id
     * @param properties the new producer properties
     * @throws OMSResourceNotExistException if the specified producer is not exists
     */
    void setProducerProperties(String producerId, KeyValue properties) throws OMSResourceNotExistException;

}
