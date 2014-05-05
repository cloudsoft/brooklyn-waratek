/*
 * Copyright 2014 by Cloudsoft Corporation Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.waratek.cloudvm;

import brooklyn.catalog.Catalog;
import brooklyn.catalog.CatalogConfig;
import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.StartableApplication;
import brooklyn.entity.java.UsesJmx;
import brooklyn.entity.java.UsesJmx.JmxAgentModes;
import brooklyn.entity.nosql.solr.SolrServer;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.location.basic.PortRanges;

import com.google.common.collect.ImmutableMap;

/**
 * Single-node Solr instance.
 */
@Catalog(name="Solr",
        description="Solr Server.",
        iconUrl="classpath://solr-logo.png")
public class SolrApplication extends AbstractApplication implements StartableApplication {

    public static final String DEFAULT_CORE_CONFIG = "https://s3-eu-west-1.amazonaws.com/brooklyn-waratek/example.tgz";

    @CatalogConfig(label="Core Configuration (URL)", priority=0)
    public static final ConfigKey<String> CORE_CONFIG_PATH = ConfigKeys.newConfigKey(
            "solr.config", "URL to the Solr core configuration archive", DEFAULT_CORE_CONFIG);

    @Override
    public void init() {
        addChild(EntitySpec.create(SolrServer.class)
                .displayName("Solr Server")
                .configure(SolrServer.SOLR_PORT, PortRanges.fromString("8080+"))
                .configure(SolrServer.SOLR_CORE_CONFIG, ImmutableMap.of("example", Entities.getRequiredUrlConfig(this, CORE_CONFIG_PATH)))
                .configure(UsesJmx.USE_JMX, Boolean.TRUE)
                .configure(UsesJmx.JMX_AGENT_MODE, JmxAgentModes.JMXMP)
                .configure(UsesJmx.JMX_PORT, PortRanges.fromString("30000+")));
    }

}
