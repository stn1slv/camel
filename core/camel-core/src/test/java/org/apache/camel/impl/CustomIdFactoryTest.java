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
package org.apache.camel.impl;

import org.apache.camel.CamelContext;
import org.apache.camel.ContextTestSupport;
import org.apache.camel.Exchange;
import org.apache.camel.NamedNode;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.InterceptStrategy;
import org.apache.camel.spi.NodeIdFactory;
import org.apache.camel.support.processor.DelegateProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Demonstrates how you can use a custom id factory to assign ids to Camel Java routes and then attach your own debugger
 * and be able to use the custom ids to know at what point you are debugging
 */
public class CustomIdFactoryTest extends ContextTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(CustomIdFactoryTest.class);

    private static int counter;
    private static String ids;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        ids = "";
        counter = 0;
        super.setUp();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                // use our own id factory so we can generate the keys we like to
                // use
                context.getCamelContextExtension().addContextPlugin(NodeIdFactory.class, buildNodeIdFactory());

                // add our debugger so we can debug camel routes when we send in
                // messages
                context.getCamelContextExtension().addInterceptStrategy(new MyDebuggerCheckingId());

                // a little content based router so we got 2 paths to route at
                // runtime
                from("direct:start").choice().when(body().contains("Hello")).to("mock:hello").otherwise().log("Hey")
                        .to("mock:other").end();
            }
        };
    }

    private static NodeIdFactory buildNodeIdFactory() {
        return definition -> "#" + definition.getShortName() + ++counter + "#";
    }

    /**
     * Test path 1
     */
    @Test
    public void testHello() throws Exception {
        assertEquals("#route1#", context.getRouteDefinitions().get(0).getId());

        getMockEndpoint("mock:hello").expectedMessageCount(1);
        template.sendBody("direct:start", "Hello World");
        assertMockEndpointsSatisfied();

        // this should take the when path (first to)
        assertEquals("#choice3##to4#", ids);
    }

    /**
     * Test path 2
     */
    @Test
    public void testOther() throws Exception {
        assertEquals("#route1#", context.getRouteDefinitions().get(0).getId());

        getMockEndpoint("mock:other").expectedMessageCount(1);
        template.sendBody("direct:start", "Bye World");
        assertMockEndpointsSatisfied();

        // this should take the otherwise path
        assertEquals("#choice3##log5##to6#", ids);
    }

    private static class MyDebuggerCheckingId implements InterceptStrategy {

        @Override
        public Processor wrapProcessorInInterceptors(
                final CamelContext context, final NamedNode definition, Processor target, Processor nextTarget) {

            return new DelegateProcessor(target) {
                @Override
                protected void processNext(Exchange exchange) throws Exception {
                    LOG.debug("Debugging at: {} with id: {} with exchange: {}", definition, definition.getId(), exchange);

                    // record the path taken at runtime
                    ids += definition.getId();

                    // continue to the real target by invoking super
                    super.processNext(exchange);
                }
            };
        }

    }
}
