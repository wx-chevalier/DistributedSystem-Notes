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

package io.openmessaging.internal;

import io.openmessaging.KeyValue;
import java.util.Set;

/**
 * WARN: The current interface prohibits direct access by the end user
 *
 * @version OMS 1.0
 * @since OMS 1.0
 */
public class DefaultKeyValue implements KeyValue {
    @Override
    public KeyValue put(String key, int value) {
        return null;
    }

    @Override
    public KeyValue put(String key, long value) {
        return null;
    }

    @Override
    public KeyValue put(String key, double value) {
        return null;
    }

    @Override
    public KeyValue put(String key, String value) {
        return null;
    }

    @Override
    public int getInt(String key) {
        return 0;
    }

    @Override
    public long getLong(String key) {
        return 0;
    }

    @Override
    public double getDouble(String key) {
        return 0;
    }

    @Override
    public String getString(String key) {
        return null;
    }

    @Override
    public Set<String> keySet() {
        return null;
    }

    @Override
    public boolean containsKey(String key) {
        return false;
    }
}
