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

package org.apache.skywalking.apm.plugin.jedis.v4.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.apache.skywalking.apm.plugin.jedis.commons.RedisMethodMatch;

import static org.apache.skywalking.apm.agent.core.plugin.bytebuddy.ArgumentTypeNameMatch.takesArgumentWithType;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

public class PipelineInstrumentation extends AbstractWitnessInstrumentation {

    private static final String ENHANCE_CLASS = "redis.clients.jedis.Pipeline";
    private static final String CONNECTION_CONSTRUCTOR_INTERCEPT_CLASS = "org.apache.skywalking.apm.plugin.jedis.v4.PipelineConnectionConstructorInterceptor";
    private static final String JEDIS_CONSTRUCTOR_INTERCEPT_CLASS = "org.apache.skywalking.apm.plugin.jedis.v4.PipelineJedisConstructorInterceptor";
    private static final String JEDIS_METHOD_INTERCEPTOR = "org.apache.skywalking.apm.plugin.jedis.commons.interceptor.JedisMethodInterceptor";

    @Override
    public ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS);
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[]{
                new ConstructorInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getConstructorMatcher() {
                        return takesArgumentWithType(0, "redis.clients.jedis.Connection");
                    }

                    @Override
                    public String getConstructorInterceptor() {
                        return CONNECTION_CONSTRUCTOR_INTERCEPT_CLASS;
                    }
                },
                new ConstructorInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getConstructorMatcher() {
                        return takesArgumentWithType(0, "redis.clients.jedis.Jedis");
                    }

                    @Override
                    public String getConstructorInterceptor() {
                        return JEDIS_CONSTRUCTOR_INTERCEPT_CLASS;
                    }
                }
        };
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[]{
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return RedisMethodMatch.INSTANCE.getJedisMethodMatcher();
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return JEDIS_METHOD_INTERCEPTOR;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                }
        };
    }
}
