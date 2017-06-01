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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * The {@code ServiceLifecycle} defines a lifecycle interface for a OMS related service endpoint, like {@link Producer},
 * {@link PushConsumer}, and so on.
 * <p>
 * If the service endpoint class implements the {@code ServiceLifecycle} interface, most of the containers can manage
 * the lifecycle of the corresponding service endpoint objects easily.
 * <p>
 * Any service endpoint should support repeated restart if it implements the {@code ServiceLifecycle} interface.
 *
 * @author vintagewang@apache.org
 * @author yukon@apache.org
 *
 * @version OMS 1.0
 * @since OMS 1.0
 */
public interface ServiceLifecycle {
    /**
     * Used for start or initialization of a service endpoint. A service endpoint instance will be in a ready state
     * after this method has been completed.
     */
    @PostConstruct
    void start();

    /**
     * Notify a service instance of the end of its life cycle. Once this method completes, the service endpoint could be
     * destroyed and eligible for garbage collection.
     */
    @PreDestroy
    void shutdown();
}
