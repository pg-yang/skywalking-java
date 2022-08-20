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

package org.apache.skywalking.apm.plugin.nats.client;

import io.nats.client.Message;
import io.nats.client.impl.NatsMessage;
import io.nats.client.support.Status;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.IntegerTag;
import org.apache.skywalking.apm.agent.core.context.tag.StringTag;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.util.StringUtil;

import java.util.Optional;

public class NatsCommons {

    private static final String SID = "sid";
    private static final String REPLY_TO = "reply_to";
    private static final String MSG_STATE = "msg_state";

    static boolean is(Message msg) {
        if (msg == null) {
            return false;
        }
        if (msg.getClass().getName().equals("io.nats.client.impl.NatsJetStreamMessage")) {
            return true;
        }
        return msg != null && msg.getClass().getName().equals(NatsMessage.class.getName()) && msg.getData().length > 0;
    }

    static AbstractSpan createEntry(Message message) {
        ContextCarrier contextCarrier = new ContextCarrier();
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            if (StringUtil.isNotEmpty(next.getHeadKey())) {
                next.setHeadValue(message.getHeaders().getFirst(next.getHeadKey()));
            }
        }
        AbstractSpan span = ContextManager.createEntrySpan("/" + message.getSubject(), contextCarrier);
        addCommonTag(span, message);
        span.setComponent(ComponentsDefine.NATS);
        return span;
    }

    static AbstractSpan createExit(Message message, String peer, String operationName) {
        ContextCarrier contextCarrier = new ContextCarrier();
        AbstractSpan span = ContextManager.createExitSpan(operationName, contextCarrier, peer);
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            if (StringUtil.isNotEmpty(next.getHeadKey())
                    && StringUtil.isNotEmpty(next.getHeadValue())) {
                message.getHeaders().add(next.getHeadKey(), next.getHeadValue());
            }
        }
        addCommonTag(span, message);
        SpanLayer.asMQ(span);
        span.setComponent(ComponentsDefine.NATS);
        return span;
    }

    static void addCommonTag(AbstractSpan span, Message message) {
        Optional.ofNullable(message.getReplyTo()).ifPresent(v -> span.tag(new StringTag(REPLY_TO), v));
        Optional.ofNullable(message.getSID()).ifPresent(v -> span.tag(new StringTag(SID), v));
        Optional.ofNullable(message.getStatus()).map(Status::getCode).ifPresent(v -> span.tag(new IntegerTag(MSG_STATE), String.valueOf(v)));
    }
}
