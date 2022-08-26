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

package org.apache.skywalking.apm.plugin.nats.client;

import org.apache.skywalking.apm.agent.core.boot.PluginConfig;

public class NatsClientPluginConfig {

    public static class Plugin {
        @PluginConfig(root = NatsClientPluginConfig.class)
        public static class NatsClient {
            /**
             * Nats publish message asynchronously , it put message to local queue ,
             * then write message to network by call flush method in another thread .
             * This config term indicate whether collect complete trace .
             * If set to true ,the plugin will trace enqueue , flush . Otherwise, only enqueue
             * Notice , If set true . will generate a lot of Span (one span for a message). These spans are not released util call flush
             */
            public static boolean ENABLE_FULL_TRACE = false;
        }
    }
}
