/*
 * Copyright 2011 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rheosoft;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.gae.mail.GMailBinding;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 *
 * @author jim
 */
public class RheoRouteBuilder extends RouteBuilder {

    private static final transient Logger LOG = LoggerFactory.getLogger(RheoRouteBuilder.class);

    @Override
    public void configure() throws Exception {

        LOG.info("entered configure");
        from("servlet:///hello?httpBindingRef=ReaderBinding").setHeader(Exchange.CONTENT_TYPE, constant("text/html")).transform().simple("Hello Jim. You are getting pretty good with this stuff! Comments were ${header.comments}.");

        from("ghttp:///ghello?httpBindingRef=ReaderBinding").setHeader(Exchange.CONTENT_TYPE, constant("text/html")).process(new PushProcessor()).transform().simple("Hello Jim. We caught a gooogle hello! The city was ${header.city}.");

        //HttpEndpoint ep = (HttpEndpoint)getContext().getEndpoint("ghttp");
        //ep.getBinding().setUseReaderForPayload(true);

        
        
        from("servlet:///task")
                .setHeader(Exchange.CONTENT_TYPE, constant("text/html"))
                .process(new TaskProcessor()).transform().simple("ETA ${header.taskETA}, Name ${header.taskName} body class:" + body().getClass().getName());
                
        
        
        from("ghttp:///gweather?httpBindingRef=ReaderBinding")
            .process(new RequestProcessor())
            .marshal().serialization()
            .to("gtask://default")
            .unmarshal().serialization()
            .process(new ResponseProcessor());
        
        
         from("ghttp:///weather")
            .process(new RequestProcessor())
            .marshal().serialization()
            .to("gtask://default")
            .unmarshal().serialization()
            .process(new ResponseProcessor());

         
         
         from("ghttp:///pull?httpBindingRef=ReaderBinding")
            .process(new RequestProcessor())
            .marshal().serialization()
            .to("gtask://pull-queue?outboundBindingRef=GPullTaskBinding")
            .unmarshal().serialization()
            .process(new ResponseProcessor());

         
        
          from("gtask://default")
            .unmarshal().serialization()
            .setHeader("Host", constant("www.google.com"))
            .setHeader(Exchange.HTTP_QUERY, constant("weather=").append(ReportData.city()))
            .enrich("ghttp://www.google.com/ig/api", reportDataAggregator())
            .setHeader(GMailBinding.GMAIL_SUBJECT, constant("Weather report"))
            .setHeader(GMailBinding.GMAIL_SENDER, ReportData.requestor())
            .setHeader(GMailBinding.GMAIL_TO, ReportData.recipient())
            .process(new ReportGenerator())        
            .to("gmail://default");
        
        
        

        //   from("jetty:http:///jetty").streamCaching().process(new PushProcessor()).transform(constant("Hello Jim. We caught a jetty hello!"));




    }
    
     private static AggregationStrategy reportDataAggregator() {
        return new AggregationStrategy() {
            @Override
            public Exchange aggregate(Exchange reportExchange, Exchange weatherExchange) {
                ReportData reportData = reportExchange.getIn().getBody(ReportData.class);
                reportData.setWeather(weatherExchange.getIn().getBody(Document.class));
                return reportExchange;
            }
        };
    }
    
}
