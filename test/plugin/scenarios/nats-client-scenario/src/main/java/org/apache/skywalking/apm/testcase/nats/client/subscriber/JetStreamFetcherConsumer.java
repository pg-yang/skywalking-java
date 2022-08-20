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

package org.apache.skywalking.apm.testcase.nats.client.subscriber;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.PullSubscribeOptions;
import io.nats.client.api.ConsumerConfiguration;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

@Slf4j
public class JetStreamFetcherConsumer implements Consumer {

    private JetStreamSubscription subscribe;
    private Connection connection;

    @Override
    public void subscribe(Connection connection, String subject) {
        this.connection = connection;
    }

    public boolean fetch(String stream, String subject) {
        try {
            ConsumerConfiguration cc = ConsumerConfiguration.builder()
                    .ackWait(Duration.ofMillis(100))
                    .build();
            PullSubscribeOptions pullOptions = PullSubscribeOptions.builder()
                    .durable(stream + "-durable")
                    .configuration(cc)
                    .build();
            JetStream js = connection.jetStream();
            this.subscribe = js.subscribe(subject, pullOptions);
            List<Message> messages = subscribe.fetch(1, 50);
            if (messages != null) {
                messages.forEach(msg -> log.info("received message : {} ", msg));
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
