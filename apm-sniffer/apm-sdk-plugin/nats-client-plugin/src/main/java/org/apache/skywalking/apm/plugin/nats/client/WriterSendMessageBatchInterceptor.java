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

import io.nats.client.impl.NatsMessage;
import org.apache.skywalking.apm.agent.core.context.AsyncSpan;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

public class WriterSendMessageBatchInterceptor implements InstanceMethodsAroundInterceptor {

    private static final Field NEXT_FIELD;

    static {
        Field field;
        try {
            field = NatsMessage.class.getDeclaredField("next");
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            field = null;
        }
        NEXT_FIELD = field;
    }

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

    }

    // Our trace begin with in-queue , and end with flush . It's flush
    // Reference to org.apache.skywalking.apm.plugin.nats.client.define.NatsMessageInstrumentation
    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        NatsMessage msgIterator = (NatsMessage) allArguments[0];
        if (!NatsCommons.is(msgIterator)) {
            return ret;
        }
        while (msgIterator != null) {
            EnhancedInstance enhancedInstance = (EnhancedInstance) msgIterator;
            Optional.ofNullable((AbstractSpan) enhancedInstance.getSkyWalkingDynamicField())
                    .ifPresent(AsyncSpan::asyncFinish);
            msgIterator = nextMsg(msgIterator);
        }
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        NatsMessage msgIterator = (NatsMessage) allArguments[0];
        if (!NatsCommons.is(msgIterator)) {
            return;
        }
        while (msgIterator != null) {
            EnhancedInstance enhancedInstance = (EnhancedInstance) msgIterator;
            Optional.ofNullable((AbstractSpan) enhancedInstance.getSkyWalkingDynamicField())
                    .ifPresent(span -> span.errorOccurred().log(t).asyncFinish());
            try {
                msgIterator = nextMsg(msgIterator);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private NatsMessage nextMsg(NatsMessage message) throws IllegalAccessException {
        if (NEXT_FIELD == null) {
            return null;
        }
        return (NatsMessage) NEXT_FIELD.get(message);

    }
}
