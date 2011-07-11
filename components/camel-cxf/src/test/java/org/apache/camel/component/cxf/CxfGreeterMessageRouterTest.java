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

package org.apache.camel.component.cxf;

import javax.xml.ws.Endpoint;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.hello_world_soap_http.GreeterImpl;
import org.junit.BeforeClass;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * The Greeter test with the MESSAGE date format
 */
public class CxfGreeterMessageRouterTest extends AbstractCXFGreeterRouterTest {
    private static int port1 = AvailablePortFinder.getNextAvailable(); 
    private static int port2 = AvailablePortFinder.getNextAvailable();
    static {
        System.setProperty("CxfGreeterMessageRouterTest.port1", Integer.toString(port1));
        System.setProperty("CxfGreeterMessageRouterTest.port2", Integer.toString(port2));
    }
    public String getPort1() {
        return Integer.toString(port1);
    }

    public String getPort2() {
        return Integer.toString(port2);
    }

    @BeforeClass
    public static void startService() {
        Object implementor = new GreeterImpl();
        String address = "http://localhost:" + port1 + "/SoapContext/SoapPort";
        endpoint = Endpoint.publish(address, implementor); 
    }
    
    
    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {
                from("cxf:bean:routerEndpoint?dataFormat=Message&publishedEndpointUrl=http://www.simple.com/services/test")
                    .to("cxf:bean:serviceEndpoint?dataFormat=Message");
            }
        };
    }
    
    @Override
    protected ClassPathXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("org/apache/camel/component/cxf/GreeterEndpointBeans.xml");
    }
  
}