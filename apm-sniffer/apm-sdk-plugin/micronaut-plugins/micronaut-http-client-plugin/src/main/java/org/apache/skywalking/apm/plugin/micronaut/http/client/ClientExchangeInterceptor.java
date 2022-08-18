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
import io.micronaut.http.context.ServerRequestContext;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.util.StringUtil;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;

public class ClientExchangeInterceptor implements InstanceMethodsAroundInterceptor {

    private static final String ASYNC_SPAN_KEY = "ASYNC-SPAN";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        MutableHttpRequest<?> request = (MutableHttpRequest<?>) allArguments[0];
        PrintUtil.println(String.format("[Client] start request %s ", request.getPath()));
        String requestMethod = request.getMethod().name();
        RequestBaseInfo requestBaseInfo = (RequestBaseInfo) objInst.getSkyWalkingDynamicField();
        AbstractSpan span = ContextManager.createExitSpan(requestMethod + ":" + request.getPath(), requestBaseInfo.getPeer());
        ServerRequestContext.currentRequest().flatMap(req -> req.getAttribute("CORS_SNAPSHOT")).ifPresent(e -> ContextManager.continued((ContextSnapshot) e));
        final ContextCarrier contextCarrier = new ContextCarrier();
        ContextManager.inject(contextCarrier);
        span.setComponent(ComponentsDefine.MICRONAUT);
        Tags.HTTP.METHOD.set(span, requestMethod);
        Tags.URL.set(span, request.getPath().equals("/") ? requestBaseInfo.getBaseUrl() : requestBaseInfo.getBaseUrl() + request.getPath());
        SpanLayer.asHttp(span);
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            request.header(next.getHeadKey(), next.getHeadValue());
        }
        span.prepareForAsync();
        ContextManager.stopSpan(span);
        request.setAttribute(ASYNC_SPAN_KEY, span);
        if (MicronautHttpClientPluginConfig.Plugin.MicronautHttpClient.COLLECT_HTTP_PARAMS) {
            collectHttpParam(request, span);
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        MutableHttpRequest<?> request = (MutableHttpRequest<?>) allArguments[0];
        Publisher<HttpResponse<?>> retPublisher = (Publisher<HttpResponse<?>>) ret;
        return Flux.from(retPublisher).doOnError(ex -> finishAndCleanup(request, ex))
                .doOnNext(resp -> {
                    PrintUtil.println(String.format("[Client] end request %s ", request.getPath()));
                    request.getAttribute(ASYNC_SPAN_KEY)
                            .map(span -> (AbstractSpan) span)
                            .ifPresent(span -> {
                                Tags.HTTP_RESPONSE_STATUS_CODE.set(span, resp.code());
                                if (resp.code() >= 400) {
                                    span.errorOccurred();
                                }
                                span.asyncFinish();
                            });
                    request.removeAttribute(ASYNC_SPAN_KEY, AbstractSpan.class);
                });
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        MutableHttpRequest<?> request = (MutableHttpRequest<?>) allArguments[0];
        request.getAttribute(ASYNC_SPAN_KEY)
                .map(span -> (AbstractSpan) span)
                .ifPresent(span -> span.errorOccurred().log(t));
    }

    private void collectHttpParam(MutableHttpRequest<?> httpRequest, AbstractSpan span) {
        String tag = httpRequest.getUri().getQuery();
        tag = MicronautHttpClientPluginConfig.Plugin.Http.HTTP_PARAMS_LENGTH_THRESHOLD > 0 ?
                StringUtil.cut(tag, MicronautHttpClientPluginConfig.Plugin.Http.HTTP_PARAMS_LENGTH_THRESHOLD) : tag;
        if (StringUtil.isNotEmpty(tag)) {
            Tags.HTTP.PARAMS.set(span, tag);
        }
    }

    private void finishAndCleanup(MutableHttpRequest<?> request, Throwable ex) {
        PrintUtil.println(String.format("[Client] end request error %s ", request.getPath()));
        request.getAttribute(ASYNC_SPAN_KEY)
                .map(span -> (AbstractSpan) span)
                .ifPresent(span -> {
                    AbstractSpan abstractSpan = span.errorOccurred();
                    if (ex != null) {
                        abstractSpan.log(ex);
                    }
                    abstractSpan.asyncFinish();
                });
        request.removeAttribute(ASYNC_SPAN_KEY, AbstractSpan.class);
    }

}
