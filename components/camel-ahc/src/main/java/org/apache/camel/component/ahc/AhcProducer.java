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
package org.apache.camel.component.ahc;

import java.io.ByteArrayOutputStream;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.Request;
import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultAsyncProducer;

/**
 *
 */
public class AhcProducer extends DefaultAsyncProducer {

    private final AsyncHttpClient client;

    public AhcProducer(AhcEndpoint endpoint) {
        super(endpoint);
        this.client = endpoint.getClient();
    }

    @Override
    public AhcEndpoint getEndpoint() {
        return (AhcEndpoint) super.getEndpoint();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean process(Exchange exchange, AsyncCallback callback) {
        try {
            // AHC supports async processing
            Request request = getEndpoint().getBinding().prepareRequest(getEndpoint(), exchange);
            log.debug("Executing request {} ", request);
            client.prepareRequest(request).execute(new AhcAsyncHandler(exchange, callback, request.getUrl()));
            return false;
        } catch (Exception e) {
            exchange.setException(e);
            callback.done(true);
            return true;
        }
    }

    /**
     * Camel {@link AsyncHandler} to receive callbacks during the processing of the request.
     */
    private final class AhcAsyncHandler implements AsyncHandler<Exchange> {

        private final Exchange exchange;
        private final AsyncCallback callback;
        private final String url;
        private final ByteArrayOutputStream os;
        private int contentLength;
        private int statusCode;
        private String statusText;

        private AhcAsyncHandler(Exchange exchange, AsyncCallback callback, String url) {
            this.exchange = exchange;
            this.callback = callback;
            this.url = url;
            this.os = new ByteArrayOutputStream();
        }

        @Override
        public void onThrowable(Throwable t) {
            log.trace("{} onThrowable {}", exchange.getExchangeId(), t);
            try {
                getEndpoint().getBinding().onThrowable(getEndpoint(), exchange, t);
            } catch (Exception e) {
                exchange.setException(e);
            }
            callback.done(false);
        }

        @Override
        public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
            // write body parts to stream, which we will bind to the Camel Exchange in onComplete
            int wrote = bodyPart.writeTo(os);
            log.trace("{} onBodyPartReceived {} bytes", exchange.getExchangeId(), wrote);
            contentLength += wrote;
            return STATE.CONTINUE;
        }

        @Override
        public STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
            log.trace("{} onStatusReceived {}", exchange.getExchangeId(), responseStatus);
            try {
                statusCode = responseStatus.getStatusCode();
                statusText = responseStatus.getStatusText();
                getEndpoint().getBinding().onStatusReceived(getEndpoint(), exchange, responseStatus);
            } catch (Exception e) {
                exchange.setException(e);
            }
            return STATE.CONTINUE;
        }

        @Override
        public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
            log.trace("{} onHeadersReceived {}", exchange.getExchangeId(), headers);
            try {
                getEndpoint().getBinding().onHeadersReceived(getEndpoint(), exchange, headers);
            } catch (Exception e) {
                exchange.setException(e);
            }
            return STATE.CONTINUE;
        }

        @Override
        public Exchange onCompleted() throws Exception {
            log.trace("{} onCompleted", exchange.getExchangeId());
            try {
                getEndpoint().getBinding().onComplete(getEndpoint(), exchange, url, os, contentLength, statusCode, statusText);
            } catch (Exception e) {
                exchange.setException(e);
            } finally {
                // signal we are done
                callback.done(false);
            }
            return exchange;
        }

        @Override
        public String toString() {
            return "AhcAsyncHandler for exchangeId: " + exchange.getExchangeId() + " -> " + url;
        }
    }

}
