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

import java.util.Set;

/**
 * @author vintagewang@apache.org
 *
 * @version OMS 1.0
 * @since OMS 1.0
 */
public interface ServiceLoadBalance {
    /**
     * Select a collection of eligible providerServicePoint object from the the list of providerServicePoint provided
     * According to different selection strategies to select providerServicePoint that satisfied with application needs,
     * such as RoundRobin or Random etc.
     *
     * @param servicePropertiesList providerServicePoint to choose from.
     * @return a collection of eligible providerServicePoint object
     */
    Set<ServiceProperties> select(Set<ServiceProperties> servicePropertiesList);

}
