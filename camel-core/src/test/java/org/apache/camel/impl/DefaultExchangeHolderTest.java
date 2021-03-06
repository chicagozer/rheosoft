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
package org.apache.camel.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.ContextTestSupport;
import org.apache.camel.Exchange;

/**
 * @version 
 */
public class DefaultExchangeHolderTest extends ContextTestSupport {

    private String id;

    public void testMarshal() throws Exception {
        DefaultExchangeHolder holder = createHolder(true);
        assertNotNull(holder);
        assertNotNull(holder.toString());
    }

    public void testNoProperties() throws Exception {
        DefaultExchangeHolder holder = createHolder(false);
        assertNotNull(holder);

        Exchange exchange = new DefaultExchange(context);
        DefaultExchangeHolder.unmarshal(exchange, holder);

        assertEquals("Hello World", exchange.getIn().getBody());
        assertEquals("Bye World", exchange.getOut().getBody());
        assertEquals(123, exchange.getIn().getHeader("foo"));
        assertNull(exchange.getProperty("bar"));
    }

    public void testUnmarshal() throws Exception {
        id = null;
        Exchange exchange = new DefaultExchange(context);

        DefaultExchangeHolder.unmarshal(exchange, createHolder(true));
        assertEquals("Hello World", exchange.getIn().getBody());
        assertEquals("Bye World", exchange.getOut().getBody());
        assertEquals(123, exchange.getIn().getHeader("foo"));
        assertEquals("Hi Camel", exchange.getIn().getHeader("CamelFoo"));
        assertEquals(444, exchange.getProperty("bar"));
        assertEquals(555, exchange.getProperty("CamelBar"));
        assertEquals(id, exchange.getExchangeId());
    }

    public void testSkipNonSerializableData() throws Exception {
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setBody("Hello World");
        exchange.getIn().setHeader("Foo", new MyFoo("Tiger"));
        exchange.getIn().setHeader("Bar", 123);

        DefaultExchangeHolder holder = DefaultExchangeHolder.marshal(exchange);

        exchange = new DefaultExchange(context);
        DefaultExchangeHolder.unmarshal(exchange, holder);

        // the non serializable header should be skipped
        assertEquals("Hello World", exchange.getIn().getBody());
        assertEquals(123, exchange.getIn().getHeader("Bar"));
        assertNull(exchange.getIn().getHeader("Foo"));
    }

    @SuppressWarnings("unchecked")
    public void testSkipNonSerializableDataFromList() throws Exception {
        // use a mixed list, the MyFoo is not serializable so the entire list should be skipped
        List list = new ArrayList();
        list.add("I am okay");
        list.add(new MyFoo("Tiger"));

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setBody("Hello World");
        exchange.getIn().setHeader("Foo", list);
        exchange.getIn().setHeader("Bar", 123);

        DefaultExchangeHolder holder = DefaultExchangeHolder.marshal(exchange);

        exchange = new DefaultExchange(context);
        DefaultExchangeHolder.unmarshal(exchange, holder);

        // the non serializable header should be skipped
        assertEquals("Hello World", exchange.getIn().getBody());
        assertEquals(123, exchange.getIn().getHeader("Bar"));
        assertNull(exchange.getIn().getHeader("Foo"));
    }

    @SuppressWarnings("unchecked")
    public void testSkipNonSerializableDataFromMap() throws Exception {
        // use a mixed Map, the MyFoo is not serializable so the entire map should be skipped
        Map map = new HashMap();
        map.put("A", "I am okay");
        map.put("B", new MyFoo("Tiger"));

        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setBody("Hello World");
        exchange.getIn().setHeader("Foo", map);
        exchange.getIn().setHeader("Bar", 123);

        DefaultExchangeHolder holder = DefaultExchangeHolder.marshal(exchange);

        exchange = new DefaultExchange(context);
        DefaultExchangeHolder.unmarshal(exchange, holder);

        // the non serializable header should be skipped
        assertEquals("Hello World", exchange.getIn().getBody());
        assertEquals(123, exchange.getIn().getHeader("Bar"));
        assertNull(exchange.getIn().getHeader("Foo"));
    }

    private DefaultExchangeHolder createHolder(boolean includeProperties) {
        Exchange exchange = new DefaultExchange(context);
        id = exchange.getExchangeId();
        exchange.getIn().setBody("Hello World");
        exchange.getIn().setHeader("foo", 123);
        exchange.getIn().setHeader("CamelFoo", "Hi Camel");
        exchange.setProperty("bar", 444);
        exchange.setProperty("CamelBar", 555);
        exchange.getOut().setBody("Bye World");
        return DefaultExchangeHolder.marshal(exchange, includeProperties);
    }

    private final class MyFoo {
        private String foo;

        private MyFoo(String foo) {
            this.foo = foo;
        }

        @SuppressWarnings("unused")
        public String getFoo() {
            return foo;
        }

    }

}
