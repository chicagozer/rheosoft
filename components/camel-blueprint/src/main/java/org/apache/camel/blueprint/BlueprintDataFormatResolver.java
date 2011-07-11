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
package org.apache.camel.blueprint;

import org.apache.camel.CamelContext;
import org.apache.camel.core.osgi.OsgiDataFormatResolver;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.DataFormatResolver;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlueprintDataFormatResolver extends OsgiDataFormatResolver {

    private static final transient Logger LOG = LoggerFactory.getLogger(BlueprintDataFormatResolver.class);

    public BlueprintDataFormatResolver(BundleContext bundleContext) {
        super(bundleContext);
    }

    @Override
    public DataFormat resolveDataFormat(String name, CamelContext context) {
        try {
            Object bean = context.getRegistry().lookup(".camelBlueprint.dataformatResolver." + name);
            if (bean instanceof DataFormatResolver) {
                LOG.debug("Found dataformat resolver: {} in registry: {}", name, bean);
                return ((DataFormatResolver) bean).resolveDataFormat(name, context);
            }
        } catch (Exception e) {
            LOG.debug("Ignored error looking up bean: " + name + ". Error: " + e);
        }
        return super.resolveDataFormat(name, context);
    }

}