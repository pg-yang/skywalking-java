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

package org.apache.skywalking.apm.testcase.nats.client.consumer;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;


@Slf4j
public class ReqReplyConsumer implements Consumer {

    private final StopSignal stopSignal;

    public ReqReplyConsumer(StopSignal stopSignal) {
        this.stopSignal = stopSignal;
    }

    @Override
    public void subscribe(Connection connection, String subject) {
        Dispatcher d = connection.createDispatcher((msg) -> {
            log.info("receive : {}, from :{} and will reply ", subject, msg);
            connection.publish(msg.getReplyTo(), "Have received msg".getBytes(StandardCharsets.UTF_8));
        });
        d.subscribe(subject);
    }
}
