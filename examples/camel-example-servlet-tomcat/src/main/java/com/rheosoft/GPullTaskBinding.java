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
package com.rheosoft;

import com.google.appengine.api.taskqueue.TaskOptions;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.component.gae.task.GTaskBinding;
import org.apache.camel.component.gae.task.GTaskEndpoint;
import org.apache.camel.component.http.HttpMessage;

/**
 * Binds the {@link TaskOptions} of the task queueing service to a Camel
 * {@link Exchange} for outbound communication. For inbound communication a
 * {@link HttpMessage} is bound to {@link Exchange}.
 */
public class GPullTaskBinding extends GTaskBinding {

    @Override
    protected void writeRequestHeaders(GTaskEndpoint endpoint, Exchange exchange, TaskOptions request) {
        // dont write headers!!!
    }

    @Override
    public TaskOptions writeRequest(GTaskEndpoint endpoint, Exchange exchange, TaskOptions request) {
        //TaskOptions answer = TaskOptions.Builder.withUrl(getWorkerRoot(endpoint) + endpoint.getPath());
        TaskOptions answer = TaskOptions.Builder.withMethod(TaskOptions.Method.PULL);
        //writeRequestHeaders(endpoint, exchange, answer);
        writeRequestBody(endpoint, exchange, answer);
        
        // flush the headers!!
        Map<java.lang.String,java.lang.String> map = new HashMap<java.lang.String,java.lang.String>();
        return answer.headers(map);
    }
}
