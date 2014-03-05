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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.catalog.Catalog;
import brooklyn.catalog.CatalogConfig;
import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.waratek.cloudvm.JavaVirtualMachine;
import brooklyn.entity.waratek.cloudvm.WaratekInfrastructure;
import brooklyn.util.flags.SetFromFlag;

/**
 * Brooklyn managed basic Waratek infrastructure.
 */
@Catalog(name="BasicWaratekInfrastructure",
    description="Deploys Simple Waratek Infrastructure.",
    iconUrl="classpath://waratek-logo.png")
public class BasicInfrastructure extends AbstractApplication {

    public static final Logger LOG = LoggerFactory.getLogger(BasicInfrastructure.class);

    @SetFromFlag("locationName")
    @CatalogConfig(label="Location Name", priority=0)
    public static final ConfigKey<String> LOCATION_NAME = WaratekInfrastructure.LOCATION_NAME;

    @SetFromFlag("runAs")
    @CatalogConfig(label="Separate User", priority=0)
    public static final ConfigKey<Boolean> USE_WARATEK_USER = JavaVirtualMachine.USE_WARATEK_USER;

    @SetFromFlag("debug")
    @CatalogConfig(label="Enable Debug", priority=0)
    public static final ConfigKey<Boolean> DEBUG = JavaVirtualMachine.DEBUG;

    @SetFromFlag("highAvailabilty")
    @CatalogConfig(label="Enable HA Policies", priority=0)
    public static final ConfigKey<Boolean> HA_POLICY_ENABLE = JavaVirtualMachine.HA_POLICY_ENABLE;

    @SetFromFlag("heapSize")
    @CatalogConfig(label="Heap Size", priority=1.1)
    public static final ConfigKey<Long> HEAP_SIZE = ConfigKeys.newConfigKeyWithDefault(JavaVirtualMachine.HEAP_SIZE, 1000000000L);

    @SetFromFlag("jvmClusterSize")
    @CatalogConfig(label="JVM Cluster Size", priority=1.2)
    public static final ConfigKey<Integer> JVM_CLUSTER_SIZE = WaratekInfrastructure.JVM_CLUSTER_SIZE;

    @SetFromFlag("jvcClusterSize")
    @CatalogConfig(label="JVC Cluster Size", priority=1.3)
    public static final ConfigKey<Integer> JVC_CLUSTER_SIZE = JavaVirtualMachine.JVC_CLUSTER_SIZE;

    @Override
    public void init() {
        EntitySpec jvmSpec = EntitySpec.create(JavaVirtualMachine.class)
                .configure(JavaVirtualMachine.USE_WARATEK_USER, getConfig(USE_WARATEK_USER))
                .configure(JavaVirtualMachine.DEBUG, getConfig(DEBUG))
                .configure(JavaVirtualMachine.HA_POLICY_ENABLE, getConfig(HA_POLICY_ENABLE))
                .configure(JavaVirtualMachine.JVC_CLUSTER_SIZE, getConfig(JVC_CLUSTER_SIZE))
                .configure(JavaVirtualMachine.JVC_CLUSTER_MAX_SIZE, 4) // TODO Make configurable
                .configure(JavaVirtualMachine.HEAP_SIZE, getConfig(HEAP_SIZE))
                .configure(JavaVirtualMachine.SSH_ADMIN_ENABLE, Boolean.TRUE);

        addChild(EntitySpec.create(WaratekInfrastructure.class)
                .configure(WaratekInfrastructure.LOCATION_NAME, getConfig(LOCATION_NAME))
                .configure(WaratekInfrastructure.JVM_CLUSTER_SIZE, getConfig(JVM_CLUSTER_SIZE))
                .configure(WaratekInfrastructure.JVM_SPEC, jvmSpec)
                .displayName("Waratek Infrastructure"));
    }

}
