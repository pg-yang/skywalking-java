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

package org.apache.skywalking.apm.testcase.nats.client.publisher;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.api.PublishAck;
import org.apache.skywalking.apm.testcase.nats.client.work.StreamUtil;

public class JetStreamPublisherFetcher implements Publisher {

    private Fetcher fetcher;

    private String stream;

    public JetStreamPublisherFetcher(Fetcher fetcher, String stream) {
        this.stream = stream;
        this.fetcher = fetcher;
    }

    @Override
    public void publish(Connection connection, String msg, String subject) {

        try {
            JetStream js = connection.jetStream();
            StreamUtil.initStream(connection, subject, stream);
            PublishAck pa = js.publish(buildMsg(subject, msg));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        while (!fetcher.fetch(stream, subject)) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public interface Fetcher {
        boolean fetch(String stream, String subject);
    }

}
