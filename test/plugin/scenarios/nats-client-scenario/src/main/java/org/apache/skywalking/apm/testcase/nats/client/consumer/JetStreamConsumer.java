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

import io.nats.client.*;
import io.nats.client.api.ConsumerConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.testcase.nats.client.util.StreamUtil;

import java.time.Duration;
import java.util.List;

@Slf4j
public class JetStreamConsumer implements Consumer {

    private final String stream;
    private JetStreamSubscription subscribe;

    public JetStreamConsumer(String stream) {
        this.stream = stream;
    }

    @Override
    public void subscribe(Connection connection, String subject) {
        try {
            ConsumerConfiguration cc = ConsumerConfiguration.builder()
                    .ackWait(Duration.ofMillis(100))
                    .build();
            PullSubscribeOptions pullOptions = PullSubscribeOptions.builder()
                    .durable(this.stream + "-durable") // required
                    .configuration(cc)
                    .build();
            JetStream js = connection.jetStream();
            StreamUtil.initStream(connection, subject, this.stream);
            this.subscribe = js.subscribe(subject, pullOptions);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean fetchMsg() {
        List<Message> messages = subscribe.fetch(10, Duration.ofMillis(100));
        if (messages != null) {
            messages.forEach(msg -> log.info("received message : {} ", msg));
            return true;
        }
        return false;
    }
}
