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

import java.util.List;

/**
 * A {@code Queue} may have multiple partitions, each partition supports streaming consumption.
 * <p>
 * A {@code PartitionConsumer} object supports consume messages from all the partitions of a
 * specified queue by streaming way.
 *
 * @author vintagewang@apache.org
 * @author yukon@apache.org
 * @version OMS 1.0
 * @see MessagingAccessPoint#createPartitionConsumer(String)
 * @since OMS 1.0
 */
public interface PartitionConsumer {
    /**
     * Returns the properties of this {@code PartitionConsumer} instance.
     * Changes to the return {@code KeyValue} are not reflected in physical {@code PartitionConsumer},
     * and use {@link ResourceManager#setConsumerProperties(String, KeyValue)} to modify.
     *
     * @return the properties
     */
    KeyValue properties();

    /**
     * Fetches the partition list of the specified queue, which related to this {@code PartitionConsumer}
     *
     * @return the partition list of queue
     */
    List<String> partitionList();

    /**
     * Creates a {@code PartitionIterator} from the specified partition.
     * <p>
     * If the specified partition doesn't exist, create it automatically.
     *
     * @param partitionName the specified partition name
     * @return the created {@code PartitionIterator}
     */
    PartitionIterator partitionIterator(String partitionName);
}