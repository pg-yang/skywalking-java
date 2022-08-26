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

package org.apache.skywalking.apm.plugin.nats.client.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.plugin.nats.client.NatsClientPluginConfig;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

public class NatsConnectionWriterInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    private static final String ENHANCE_CLASS = "io.nats.client.impl.NatsConnectionWriter";
    private static final String PUBLISH_INTERCEPTOR_CLASS_NAME = "org.apache.skywalking.apm.plugin.nats.client.WriterSendMessageBatchInterceptor";
    private static final String CONSTRUCTOR_INTERCEPTOR = "org.apache.skywalking.apm.plugin.nats.client.WriterConstructorInterceptor";
    private static final String QUEUE_INTERCEPTOR_CLASS_NAME = "org.apache.skywalking.apm.plugin.nats.client.WriterQueueInterceptor";

    private static final InstanceMethodsInterceptPoint QUEUE_INTERCEPTOR = new InstanceMethodsInterceptPoint() {
        @Override
        public ElementMatcher<MethodDescription> getMethodsMatcher() {
            return named("queue").and(takesArguments(1));
        }

        @Override
        public String getMethodsInterceptor() {
            return QUEUE_INTERCEPTOR_CLASS_NAME;
        }

        @Override
        public boolean isOverrideArgs() {
            return false;
        }
    };

    private static final InstanceMethodsInterceptPoint SEND_MSG_INTERCEPTOR = new InstanceMethodsInterceptPoint() {
        @Override
        public ElementMatcher<MethodDescription> getMethodsMatcher() {
            return named("sendMessageBatch").and(takesArguments(3));
        }

        @Override
        public String getMethodsInterceptor() {
            return PUBLISH_INTERCEPTOR_CLASS_NAME;
        }

        @Override
        public boolean isOverrideArgs() {
            return true;
        }
    };

    @Override
    protected ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS);
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[]{
                new ConstructorInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getConstructorMatcher() {
                        return takesArguments(1);
                    }

                    @Override
                    public String getConstructorInterceptor() {
                        return CONSTRUCTOR_INTERCEPTOR;
                    }
                }
        };
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        if (NatsClientPluginConfig.Plugin.NatsClient.ENABLE_FULL_TRACE) {
            return new InstanceMethodsInterceptPoint[]{QUEUE_INTERCEPTOR, SEND_MSG_INTERCEPTOR};
        }
        return new InstanceMethodsInterceptPoint[]{QUEUE_INTERCEPTOR};
    }

}
