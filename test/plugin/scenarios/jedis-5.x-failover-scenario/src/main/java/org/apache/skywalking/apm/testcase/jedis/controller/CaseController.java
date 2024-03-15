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

package org.apache.skywalking.apm.testcase.jedis.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiClusterClientConfig;
import redis.clients.jedis.PipelineBase;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;

@RestController
@RequestMapping("/case")
public class CaseController {

    private static final String SUCCESS = "Success";

    @Value("${redis.host:127.0.0.1}")
    private String redisHost;

    @Value("${redis.port:6379}")
    private Integer redisPort;

    @RequestMapping("/jedis-scenario")
    @ResponseBody
    public String testcase() throws Exception {
        JedisClientConfig config = DefaultJedisClientConfig.builder().build();
        MultiClusterClientConfig.ClusterConfig[] clientConfigs = new MultiClusterClientConfig.ClusterConfig[1];
        clientConfigs[0] = new MultiClusterClientConfig.ClusterConfig(new HostAndPort(redisHost, redisPort), config);
        MultiClusterClientConfig.Builder builder = new MultiClusterClientConfig.Builder(clientConfigs);
        try (MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
            builder.build())) {
            executeRedisCommand(provider);
        }
        return SUCCESS;
    }

    private void executeRedisCommand(final MultiClusterPooledConnectionProvider provider) {
        try (UnifiedJedis jedis = new UnifiedJedis(provider)) {
            jedis.set("a", "1");
            jedis.get("a");
            jedis.del("a");

            PipelineBase pipeline = jedis.pipelined();
            pipeline.hset("a", "a", "a");
            pipeline.hget("a", "a");
            pipeline.hdel("a", "a");
            pipeline.sync();

            HashMap<String, String> hash = new HashMap<>();
            StreamEntryID streamEntryID = new StreamEntryID("0-1");
            hash.put("a", "1");
            jedis.xadd("abc", streamEntryID, hash);
            Map<String, StreamEntryID> hashMap = new HashMap<>();
            hashMap.put("abc", streamEntryID);
            jedis.xread(new XReadParams().count(1), hashMap);
            jedis.xdel("abc", streamEntryID);
        }

    }

    @RequestMapping("/healthCheck")
    @ResponseBody
    public String healthCheck() throws Exception {
        return SUCCESS;
    }
}
