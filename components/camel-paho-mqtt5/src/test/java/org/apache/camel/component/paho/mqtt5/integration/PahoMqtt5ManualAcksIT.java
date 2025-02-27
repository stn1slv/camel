/*
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
package org.apache.camel.component.paho.mqtt5.integration;

import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Test;

public class PahoMqtt5ManualAcksIT extends PahoMqtt5ITSupport {

    @EndpointInject("mock:test")
    MockEndpoint mock;

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:test")
                        .to("paho-mqtt5:queue?brokerUrl=tcp://localhost:" + mqttPort + "&qos=2");

                from("paho-mqtt5:queue?brokerUrl=tcp://localhost:" + mqttPort + "&qos=2&manualAcksEnabled=true")
                        .to("mock:test");
            }
        };
    }

    @Test
    public void testManualAcks() throws Exception {
        mock.expectedMessageCount(1);

        template.sendBody("direct:test", "Test Message");

        mock.assertIsSatisfied();
    }
}
