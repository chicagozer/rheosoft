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
package org.apache.camel.component.cxf.jaxrs;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.component.cxf.jaxrs.testbean.Customer;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.junit4.CamelSpringTestSupport;
import org.apache.camel.util.CastUtils;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CxfRsProducerTest extends CamelSpringTestSupport {
    private static int port1 = AvailablePortFinder.getNextAvailable(); 
    private static int port2 = AvailablePortFinder.getNextAvailable(); 
    
    public static class JettyProcessor implements Processor {
        public void process(Exchange exchange) throws Exception {
            // check the query
            Message inMessage = exchange.getIn();
            exchange.getOut().setBody(inMessage.getHeader(Exchange.HTTP_QUERY, String.class));
        }
    }

    public int getPort1() {
        return port1;
    }
    public int getPort2() {
        return port2;
    }
    
    @Override
    protected AbstractXmlApplicationContext createApplicationContext() {     
        System.setProperty("CxfRsProducerTest.port1", Integer.toString(port1));
        System.setProperty("CxfRsProducerTest.port2", Integer.toString(port2));
        return new ClassPathXmlApplicationContext("org/apache/camel/component/cxf/jaxrs/CxfRsSpringProducer.xml");
    }
    
    protected void setupDestinationURL(Message inMessage) {
        // do nothing here
    }
    
    @Test
    public void testGetConstumerWithClientProxyAPI() {
        // START SNIPPET: ProxyExample
        Exchange exchange = template.send("direct://proxy", new Processor() {
            public void process(Exchange exchange) throws Exception {
                exchange.setPattern(ExchangePattern.InOut);
                Message inMessage = exchange.getIn();
                setupDestinationURL(inMessage);
                // set the operation name 
                inMessage.setHeader(CxfConstants.OPERATION_NAME, "getCustomer");
                // using the proxy client API
                inMessage.setHeader(CxfConstants.CAMEL_CXF_RS_USING_HTTP_API, Boolean.FALSE);
                // set the parameters , if you just have one parameter 
                // camel will put this object into an Object[] itself
                inMessage.setBody("123");
            }
        });
     
        // get the response message 
        Customer response = (Customer) exchange.getOut().getBody();
        
        assertNotNull("The response should not be null ", response);
        assertEquals("Get a wrong customer id ", String.valueOf(response.getId()), "123");
        assertEquals("Get a wrong customer name", response.getName(), "John");
        assertEquals("Get a wrong response code", 200, exchange.getOut().getHeader(Exchange.HTTP_RESPONSE_CODE));
        // END SNIPPET: ProxyExample     
    }
    
    @Test
    public void testGetConstumersWithClientProxyAPI() {
        Exchange exchange = template.send("direct://proxy", new Processor() {
            public void process(Exchange exchange) throws Exception {
                exchange.setPattern(ExchangePattern.InOut);
                Message inMessage = exchange.getIn();
                setupDestinationURL(inMessage);
                // set the operation name 
                inMessage.setHeader(CxfConstants.OPERATION_NAME, "getCustomers");
                // using the proxy client API
                inMessage.setHeader(CxfConstants.CAMEL_CXF_RS_USING_HTTP_API, Boolean.FALSE);
                // set the parameters , if you just have one parameter 
                // camel will put this object into an Object[] itself
                inMessage.setBody(null);
            }
        });
     
        // get the response message 
        List<Customer> response = CastUtils.cast((List) exchange.getOut().getBody());
        
        assertNotNull("The response should not be null ", response);
        assertEquals("Get a wrong customer id ", String.valueOf(response.get(0).getId()), "113");
        assertEquals("Get a wrong customer name", response.get(0).getName(), "Dan");
        assertEquals("Get a wrong response code", 200, exchange.getOut().getHeader(Exchange.HTTP_RESPONSE_CODE));
    }
    
    @Test
    public void testGetConstumerWithHttpCentralClientAPI() {
     // START SNIPPET: HttpExample
        Exchange exchange = template.send("direct://http", new Processor() {
            public void process(Exchange exchange) throws Exception {
                exchange.setPattern(ExchangePattern.InOut);
                Message inMessage = exchange.getIn();
                setupDestinationURL(inMessage);
                // using the http central client API
                inMessage.setHeader(CxfConstants.CAMEL_CXF_RS_USING_HTTP_API, Boolean.TRUE);
                // set the Http method
                inMessage.setHeader(Exchange.HTTP_METHOD, "GET");
                // set the relative path
                inMessage.setHeader(Exchange.HTTP_PATH, "/customerservice/customers/123");                
                // Specify the response class , cxfrs will use InputStream as the response object type 
                inMessage.setHeader(CxfConstants.CAMEL_CXF_RS_RESPONSE_CLASS, Customer.class);
                // since we use the Get method, so we don't need to set the message body
                inMessage.setBody(null);                
            }
        });
     
        // get the response message 
        Customer response = (Customer) exchange.getOut().getBody();
        
        assertNotNull("The response should not be null ", response);
        assertEquals("Get a wrong customer id ", String.valueOf(response.getId()), "123");
        assertEquals("Get a wrong customer name", response.getName(), "John");
        assertEquals("Get a wrong response code", 200, exchange.getOut().getHeader(Exchange.HTTP_RESPONSE_CODE));
        // END SNIPPET: HttpExample 
    }
    
    @Test
    public void testGetConstumerWithCxfRsEndpoint() {
        Exchange exchange 
            = template.send("cxfrs://http://localhost:" + getPort1() + "?httpClientAPI=true", new Processor() {
                public void process(Exchange exchange) throws Exception {
                    exchange.setPattern(ExchangePattern.InOut);
                    Message inMessage = exchange.getIn();
                    // set the Http method
                    inMessage.setHeader(Exchange.HTTP_METHOD, "GET");
                    // set the relative path
                    inMessage.setHeader(Exchange.HTTP_PATH, "/customerservice/customers/123");                
                    // Specify the response class , cxfrs will use InputStream as the response object type 
                    inMessage.setHeader(CxfConstants.CAMEL_CXF_RS_RESPONSE_CLASS, Customer.class);
                    // since we use the Get method, so we don't need to set the message body
                    inMessage.setBody(null);                
                }
            });
     
        // get the response message 
        Customer response = (Customer) exchange.getOut().getBody();
        assertNotNull("The response should not be null ", response);
        assertEquals("Get a wrong customer id ", String.valueOf(response.getId()), "123");
        assertEquals("Get a wrong customer name", response.getName(), "John");
        assertEquals("Get a wrong response code", 200, exchange.getOut().getHeader(Exchange.HTTP_RESPONSE_CODE));
    }
    
    @Test
    public void testAddCustomerUniqueResponseCodeWithHttpClientAPI() {
        Exchange exchange 
            = template.send("cxfrs://http://localhost:" + getPort1() + "?httpClientAPI=true", new Processor() {
                public void process(Exchange exchange) throws Exception {
                    exchange.setPattern(ExchangePattern.InOut);
                    Message inMessage = exchange.getIn();
                    // set the Http method
                    inMessage.setHeader(Exchange.HTTP_METHOD, "POST");
                    // set the relative path
                    inMessage.setHeader(Exchange.HTTP_PATH, "/customerservice/customersUniqueResponseCode");                
                    // create a new customer object
                    Customer customer = new Customer();
                    customer.setId(9999);
                    customer.setName("HttpClient");
                    inMessage.setBody(customer);                
                }
            });
        
        // get the response message 
        Response response = (Response) exchange.getOut().getBody();
        assertNotNull("The response should not be null ", response);
        assertNotNull("The response entity should not be null", response.getEntity());
        // check the response code
        assertEquals("Get a wrong response code", 201, response.getStatus());
        // check the response code from message header
        assertEquals("Get a wrong response code", 201, exchange.getOut().getHeader(Exchange.HTTP_RESPONSE_CODE));
    }
    
    @Test
    public void testAddCustomerUniqueResponseCodeWithProxyAPI() {
        Exchange exchange = template.send("direct://proxy", new Processor() {
            public void process(Exchange exchange) throws Exception {
                exchange.setPattern(ExchangePattern.InOut);
                Message inMessage = exchange.getIn();
                setupDestinationURL(inMessage);
                // set the operation name 
                inMessage.setHeader(CxfConstants.OPERATION_NAME, "addCustomerUniqueResponseCode");
                // using the proxy client API
                inMessage.setHeader(CxfConstants.CAMEL_CXF_RS_USING_HTTP_API, Boolean.FALSE);
                // set the parameters , if you just have one parameter 
                // camel will put this object into an Object[] itself
                Customer customer = new Customer();
                customer.setId(8888);
                customer.setName("ProxyAPI");
                inMessage.setBody(customer);
            }
        });
        
        // get the response message 
        Response response = (Response) exchange.getOut().getBody();
        assertNotNull("The response should not be null ", response);
        assertNotNull("The response entity should not be null", response.getEntity());
        // check the response code
        assertEquals("Get a wrong response code", 201, response.getStatus());
        // check the response code from message header
        assertEquals("Get a wrong response code", 201, exchange.getOut().getHeader(Exchange.HTTP_RESPONSE_CODE));
        
    }
    
    @Test
    public void testAddCustomerUniqueResponseCode() {
        Exchange exchange 
            = template.send("cxfrs://http://localhost:" + getPort1()  + "?httpClientAPI=true", new Processor() {
                public void process(Exchange exchange) throws Exception {
                    exchange.setPattern(ExchangePattern.InOut);
                    Message inMessage = exchange.getIn();
                    // set the Http method
                    inMessage.setHeader(Exchange.HTTP_METHOD, "POST");
                    // set the relative path
                    inMessage.setHeader(Exchange.HTTP_PATH, "/customerservice/customersUniqueResponseCode");                
                    // put the response's entity into out message body
                    inMessage.setHeader(CxfConstants.CAMEL_CXF_RS_RESPONSE_CLASS, Customer.class);
                    // create a new customer object
                    Customer customer = new Customer();
                    customer.setId(8888);
                    customer.setName("Willem");
                    inMessage.setBody(customer);                
                }
            });
        
        // get the response message 
        Customer response = (Customer) exchange.getOut().getBody();
        assertNotNull("The response should not be null ", response);
        assertTrue("Get a wrong customer id ", response.getId() != 8888);
        assertEquals("Get a wrong customer name", response.getName(), "Willem");
        assertEquals("Get a wrong response code", 201, exchange.getOut().getHeader(Exchange.HTTP_RESPONSE_CODE));
    }
    
    @Test
    public void testProducerWithQueryParameters() {
        // START SNIPPET: QueryExample
        Exchange exchange = template.send("cxfrs://http://localhost:" + getPort2() + "/testQuery?httpClientAPI=true&q1=12&q2=13"
        // END SNIPPET: QueryExample                                   
            , new Processor() {        
                public void process(Exchange exchange) throws Exception {
                    exchange.setPattern(ExchangePattern.InOut);
                    Message inMessage = exchange.getIn();
                    // set the Http method
                    inMessage.setHeader(Exchange.HTTP_METHOD, "GET");
                    inMessage.setHeader(CxfConstants.CAMEL_CXF_RS_RESPONSE_CLASS, InputStream.class);
                    inMessage.setBody(null);                
                }
            
            });
     
        // get the response message 
        String response = exchange.getOut().getBody(String.class);
        assertNotNull("The response should not be null ", response);
        assertEquals("The response value is wrong", "q1=12&q2=13", response);
    }
    
    @Test
    public void testProducerWithQueryParametersHeader() {
        Exchange exchange = template.send("cxfrs://http://localhost:" + getPort2() + "/testQuery?httpClientAPI=true&q1=12&q2=13"
            , new Processor() {
                public void process(Exchange exchange) throws Exception {
                    exchange.setPattern(ExchangePattern.InOut);
                    Message inMessage = exchange.getIn();
                    // set the Http method
                    inMessage.setHeader(Exchange.HTTP_METHOD, "GET");
                    inMessage.setHeader(CxfConstants.CAMEL_CXF_RS_RESPONSE_CLASS, InputStream.class);
                    // override the parameter setting from URI
                    // START SNIPPET: QueryMapExample
                    Map<String, String> queryMap = new LinkedHashMap<String, String>();                    
                    queryMap.put("q1", "new");
                    queryMap.put("q2", "world");                    
                    inMessage.setHeader(CxfConstants.CAMEL_CXF_RS_QUERY_MAP, queryMap);
                    // END SNIPPET: QueryMapExample 
                    inMessage.setBody(null);                
                }
            
            });
     
        // get the response message 
        String response = exchange.getOut().getBody(String.class);
        assertNotNull("The response should not be null ", response);
        assertEquals("The response value is wrong", "q1=new&q2=world", response);
    }
    
    

    @Test    
    public void testRestServerDirectlyGetCustomer() {
        // we cannot convert directly to Customer as we need camel-jaxb
        String response = template.requestBodyAndHeader("cxfrs:http://localhost:" + getPort1() + "/customerservice/customers/123",
                null, Exchange.HTTP_METHOD, "GET", String.class);
        
        assertNotNull("The response should not be null ", response);
    }

    @Test
    public void testRestServerDirectlyAddCustomer() {
        Customer input = new Customer();
        input.setName("Donald Duck");

        // we cannot convert directly to Customer as we need camel-jaxb
        String response = template.requestBodyAndHeader("cxfrs:http://localhost:" + getPort1() + "/customerservice/customers",
                input, Exchange.HTTP_METHOD, "POST", String.class);

        assertNotNull(response);
        assertTrue(response.endsWith("<name>Donald Duck</name></Customer>"));
    }

}
