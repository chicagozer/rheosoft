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
package org.apache.camel.component.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.camel.RuntimeCamelException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;


/**
 * @version 
 */
public class SqlRouteTest extends CamelTestSupport {
    protected String driverClass = "org.hsqldb.jdbcDriver";
    protected String url = "jdbc:hsqldb:mem:camel_jdbc";
    protected String user = "sa";
    protected String password = "";
    private DataSource ds;
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testSimpleBody() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(1);
        template.sendBody("direct:simple", "XXX");
        mock.assertIsSatisfied();
        List received = assertIsInstanceOf(List.class, mock.getReceivedExchanges().get(0).getIn().getBody());
        Map row = assertIsInstanceOf(Map.class, received.get(0));
        assertEquals("Linux", row.get("PROJECT"));
    }

    @Test
    public void testQueryAsHeader() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(1);

        template.sendBodyAndHeader("direct:simple", "Camel", SqlConstants.SQL_QUERY, "select * from projects where project = ? order by id");
        mock.assertIsSatisfied();
        List received = assertIsInstanceOf(List.class, mock.getReceivedExchanges().get(0).getIn().getBody());
        Map row = assertIsInstanceOf(Map.class, received.get(0));
        assertEquals(1, row.get("id"));
        assertEquals("ASF", row.get("license"));
        mock.reset();

        mock.expectedMessageCount(1);
        template.sendBodyAndHeader("direct:simple", 3, SqlConstants.SQL_QUERY, "select * from projects where id = ? order by id");
        mock.assertIsSatisfied();
        received = assertIsInstanceOf(List.class, mock.getReceivedExchanges().get(0).getIn().getBody());
        row = assertIsInstanceOf(Map.class, received.get(0));
        assertEquals("Linux", row.get("PROJECT"));
        assertEquals("XXX", row.get("license"));
    }

    @Test
    public void testListBody() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(1);
        List<Object> body = new ArrayList<Object>();
        body.add("ASF");
        body.add("Camel");
        template.sendBody("direct:list", body);
        mock.assertIsSatisfied();
        List received = assertIsInstanceOf(List.class, mock.getReceivedExchanges().get(0).getIn().getBody());
        Map row = assertIsInstanceOf(Map.class, received.get(0));
        assertEquals(1, row.get("ID"));
    }

    @Test
    public void testLowNumberOfParameter() throws Exception {
        try {
            template.sendBody("direct:list", "ASF");
            fail();
        } catch (RuntimeCamelException e) {
            // should have DataAccessException thrown
            assertTrue("Exception thrown is wrong", e.getCause() instanceof DataAccessException);
        }
    }

    @Test
    public void testHighNumberOfParameter() throws Exception {
        try {
            template.sendBody("direct:simple", new Object[] {"ASF", "Foo"});
            fail();
        } catch (RuntimeCamelException e) {
            // should have DataAccessException thrown
            assertTrue("Exception thrown is wrong", e.getCause() instanceof DataAccessException);
        }
    }

    @Test
    public void testListResult() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedHeaderReceived(SqlConstants.SQL_ROW_COUNT, "2");
        mock.expectedMessageCount(1);
        List<Object> body = new ArrayList<Object>();
        body.add("ASF");
        template.sendBody("direct:simple", body);
        mock.assertIsSatisfied();
        List received = assertIsInstanceOf(List.class, mock.getReceivedExchanges().get(0).getIn().getBody());
        assertEquals(2, received.size());
        Map row1 = assertIsInstanceOf(Map.class, received.get(0));
        assertEquals("Camel", row1.get("PROJECT"));
        Map row2 = assertIsInstanceOf(Map.class, received.get(1));
        assertEquals("AMQ", row2.get("PROJECT"));
    }

    @Test
    public void testListLimitedResult() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(1);
        List<Object> body = new ArrayList<Object>();
        body.add("ASF");
        template.sendBody("direct:simpleLimited", body);
        mock.assertIsSatisfied();
        List received = assertIsInstanceOf(List.class, mock.getReceivedExchanges().get(0).getIn().getBody());
        assertEquals(1, received.size());
        Map row1 = assertIsInstanceOf(Map.class, received.get(0));
        assertEquals("Camel", row1.get("PROJECT"));
    }

    @Test
    public void testInsert() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(1);

        template.sendBody("direct:insert", new Object[] {10, "test", "test"});
        mock.assertIsSatisfied();
        try {
            String projectName = (String)jdbcTemplate.queryForObject("select project from projects where id = 10", String.class);
            assertEquals("test", projectName);
        } catch (EmptyResultDataAccessException e) {
            fail("no row inserted");
        }

        Integer actualUpdateCount = mock.getExchanges().get(0).getIn().getHeader(SqlConstants.SQL_UPDATE_COUNT, Integer.class);
        assertEquals((Integer) 1, actualUpdateCount);
    }

    @Test
    public void testNoBody() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(1);
        template.sendBody("direct:no-param", null);
        mock.assertIsSatisfied();
        List received = assertIsInstanceOf(List.class, mock.getReceivedExchanges().get(0).getIn().getBody());
        Map row = assertIsInstanceOf(Map.class, received.get(0));
        assertEquals("Camel", row.get("PROJECT"));
    }
    
    @Test
    public void testHashesInQuery() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(1);
        template.sendBody("direct:no-param-insert", "XGPL");
        mock.assertIsSatisfied();
        Number received = assertIsInstanceOf(Number.class, mock.getReceivedExchanges().get(0).getIn().getHeader(SqlConstants.SQL_UPDATE_COUNT));
        assertEquals(1, received.intValue());
        Map projectNameInserted = jdbcTemplate.queryForMap("select project, license from projects where id = 5");
        assertEquals("#", projectNameInserted.get("PROJECT"));
        assertEquals("XGPL", projectNameInserted.get("LICENSE"));
    }
    
    @Test
    public void testBodyButNoParams() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(1);
        template.sendBody("direct:no-param", "Mock body");
        mock.assertIsSatisfied();
        List received = assertIsInstanceOf(List.class, mock.getReceivedExchanges().get(0).getIn().getBody());
        Map row = assertIsInstanceOf(Map.class, received.get(0));
        assertEquals("Camel", row.get("PROJECT"));
    }

    
    @Before
    public void setUp() throws Exception {
        Class.forName(driverClass);
        super.setUp();

        jdbcTemplate = new JdbcTemplate(ds);
        jdbcTemplate.execute("create table projects (id integer primary key,"
                             + "project varchar(10), license varchar(5))");
        jdbcTemplate.execute("insert into projects values (1, 'Camel', 'ASF')");
        jdbcTemplate.execute("insert into projects values (2, 'AMQ', 'ASF')");
        jdbcTemplate.execute("insert into projects values (3, 'Linux', 'XXX')");
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
        jdbcTemplate.execute("drop table projects");
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                ds = new SingleConnectionDataSource(url, user, password, true);

                getContext().getComponent("sql", SqlComponent.class).setDataSource(ds);

                errorHandler(noErrorHandler());
                
                from("direct:simple").to("sql:select * from projects where license = # order by id")
                    .to("mock:result");

                from("direct:list")
                    .to("sql:select * from projects where license = # and project = # order by id")
                    .to("mock:result");

                from("direct:simpleLimited")
                    .to("sql:select * from projects where license = # order by id?template.maxRows=1")
                    .to("mock:result");

                from("direct:insert").to("sql:insert into projects values (#, #, #)").to("mock:result");
                
                from("direct:no-param").to("sql:select * from projects order by id").to("mock:result");
                
                from("direct:no-param-insert").to("sql:insert into projects values (5, '#', param)?placeholder=param").to("mock:result");
            }
        };
    }

}
