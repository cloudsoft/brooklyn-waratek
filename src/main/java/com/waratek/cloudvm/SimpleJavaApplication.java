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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.catalog.Catalog;
import brooklyn.catalog.CatalogConfig;
import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.waratek.JavaContainer;
import brooklyn.entity.waratek.JavaVM;
import brooklyn.entity.waratek.WaratekJavaApp;
import brooklyn.util.flags.SetFromFlag;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/** 
 * Brooklyn managed Waratek SimpleJavaApplication.
 */
@Catalog(name="SimpleJavaApplication",
    description="Deploys the Waratek Java SimpleJavaApplication.",
    iconUrl="classpath://com/waratek/waratek-logo.png")
public class SimpleJavaApplication extends AbstractApplication {

    public static final Logger LOG = LoggerFactory.getLogger(SimpleJavaApplication.class);

    @SetFromFlag("runAs")
    @CatalogConfig(label="Separate User", priority=0)
    public static final ConfigKey<Boolean> USE_WARATEK_USER = JavaVM.USE_WARATEK_USER;

    @SetFromFlag("debug")
    @CatalogConfig(label="Enable Debug", priority=0)
    public static final ConfigKey<Boolean> DEBUG = JavaVM.DEBUG;

    @SetFromFlag("highAvailabilty")
    @CatalogConfig(label="Enable HA Policies", priority=0)
    public static final ConfigKey<Boolean> HA_POLICY_ENABLE = JavaVM.HA_POLICY_ENABLE;

    @SetFromFlag("heapSize")
    @CatalogConfig(label="Heap Size", priority=1.1)
    public static final ConfigKey<Long> HEAP_SIZE = ConfigKeys.newConfigKeyWithDefault(JavaVM.HEAP_SIZE, 1000000000L);

    @SetFromFlag("args")
    @CatalogConfig(label="Java Args", priority=1.2)
    public static final ConfigKey<List> ARGS = ConfigKeys.newConfigKeyWithDefault(JavaContainer.ARGS,
            ImmutableList.of(Integer.toString(1024 * 64))); // 64KiB

    @SetFromFlag(value="main")
    @CatalogConfig(label="Java Main Class", priority=1.2)
    public static final ConfigKey<String> MAIN_CLASS = ConfigKeys.newConfigKeyWithDefault(JavaContainer.MAIN_CLASS, "com.example.HelloWorld");

    @SetFromFlag("classpath")
    @CatalogConfig(label="Java Classpath", priority=1.2)
    public static final ConfigKey<List> CLASSPATH = ConfigKeys.newConfigKeyWithDefault(JavaContainer.CLASSPATH, ImmutableList.of("brooklyn-waratek-examples.jar"));

    @SetFromFlag("jvmClusterSize")
    @CatalogConfig(label="JVM Cluster Size", priority=2.1)
    public static final ConfigKey<Integer> JVM_CLUSTER_SIZE = WaratekJavaApp.JVM_CLUSTER_SIZE;

    @SetFromFlag("jvcClusterSize")
    @CatalogConfig(label="JVC Cluster Size", priority=2.2)
    public static final ConfigKey<Integer> JVC_CLUSTER_SIZE = JavaVM.JVC_CLUSTER_SIZE;

    @Override
    public void init() {
        String mainClass = Iterables.getLast(Splitter.on(".").split(getConfig(MAIN_CLASS)));

        EntitySpec jvcSpec = EntitySpec.create(JavaContainer.class)
                .configure(JavaContainer.ARGS, getConfig(ARGS))
                .configure(JavaContainer.MAIN_CLASS, getConfig(MAIN_CLASS))
                .configure(JavaContainer.CLASSPATH, getConfig(CLASSPATH))
                .configure(JavaContainer.JVM_DEFINES, Maps.<String, Object>newHashMap())
                .configure(JavaContainer.JVM_XARGS, Lists.<String>newArrayList());

        EntitySpec jvmSpec = EntitySpec.create(JavaVM.class)
                .configure(JavaVM.USE_WARATEK_USER, getConfig(USE_WARATEK_USER))
                .configure(JavaVM.DEBUG, getConfig(DEBUG))
                .configure(JavaVM.HA_POLICY_ENABLE, getConfig(HA_POLICY_ENABLE))
                .configure(JavaVM.JVC_CLUSTER_SIZE, getConfig(JVC_CLUSTER_SIZE))
                .configure(JavaVM.JVC_CLUSTER_MAX_SIZE, 4) // TODO Make configurable
                .configure(JavaVM.HEAP_SIZE, getConfig(HEAP_SIZE))
                .configure(JavaVM.SSH_ADMIN_ENABLE, Boolean.TRUE)
                .configure(JavaVM.HTTP_ADMIN_ENABLE, Boolean.TRUE)
                .configure(JavaVM.JVC_SPEC, jvcSpec);

        addChild(EntitySpec.create(WaratekJavaApp.class)
                .configure(WaratekJavaApp.JVM_CLUSTER_SIZE, getConfig(JVM_CLUSTER_SIZE))
                .configure(WaratekJavaApp.JVM_SPEC, jvmSpec)
                .displayName("Waratek "+mainClass+" Application"));
    }

}
