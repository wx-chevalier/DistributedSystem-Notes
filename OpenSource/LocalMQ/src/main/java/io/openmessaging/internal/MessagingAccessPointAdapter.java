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
import io.openmessaging.ServiceEndPoint;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * WARN: The current interface prohibits direct access by the end user
 *
 * @version OMS 1.0
 * @since OMS 1.0
 */
public class MessagingAccessPointAdapter {
    private static final String PROTOCOL_NAME = "protocol";
    private static final String SPI_NAME = "spi";
    private static final String URL_NAME = "urls";
    private static final String URL = "url";
    private static final String DEFAULT_SERVICE_END_POINT = "rocketmq";
    private static final String DEFAULT_SERVICE_IMPL = "org.apache.rocketmq.openrelay.impl.ServiceEndPointStandardImpl";
    private static final String URL_SEPARATOR = ":";
    private static final String LIST_SEPARATOR = ",";
    private static final String PARAM_SEPARATOR = "&";
    private static final String KV_SEPARATOR = "=";
    private static Map<String, String> serviceEndPointClassMap = new HashMap<>();

    static {
        serviceEndPointClassMap.put(DEFAULT_SERVICE_END_POINT, DEFAULT_SERVICE_IMPL);
    }

    private static Map<String, List<String>> parseURI(String uri) {
        if (uri == null || uri.length() == 0) {
            return new HashMap<>();
        }

        int spiIndex = 0;
        int index = uri.indexOf(URL_SEPARATOR);
        Map<String, List<String>> results = new HashMap<>();
        String protocol = uri.substring(0, index);
        List<String> protocolSet = new ArrayList<>();
        protocolSet.add(protocol);
        results.put(PROTOCOL_NAME, protocolSet);
        if (index > 0) {
            String spi;
            spiIndex = uri.indexOf(URL_SEPARATOR, index + 1);
            if (spiIndex > 0) {
                spi = uri.substring(index + 1, spiIndex);
            }
            else {
                spi = uri.substring(index + 1);
            }
            List<String> spiSet = new ArrayList<>();
            spiSet.add(spi);
            results.put(SPI_NAME, spiSet);
        }
        if (spiIndex > 0) {
            String urlList = uri.substring(spiIndex + 1);
            String[] list = urlList.split(LIST_SEPARATOR);
            if (list.length > 0) {
                results.put(URL_NAME, Arrays.asList(list));
            }
        }
        return results;
    }

    private static ServiceEndPoint instantiateServiceEndPoint(String driver, KeyValue properties)
        throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
        InvocationTargetException, InstantiationException {
        String serviceImpl = driver;
        if (serviceImpl == null)
            serviceImpl = DEFAULT_SERVICE_IMPL;
        if (serviceEndPointClassMap.containsKey(driver))
            serviceImpl = serviceEndPointClassMap.get(driver);
        Class<?> serviceEndPointClass = Class.forName(serviceImpl);
        if (serviceEndPointClass == null)
            return null;

        if (properties.getString(URL) != null) {
            String[] propertySplits = ((String)properties.getString(URL)).split(PARAM_SEPARATOR);
            if (propertySplits.length > 0) {
                for (int index = 1; index < propertySplits.length; index++) {
                    String[] kv = propertySplits[index].split(KV_SEPARATOR);
                    properties.put(kv[0], kv[1]);
                }
            }
        }
        Class[] paramTypes = {Properties.class};
        Constructor constructor = serviceEndPointClass.getConstructor(paramTypes);
        assert constructor != null;
        return (ServiceEndPoint)constructor.newInstance(properties);
    }

    private static ServiceEndPoint createServiceEndPoint(Map<String, List<String>> url, KeyValue properties)
        throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
        InstantiationException, IllegalAccessException {
        List<String> driver = url.get(SPI_NAME);
        List<String> urls = url.get(URL_NAME);
        Collections.shuffle(urls);
        Collections.shuffle(driver);
        if (urls.size() > 0)
            properties.put(URL, urls.get(0));
        return MessagingAccessPointAdapter.instantiateServiceEndPoint(driver.get(0), properties);
    }

    public static ServiceEndPoint createServiceEndPoint(String url, KeyValue properties)
        throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
        IllegalAccessException, InvocationTargetException {
        Map<String, List<String>> driverUrl = parseURI(url);
        if (null == driverUrl || driverUrl.size() == 0) {
            throw new IllegalArgumentException("driver url parsed result.size ==0");
        }
        return createServiceEndPoint(driverUrl, properties);
    }
}