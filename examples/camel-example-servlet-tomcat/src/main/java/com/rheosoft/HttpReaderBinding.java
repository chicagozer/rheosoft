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

import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.StreamCache;
import org.apache.camel.component.http.DefaultHttpBinding;
import org.apache.camel.component.http.HttpConstants;
import org.apache.camel.component.http.HttpEndpoint;
import org.apache.camel.component.http.HttpMessage;
import org.apache.camel.component.http.helper.HttpHelper;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jim
 */
public class HttpReaderBinding extends DefaultHttpBinding {

    private static final transient Logger LOG = LoggerFactory.getLogger(HttpReaderBinding.class);
    private static final transient boolean useReader = false;
    private HttpEndpoint endpoint;

    public HttpReaderBinding() {
        setUseReaderForPayload(useReader);
    }

    public HttpReaderBinding(HttpEndpoint endpoint) {
        super(endpoint);
        setUseReaderForPayload(useReader);
        this.endpoint = endpoint;
    }

    public HttpReaderBinding(HeaderFilterStrategy headerFilterStrategy) {
        super(headerFilterStrategy);
        setUseReaderForPayload(useReader);
    }

    @Override
    public void readRequest(HttpServletRequest request, HttpMessage message) {
        LOG.trace("readRequest {}", request);

        // lets force a parse of the body and headers
        // message.getBody();
        // populate the headers from the request
        Map<String, Object> headers = message.getHeaders();
        HeaderFilterStrategy headerFilterStrategy = getHeaderFilterStrategy();



        //apply the headerFilterStrategy
        Enumeration names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            Object value = request.getHeader(name);
            // mapping the content-type 
            if (name.toLowerCase().equals("content-type")) {
                name = Exchange.CONTENT_TYPE;
            }
            if (headerFilterStrategy != null
                    && !headerFilterStrategy.applyFilterToExternalHeaders(name, value, message.getExchange())) {
                headers.put(name, value);
            }
        }

        if (request.getCharacterEncoding() != null) {
            headers.put(Exchange.HTTP_CHARACTER_ENCODING, request.getCharacterEncoding());
            message.getExchange().setProperty(Exchange.CHARSET_NAME, request.getCharacterEncoding());
        }

        try {
            populateRequestParameters(request, message);
        } catch (Exception e) {
            throw new RuntimeCamelException("Cannot read request parameters due " + e.getMessage(), e);
        }

        Object body = message.getBody();
        // reset the stream cache if the body is the instance of StreamCache
        if (body instanceof StreamCache) {
            ((StreamCache) body).reset();
        }

        // store the method and query and other info in headers
        headers.put(Exchange.HTTP_METHOD, request.getMethod());
        headers.put(Exchange.HTTP_QUERY, request.getQueryString());
        headers.put(Exchange.HTTP_URL, request.getRequestURL());
        headers.put(Exchange.HTTP_URI, request.getRequestURI());
        headers.put(Exchange.HTTP_PATH, request.getPathInfo());
        headers.put(Exchange.CONTENT_TYPE, request.getContentType());

        if (LOG.isTraceEnabled()) {
            LOG.trace("HTTP method {}", request.getMethod());
            LOG.trace("HTTP query {}", request.getQueryString());
            LOG.trace("HTTP url {}", request.getRequestURL());
            LOG.trace("HTTP uri {}", request.getRequestURI());
            LOG.trace("HTTP path {}", request.getPathInfo());
            LOG.trace("HTTP content-type {}", request.getContentType());
        }

        // if content type is serialized java object, then de-serialize it to a Java object
        if (request.getContentType() != null && HttpConstants.CONTENT_TYPE_JAVA_SERIALIZED_OBJECT.equals(request.getContentType())) {
            try {
                InputStream is = endpoint.getCamelContext().getTypeConverter().mandatoryConvertTo(InputStream.class, body);
                Object object = HttpHelper.deserializeJavaObjectFromStream(is);
                if (object != null) {
                    message.setBody(object);
                }
            } catch (Exception e) {
                throw new RuntimeCamelException("Cannot deserialize body to Java object", e);
            }
        }

        populateAttachments(request, message);
    }

    @Override
    protected void populateRequestParameters(HttpServletRequest request, HttpMessage message) throws Exception {
        //we populate the http request parameters without checking the request method

        HeaderFilterStrategy headerFilterStrategy = getHeaderFilterStrategy();


        Map<String, Object> headers = message.getHeaders();

        LOG.trace("HTTP method {} with Content-Type {}", request.getMethod(), request.getContentType());


        Enumeration names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            Object value = request.getParameter(name);
            LOG.trace("HTTP header {} = {}", name, value);


            if (headerFilterStrategy != null
                    && !headerFilterStrategy.applyFilterToExternalHeaders(name, value, message.getExchange())) {
                headers.put(name, value);
            }
        }


    }
}
