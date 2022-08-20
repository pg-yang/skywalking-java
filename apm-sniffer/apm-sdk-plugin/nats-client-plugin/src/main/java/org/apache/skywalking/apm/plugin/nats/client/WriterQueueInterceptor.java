/*
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.skywalking.apm.plugin.nats.client;

import io.nats.client.Connection;
import io.nats.client.impl.NatsMessage;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;

import java.lang.reflect.Method;
import java.util.HashMap;

public class WriterQueueInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        NatsMessage message = (NatsMessage) allArguments[0];
        if (!NatsCommons.is(message)) {
            return;
        }
        EnhancedInstance enhancedMsg = (EnhancedInstance) allArguments[0];
        Connection connection = (Connection) objInst.getSkyWalkingDynamicField();
        AbstractSpan span = NatsCommons.createExit(message, connection.getConnectedUrl(), "publish/queue/" + message.getSubject());
        span.prepareForAsync();
        ContextManager.stopSpan(span);
        enhancedMsg.setSkyWalkingDynamicField(span);
        Tags.MQ_QUEUE.set(span, "publish/queue/" + message.getSubject());
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        NatsMessage message = (NatsMessage) allArguments[0];
        if (!NatsCommons.is(message)) {
            return ret;
        }
        if (!(Boolean) ret) {
            EnhancedInstance enhancedMsg = (EnhancedInstance) allArguments[0];
            AbstractSpan span = (AbstractSpan) enhancedMsg.getSkyWalkingDynamicField();
            HashMap<String, String> event = new HashMap<>();
            event.put("queue", "in-queue-err");
            span.errorOccurred().log(System.currentTimeMillis(), event);
            span.asyncFinish();
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        EnhancedInstance enhancedMsg = (EnhancedInstance) allArguments[0];
        AbstractSpan span = (AbstractSpan) enhancedMsg.getSkyWalkingDynamicField();
        span.errorOccurred().log(t);
        span.asyncFinish();
    }
}
