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

package org.apache.skywalking.apm.plugin.micronaut.http.server;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;

public class ServerWriteAndFlushNettyInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(final EnhancedInstance objInst,
                             final Method method,
                             final Object[] allArguments,
                             final Class<?>[] argumentsTypes,
                             final MethodInterceptResult result) throws Throwable {

        Publisher<MutableHttpResponse<?>> publisher = (Publisher<MutableHttpResponse<?>>) allArguments[2];
        ContextSnapshot capture = ContextManager.capture();
        AbstractSpan span = ContextManager.activeSpan();

        Publisher<MutableHttpResponse<?>> newArg = Flux.from(publisher)
                .contextWrite(content -> content.put("content_micronaut", capture))
                .doOnNext(res -> {
                    int code = res.status().getCode();
                    Tags.HTTP_RESPONSE_STATUS_CODE.set(span, code);
                    if (code >= 400) {
                        span.errorOccurred();
                    }
                    if (!MicronautHttpServerPluginConfig.Plugin.MicronautHttpServer.COLLECT_HTTP_PARAMS && span.isProfiling()) {
                        HttpParamCollector.collectHttpParam((HttpRequest<?>) allArguments[1], span);
                    }
                    ContextManager.stopSpan();
                });
        allArguments[2] = newArg;
    }

    @Override
    public Object afterMethod(final EnhancedInstance objInst,
                              final Method method,
                              final Object[] allArguments,
                              final Class<?>[] argumentsTypes,
                              final Object ret) throws Throwable {
        return ret;
    }

    @Override
    public void handleMethodException(final EnhancedInstance objInst,
                                      final Method method,
                                      final Object[] allArguments,
                                      final Class<?>[] argumentsTypes,
                                      final Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }

}
