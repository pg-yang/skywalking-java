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

import io.micronaut.http.server.netty.NettyHttpRequest;
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

import java.lang.reflect.Method;

public class HandleStatusErrorInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        NettyHttpRequest<?> request = (NettyHttpRequest<?>) allArguments[1];
        PrintUtil.println(String.format("[Server] receive %s , Thread - > %s", request.getUri(), Thread.currentThread()));
        ContextCarrier contextCarrier = new ContextCarrier();
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            next.setHeadValue(request.getHeaders().get(next.getHeadKey()));
        }
        String operationName = String.join(":", request.getMethod(), request.getPath());
        AbstractSpan span = ContextManager.createEntrySpan(operationName, contextCarrier);
        ContextSnapshot capture = ContextManager.capture();
        request.setAttribute("CORS_SPAN", span);
        request.setAttribute("CORS_SNAPSHOT", capture);
        Tags.URL.set(span, String.format("%s://%s:%s%s", request.isSecure() ? "https" : "http", request.getServerName(), request.getServerAddress().getPort(), request.getUri().getPath()));
        Tags.HTTP.METHOD.set(span, request.getMethodName());
        span.setComponent(ComponentsDefine.MICRONAUT);
        SpanLayer.asHttp(span);
        span.prepareForAsync();
        ContextManager.stopSpan(span);
        if (MicronautHttpServerPluginConfig.Plugin.MicronautHttpServer.COLLECT_HTTP_PARAMS) {
            HttpParamCollector.collectHttpParam(request, span);
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        return null;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {

    }
}
