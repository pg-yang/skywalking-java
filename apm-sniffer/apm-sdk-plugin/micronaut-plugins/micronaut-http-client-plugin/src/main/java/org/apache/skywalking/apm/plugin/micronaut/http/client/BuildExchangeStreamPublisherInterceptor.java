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

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.reactivestreams.Publisher;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.function.Function;

public class BuildExchangeStreamPublisherInterceptor implements InstanceMethodsAroundInterceptor {
    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        MutableHttpRequest<?> request = (MutableHttpRequest) allArguments[1];
        Function<URI, Publisher<? extends HttpResponse<?>>> function = (Function<URI, Publisher<? extends HttpResponse<?>>>) ret;
        return (Function<URI, Publisher<? extends HttpResponse<?>>>) uri -> {
            MicronautCommons.startTrace(request, uri);
            return MicronautCommons.buildTracePublisher(request, function.apply(uri));
        };
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        MutableHttpRequest<?> request = (MutableHttpRequest) allArguments[1];
        MicronautCommons.finishOnAction(request, span -> span.log(t).errorOccurred());
    }
}
