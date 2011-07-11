/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.jms.temp;

import javax.jms.ConnectionFactory;

import org.apache.activemq.command.ActiveMQTempQueue;
import org.apache.camel.CamelContext;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.CamelJmsTestHelper;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.camel.component.jms.JmsComponent.jmsComponentAutoAcknowledge;

/**
 * @version 
 */
public class TemporaryQueueRouteTest extends CamelTestSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(TemporaryQueueRouteTest.class);

    protected String endpointUri = "activemq:temp:queue:cheese";
    protected Object expectedBody = "<hello>world!</hello>";
    protected MyBean myBean = new MyBean();

    @Test
    public void testSendMessage() throws Exception {
        MockEndpoint endpoint = getMockEndpoint("mock:result");
        endpoint.expectedBodiesReceived("Result");

        Thread.sleep(1000);
        log.info("Sending: " + expectedBody + " to: " + endpointUri);
        template.sendBody(endpointUri, expectedBody);

        endpoint.assertIsSatisfied();

        Message message = myBean.getMessage();
        assertNotNull("should have received a message", message);

        LOG.info("Received: " + message);
        Object header = message.getHeader("JMSDestination");
        isValidDestination(header);
    }

    protected void isValidDestination(Object header) {
        ActiveMQTempQueue destination = assertIsInstanceOf(ActiveMQTempQueue.class, header);
        LOG.info("Received message has a temporary queue: " + destination);
    }

    protected CamelContext createCamelContext() throws Exception {
        deleteDirectory("activemq-data");

        CamelContext camelContext = super.createCamelContext();

        ConnectionFactory connectionFactory = CamelJmsTestHelper.createPersistentConnectionFactory();
        camelContext.addComponent("activemq", jmsComponentAutoAcknowledge(connectionFactory));

        return camelContext;
    }

    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws Exception {
                from(endpointUri).bean(myBean).to("mock:result");
            }
        };
    }

    public static class MyBean {
        private Message message;

        public String onMessage(Message message) {
            this.message = message;
            LOG.info("Invoked bean with: " + message);
            return "Result";
        }

        public Message getMessage() {
            return message;
        }
    }
}
