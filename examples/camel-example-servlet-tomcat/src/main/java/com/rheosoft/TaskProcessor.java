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

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskHandle;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class TaskProcessor implements Processor {

    private static final Logger LOGGER = Logger.getLogger(TaskProcessor.class.getName());
    
    @Override
    public void process(Exchange exchange) throws Exception {
        LOGGER.finest("entered process");

        
        Queue q = QueueFactory.getQueue("pull-queue");
        List<TaskHandle> tasks = q.leaseTasks(3600, TimeUnit.SECONDS, 1);
        if (!tasks.isEmpty())
            {
                TaskHandle t = tasks.get(0);
                exchange.getIn().setHeader("taskETA", t.getEtaMillis());
                exchange.getIn().setHeader("taskName", t.getName());
                exchange.getIn().setHeader("taskQueueName", t.getQueueName());
                exchange.getIn().setHeader("taskRetryCount", t.getRetryCount());
                exchange.getIn().setBody(t.getPayload());
                q.deleteTask(t);
            }
            
    }

}
