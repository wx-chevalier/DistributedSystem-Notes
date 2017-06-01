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
 * The {@code Message} interface is the root interface of all OMS messages, and the most commonly used OMS message is
 * {@link BytesMessage}.
 * <p>
 * Most message-oriented middleware (MOM) products treat messages as lightweight entities that consist of a header and a
 * body, like <a href="http://rocketmq.apache.org/">Apache RocketMQ</a>. The header contains fields used for message
 * routing and identification; the body contains the application data being sent.
 * <p>
 * The {@code Message} is a lightweight status that only contains the property related information of a specific message
 * object, and the {@code Message} is composed of the following parts:
 *
 * <UL>
 * <LI>Header - All messages support the same set of header fields. Header fields contain values used by both
 * clients and providers to identify and route messages.
 * <LI>Properties - Each message contains a built-in facility for supporting application-defined property values.
 * Properties provide an efficient mechanism for supporting application-defined message filtering.
 * </UL>
 *
 * The body part is placed in the implementation classes of {@code Message}.
 *
 * @author vintagewang@apache.org
 * @author yukon@apache.org
 *
 * @version OMS 1.0
 * @since OMS 1.0
 */
public interface Message {
    /**
     * Returns all the header fields of the {@code Message} object as a {@code KeyValue}.
     *
     * @return the headers of a {@code Message}
     * @see MessageHeader
     */
    KeyValue headers();

    /**
     * Returns all the built-in property fields of the {@code Message} object as a {@code KeyValue}.
     *
     * @return the properties of a {@code Message}
     */
    KeyValue properties();

    /**
     * Puts a {@code String}-{@code int} {@code KeyValue} entry to he headers of a {@code Message}.
     *
     * @param key  the key to be placed into the headers
     * @param value the value corresponding to <tt>key</tt>
     */
    Message putHeaders(String key, int value);

    /**
     * Puts a {@code String}-{@code long} {@code KeyValue} entry to he headers of a {@code Message}.
     *
     * @param key  the key to be placed into the headers
     * @param value the value corresponding to <tt>key</tt>
     */
    Message putHeaders(String key, long value);

    /**
     * Puts a {@code String}-{@code double} {@code KeyValue} entry to he headers of a {@code Message}.
     *
     * @param key  the key to be placed into the headers
     * @param value the value corresponding to <tt>key</tt>
     */
    Message putHeaders(String key, double value);

    /**
     * Puts a {@code String}-{@code String} {@code KeyValue} entry to he headers of a {@code Message}.
     *
     * @param key  the key to be placed into the headers
     * @param value the value corresponding to <tt>key</tt>
     */
    Message putHeaders(String key, String value);

    /**
     * Puts a {@code String}-{@code int} {@code KeyValue} entry to he headers of a {@code Message}.
     *
     * @param key  the key to be placed into the headers
     * @param value the value corresponding to <tt>key</tt>
     */
    Message putProperties(String key, int value);

    /**
     * Puts a {@code String}-{@code long} {@code KeyValue} entry to he headers of a {@code Message}.
     *
     * @param key  the key to be placed into the headers
     * @param value the value corresponding to <tt>key</tt>
     */
    Message putProperties(String key, long value);

    /**
     * Puts a {@code String}-{@code double} {@code KeyValue} entry to he headers of a {@code Message}.
     *
     * @param key  the key to be placed into the headers
     * @param value the value corresponding to <tt>key</tt>
     */
    Message putProperties(String key, double value);

    /**
     * Puts a {@code String}-{@code String} {@code KeyValue} entry to he headers of a {@code Message}.
     *
     * @param key  the key to be placed into the headers
     * @param value the value corresponding to <tt>key</tt>
     */
    Message putProperties(String key, String value);
}