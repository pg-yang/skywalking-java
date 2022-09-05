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

package org.apache.skywalking.apm.plugin.jedis.v3;

import org.apache.skywalking.apm.agent.core.boot.PluginConfig;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class JedisPluginConfig {
    public static class Plugin {
        @PluginConfig(root = JedisPluginConfig.class)
        public static class Jedis {
            /**
             * If set to true, the parameters of the Redis command would be collected.
             */
            public static boolean TRACE_REDIS_PARAMETERS = true;
            /**
             * For the sake of performance, SkyWalking won't save Redis parameter string into the tag.
             * If TRACE_REDIS_PARAMETERS is set to true, the first {@code REDIS_PARAMETER_MAX_LENGTH} parameter
             * characters would be collected.
             * <p>
             * Set a negative number to save specified length of parameter string to the tag.
             */
            public static int REDIS_PARAMETER_MAX_LENGTH = 128;

            /**
             * First , Operation represent a cache span is write or write action , and the op is tagged with key "cache.op"
             * This config term define what command should be converted to write Operation .
             * In OAP , virtual cache service analysis cache write/read metrics separately
             *
             * @see org.apache.skywalking.apm.agent.core.context.tag.Tags#CACHE_OP
             * @see JedisMethodInterceptor#pareOperation(String)
             */
            public static Set<String> OPERATION_MAPPING_WRITE = new HashSet<>(Arrays.asList(
                    "del",
                    "setex",
                    "brpoplpush",
                    "brpoplpush",
                    "lpushx",
                    "setnx",
                    "lset",
                    "decrBy",
                    "blpop",
                    "rpoplpush",
                    "incr",
                    "setbit",
                    "set",
                    "sadd",
                    "rpushx",
                    "hdel",
                    "lrem",
                    "hset",
                    "hdel"
            ));
            /**
             * First , Operation represent a cache span is write or read action , and the op is tagged with key "cache.op"
             * This config term define what command should be converted to write Operation .
             * In OAP , virtual cache service analysis cache write/read metrics separately
             *
             * @see org.apache.skywalking.apm.agent.core.context.tag.Tags#CACHE_OP
             * @see JedisMethodInterceptor#pareOperation(String)
             */
            public static Set<String> OPERATION_MAPPING_READ = new HashSet<>(Arrays.asList(
                    "zcount",
                    "hscan",
                    "zlexcount",
                    "bitcount",
                    "llen",
                    "zscan",
                    "hvals",
                    "scan",
                    "zrank",
                    "blpop",
                    "get",
                    "hexists",
                    "zcard",
                    "zrevrank",
                    "setrange",
                    "sdiff",
                    "zrevrange",
                    "getbit",
                    "scard",
                    "hget"
                    ));

        }
    }
}
