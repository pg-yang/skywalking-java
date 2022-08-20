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
import io.nats.client.Message;
import io.nats.client.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.testcase.nats.client.work.StopSignal;

import java.time.Duration;

@Slf4j
public class NextMsgConsumer implements Consumer {

    private final StopSignal stopSignal;

    public NextMsgConsumer(StopSignal stopSignal) {
        this.stopSignal = stopSignal;
    }

    @Override
    public void subscribe(Connection connection, String subject) {
        Subscription sub = connection.subscribe(subject);
        new Thread(() -> {
            while (!stopSignal.stopped()) {
                try {
                    Message msg = sub.nextMessage(Duration.ofMinutes(5));
                    if (msg != null) {
                        msg.ack();
                        log.info("receive : {}, from :{} ", subject, msg);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

}