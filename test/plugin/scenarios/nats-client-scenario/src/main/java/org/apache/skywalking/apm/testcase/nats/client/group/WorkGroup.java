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

package org.apache.skywalking.apm.testcase.nats.client.group;


import io.nats.client.Connection;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsMessage;
import org.apache.skywalking.apm.testcase.nats.client.connet.TrackedConnection;
import org.apache.skywalking.apm.testcase.nats.client.consumer.Consumer;
import org.apache.skywalking.apm.testcase.nats.client.publisher.Publisher;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WorkGroup {
    private final String message;
    private final String subject;
    private final Map<String, List<String>> headers;
    private final String url;
    private final List<Connection> connections = new ArrayList<>();

    public WorkGroup(String message, String subject, Map<String, List<String>> headers, String url) {
        this.message = message;
        this.subject = subject;
        this.headers = headers;
        this.url = url;
    }

    // A work will push and receive message.
    public Work build(Publisher publisher, Consumer consumer) {

        NatsMessage.Builder msgBuilder = NatsMessage.builder()
                .data(this.message, StandardCharsets.UTF_8)
                .subject(this.subject);
        Headers msgHeader = new Headers();
        Optional.ofNullable(headers).map(Map::entrySet).ifPresent(
                e -> e.forEach(entry -> msgHeader.add(entry.getKey(), entry.getValue()))
        );

        return new Work() {
            @Override
            public void subscribe() {
                Connection consumerCon = WorkGroup.this.createConnection();
                Thread thread = new Thread(() -> {
                    consumer.subscribe(consumerCon, WorkGroup.this.subject);
                });
                thread.start();
            }

            @Override
            public void publish() {
                Connection connection = WorkGroup.this.createConnection();
                publisher.publish(connection, msgBuilder.build(), WorkGroup.this.subject);
            }
        };
    }

    private Connection createConnection() {
        Connection connection;
        try {
            connection = TrackedConnection.newConnection(WorkGroup.this.url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return connection;
    }

    public void cleanup() {
        connections.forEach(c -> {
            try {
                c.close();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public interface Work {
        void subscribe();

        void publish();


    }


}
