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

import io.openmessaging.observer.Observer;

/**
 * @author vintagewang@apache.org
 *
 * @version OMS 1.0
 * @since OMS 1.0
 */
public interface ServiceEndPoint extends ServiceLifecycle {
    /**
     * Register/re-register a service in a serviceEndPoint object
     * if service has been registered in serviceEndPoint object, it will be failed when registering delicately
     *
     * @param service the service to publish in serviceEndPoint
     */
    void publish(Object service);

    /**
     * Like {@link #publish(Object)} but specifying {@code properties}
     * that can be used to configure the service published
     *
     * @param service the service to publish in serviceEndPoint
     * @param properties the service published properties
     */

    void publish(Object service, KeyValue properties);

    /**
     * Bind a service object to serviceEndPoint, which can directly call services provided by service object
     *
     * @param type service type to bind in serviceEndPoint
     * @return service proxy object to bind
     */
    <T> T bind(Class<T> type);

    /**
     * Like {@link #bind(Class)} but specifying {@code properties} that can be used to configure the service band
     *
     * @param type service type to bind in serviceEndPoint
     * @param properties the service bind properties
     * @param <T> service proxy object to bind
     * @return service proxy object to bind
     */
    <T> T bind(Class<T> type, KeyValue properties);

    /**
     * Like {@link #bind(Class, KeyValue)} but specifying {@code serviceLoadBalance} that can be used to select
     * endPoint target
     *
     * @param type service type to bind in serviceConsumer
     * @param properties the service band properties
     * @param serviceLoadBalance select endPoint target algorithm
     * @param <T> service proxy object to bind
     * @return service proxy object to bind
     */
    <T> T bind(Class<T> type, KeyValue properties, ServiceLoadBalance serviceLoadBalance);

    /**
     * Register an observer in an serviceEndPoint object. Whenever serviceEndPoint object publish or bind an service
     * object, it will be notified to the list of observer object registered before
     *
     * @param observer observer event object to an serviceEndPoint object
     */
    void addObserver(Observer observer);

    /**
     * Removes the given observer from the list of observer
     * <p>
     * If the given observer has not been previously registered (i.e. it was
     * never added) then this method call is a no-op. If it had been previously
     * added then it will be removed. If it had been added more than once, then
     * only the first occurrence will be removed.
     *
     * @param observer The observer to remove
     */
    void deleteObserver(Observer observer);

    /**
     * @return
     */
    InvokeContext invokeContext();
}
