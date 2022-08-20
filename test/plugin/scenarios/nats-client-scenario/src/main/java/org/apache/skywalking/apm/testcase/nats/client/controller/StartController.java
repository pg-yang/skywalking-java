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

package org.apache.skywalking.apm.testcase.nats.client.controller;

import org.apache.skywalking.apm.testcase.nats.client.consumer.JetStreamConsumer;
import org.apache.skywalking.apm.testcase.nats.client.consumer.StopSignal;
import org.apache.skywalking.apm.testcase.nats.client.group.WorkGroup;
import org.apache.skywalking.apm.testcase.nats.client.publisher.JetStreamPublisherFetcher;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;

@Controller
@RequestMapping("nats")
public class StartController {

    private final Map<String, List<String>> headers;

    private final List<WorkGroup.Work> works = new ArrayList<>();

    private final StopSignal stopSignal = new StopSignal();

    public StartController() {
        this.headers = new HashMap<>();
        this.headers.put("a", Arrays.asList("1", "2", "3"));
        this.headers.put("b", Arrays.asList("7", "8"));
    }

    @GetMapping(value = "start", produces = MediaType.TEXT_HTML_VALUE)
    public String start() throws Exception {
        works.forEach(WorkGroup.Work::publish);
        return "success";
    }

    @PostConstruct
    public void init() {
        //normal connection
        String server = System.getProperty("nats.server.url");
        if (server == null) {
            throw new RuntimeException("missing property : nats.server.url");
        }
        // test normal message
//        works.add(new WorkGroup("normal msg", "test-normal", this.headers, server)
//                .build(new NormalPublisher(), new NormalConsumer(stopSignal)));
//
        // test request-reply message
//        works.add(new WorkGroup("request-reply msg", "test-request-reply", this.headers, server)
//                .build(new ReqReplyPublisher(), new ReqReplyConsumer(stopSignal)));

        // test stream message
//        works.add(new WorkGroup("request-stream msg", "test-request-stream-v1", this.headers, server)
//                .build(new JetStreamPublisher("test-stream-v1"), new NormalConsumer(stopSignal)));
//
//        // test stream message and pull message
        JetStreamConsumer jetStreamConsumer = new JetStreamConsumer("test-stream-v2");
        works.add(new WorkGroup("request-stream msg", "test-request-stream-v2", this.headers, server)
                .build(new JetStreamPublisherFetcher("test-stream-v2", jetStreamConsumer::fetchMsg), jetStreamConsumer));
//
//        // test stream message and deal message with handler
//        works.add(new WorkGroup("request-stream msg", "test-request-stream-v3", this.headers, server)
//                .build(new JetStreamPublisher("test-stream-v3"), new JetStreamHandlerConsumer("test-stream-v3", stopSignal)));

        works.forEach(WorkGroup.Work::subscribe);
    }

    @PreDestroy
    public void stop() {
        stopSignal.stop();
    }


}
