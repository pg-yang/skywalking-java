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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.plugin.micronaut.http.client;

import io.micronaut.http.client.LoadBalancer;
import io.micronaut.http.client.loadbalance.FixedLoadBalancer;
import io.micronaut.http.client.loadbalance.ServiceInstanceListRoundRobinLoadBalancer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;

import java.net.URI;

public class ClientConstructorInterceptPoint implements InstanceConstructorInterceptor {

    private static final RequestBaseInfo UNKNOWN_TARGET = new RequestBaseInfo("UNKNOWN", "UNKNOWN");


    /*
     * Hold the target base info , it maybe serviceId or domain address( see io.micronaut.http.client.DefaultLoadBalancerResolver#resolve )
     * We construct the info ahead of time for reducing runtime impact in ClientExchangeInterceptor
     */
    @Override
    public void onConstruct(EnhancedInstance enhancedInstance, Object[] objects) throws Throwable {
        LoadBalancer loadBalancer = (LoadBalancer) objects[0];
        if (loadBalancer instanceof FixedLoadBalancer) {
            URI uri = ((FixedLoadBalancer) loadBalancer).getUri();
            enhancedInstance.setSkyWalkingDynamicField(new RequestBaseInfo(uri.getHost() + ":" + uri.getPort(), uri.toString()));
        } else if (loadBalancer instanceof ServiceInstanceListRoundRobinLoadBalancer) {
            ServiceInstanceListRoundRobinLoadBalancer loader = (ServiceInstanceListRoundRobinLoadBalancer) loadBalancer;
            enhancedInstance.setSkyWalkingDynamicField(new RequestBaseInfo(loader.getServiceID(), loader.getServiceID()));
        } else {
            enhancedInstance.setSkyWalkingDynamicField(UNKNOWN_TARGET);
        }
    }
}