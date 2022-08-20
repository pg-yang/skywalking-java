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

import org.apache.skywalking.apm.testcase.nats.client.publisher.JetStreamPublisher;
import org.apache.skywalking.apm.testcase.nats.client.publisher.JetStreamPublisherFetcher;
import org.apache.skywalking.apm.testcase.nats.client.publisher.NormalPublisher;
import org.apache.skywalking.apm.testcase.nats.client.publisher.ReqReplyPublisher;
import org.apache.skywalking.apm.testcase.nats.client.subscriber.JetStreamFetcherConsumer;
import org.apache.skywalking.apm.testcase.nats.client.subscriber.JetStreamHandlerConsumer;
import org.apache.skywalking.apm.testcase.nats.client.subscriber.NextMsgConsumer;
import org.apache.skywalking.apm.testcase.nats.client.subscriber.ReqReplyConsumer;
import org.apache.skywalking.apm.testcase.nats.client.work.StopSignal;
import org.apache.skywalking.apm.testcase.nats.client.work.WorkBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("nats")
public class StartController {

    private final List<WorkBuilder.Work> works = new ArrayList<>();

    private final StopSignal stopSignal = new StopSignal();

    @GetMapping(value = "check")
    public String check() throws Exception {
        return "success";
    }

    @GetMapping(value = "start", produces = MediaType.TEXT_HTML_VALUE)
    public String start() throws Exception {
        works.forEach(WorkBuilder.Work::publish);
        return "success";
    }

    @PostConstruct
    public void init() {
        //normal connection
        String server = System.getProperty("nats.server");
        if (server == null) {
            throw new RuntimeException("missing property : nats.server");
        }
        // test normal message  have pub enqueue no sub
        works.add(new WorkBuilder("message-subject-1", "subject-1", server)
                .build(new NormalPublisher(), new NextMsgConsumer(stopSignal)));

        // test request-reply message
        works.add(new WorkBuilder("request-reply-subject-2", "subject-2", server)
                .build(new ReqReplyPublisher(), new ReqReplyConsumer()));

//
//        // test stream message and handle message
        works.add(new WorkBuilder("stream-subject-3", "subject-3", server)
                .build(new JetStreamPublisher("test-stream-v3"), new JetStreamHandlerConsumer("test-stream-v3")));

        // test stream message and pull message
        JetStreamFetcherConsumer jetStreamConsumer = new JetStreamFetcherConsumer();
        works.add(new WorkBuilder("request-stream-subject-4", "subject-4", server)
                .build(new JetStreamPublisherFetcher(jetStreamConsumer::fetch, "test-stream-v2-"), jetStreamConsumer));

        works.forEach(WorkBuilder.Work::subscribe);
    }

    @PreDestroy
    public void stop() {
        stopSignal.stop();
    }

}
