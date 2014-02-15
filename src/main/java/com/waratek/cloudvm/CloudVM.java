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

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.catalog.Catalog;
import brooklyn.catalog.CatalogConfig;
import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.waratek.JavaContainer;
import brooklyn.entity.waratek.JavaVM;
import brooklyn.util.flags.SetFromFlag;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;

/** 
 * Brooklyn managed Waratek CloudVM.
 */
@Catalog(name="CloudVM",
    description="Deploys the Waratek Java CloudVM.",
    iconUrl="classpath://com/waratek/waratek-logo.png")
public class CloudVM extends AbstractApplication {

    public static final Logger LOG = LoggerFactory.getLogger(CloudVM.class);

    @SetFromFlag("runAs")
    @CatalogConfig(label="Separate User", priority=0)
    public static final ConfigKey<Boolean> USE_WARATEK_USER = ConfigKeys.newBooleanConfigKey(
            "waratek.runAs", "Run the CloudVM as a the waratek user (default true)",
            Boolean.TRUE);

    @SetFromFlag("debug")
    @CatalogConfig(label="Enable Debug", priority=0)
    public static final ConfigKey<Boolean> DEBUG = ConfigKeys.newBooleanConfigKey(
            "waratek.debug", "Enable debug options (default false)",
            Boolean.FALSE);

    @SetFromFlag("highAvailabilty")
    @CatalogConfig(label="Enable HA Policies", priority=0)
    public static final ConfigKey<Boolean> HA_POLICY_ENABLE = ConfigKeys.newBooleanConfigKey(
            "waratek.policy.ha", "Enable high-availability and resilience/restart policies (default false)",
            Boolean.FALSE);

    @SetFromFlag("heapSize")
    @CatalogConfig(label="Heap Size", priority=1.1)
    public static final ConfigKey<Long> HEAP_SIZE = ConfigKeys.newLongConfigKey(
            "waratek.heap.size", "Amount of memory to allocate to the CloudVM (in bytes, default 1GB)",
            1000000000L);

    @SetFromFlag("args")
    @CatalogConfig(label="Java Args", priority=1.2)
    public static final ConfigKey<List> ARGS = ConfigKeys.<List>newConfigKey(List.class,
            "waratek.javaApp.args", "Arguments for the application");

    @SetFromFlag(value="main")
    @CatalogConfig(label="Java Main Class", priority=1.2)
    public static final ConfigKey<String> MAIN_CLASS = ConfigKeys.newStringConfigKey("vanillaJavaApp.mainClass", "Java class to launch");

    @SetFromFlag("classpath")
    @CatalogConfig(label="Java Classpath", priority=1.2)
    public static final ConfigKey<List<String>> CLASSPATH = ConfigKeys.<List<String>>newConfigKey(new TypeToken<List<String>>() { },
            "waratek.javaApp.classpath", "Java classpath for the application");

    @SetFromFlag("initialSize")
    @CatalogConfig(label="Cluster Size", priority=2)
    public static final ConfigKey<Integer> JVC_CLUSTER_SIZE = ConfigKeys.newConfigKeyWithDefault(DynamicCluster.INITIAL_SIZE, 1);

    @Override
    public void init() {
        EntitySpec jvcSpec = EntitySpec.create(JavaContainer.class)
                .configure(JavaContainer.ARGS, getConfig(ARGS))
                .configure(JavaContainer.MAIN_CLASS, getConfig(MAIN_CLASS))
                .configure(JavaContainer.CLASSPATH, getConfig(CLASSPATH))
                .configure(JavaContainer.JVM_DEFINES, Maps.<String, Object>newHashMap())
                .configure(JavaContainer.JVM_XARGS, Lists.<String>newArrayList());

        addChild(EntitySpec.create(JavaVM.class)
                .configure(JavaVM.WARATEK_USER, getConfig(USE_WARATEK_USER))
                .configure(JavaVM.DEBUG, getConfig(DEBUG))
                .configure(JavaVM.HA_POLICY_ENABLE, getConfig(HA_POLICY_ENABLE))
                .configure(JavaVM.JVC_CLUSTER_SIZE, getConfig(JVC_CLUSTER_SIZE))
                .configure(JavaVM.HEAP_SIZE, getConfig(HEAP_SIZE))
                .configure(JavaVM.SSH_ADMIN_ENABLE, Boolean.TRUE)
                .configure(JavaVM.HTTP_ADMIN_ENABLE, Boolean.TRUE)
                .configure(JavaVM.JVC_SPEC, jvcSpec)
                .displayName("Waratek CloudVM"));
    }

}
