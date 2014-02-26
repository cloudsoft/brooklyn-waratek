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
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.waratek.WaratekApplicationCluster;
import brooklyn.entity.waratek.WaratekJavaApplication;
import brooklyn.util.flags.SetFromFlag;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/** 
 * Brooklyn managed Waratek SimpleJavaApplication.
 */
@Catalog(name="SimpleJavaApplication",
    description="Deploys Simple Waratek Java Application.",
    iconUrl="classpath://waratek-logo.png")
public class SimpleJavaApplication extends AbstractApplication {

    public static final Logger LOG = LoggerFactory.getLogger(SimpleJavaApplication.class);

    @SetFromFlag("args")
    @CatalogConfig(label="Java Args", priority=0)
    public static final ConfigKey<List> ARGS = ConfigKeys.newConfigKeyWithDefault(WaratekJavaApplication.ARGS,
            ImmutableList.of(Integer.toString(1024 * 64))); // 64KiB

    @SetFromFlag(value="main")
    @CatalogConfig(label="Java Main Class", priority=0)
    public static final ConfigKey<String> MAIN_CLASS = ConfigKeys.newConfigKeyWithDefault(WaratekJavaApplication.MAIN_CLASS, "com.example.HelloWorld");

    @SetFromFlag("classpath")
    @CatalogConfig(label="Java Classpath", priority=0)
    public static final ConfigKey<List> CLASSPATH = ConfigKeys.newConfigKeyWithDefault(WaratekJavaApplication.CLASSPATH, ImmutableList.of("brooklyn-waratek-examples.jar"));

    @SetFromFlag("initialSize")
    @CatalogConfig(label="Cluster Size", priority=1)
    public static final ConfigKey<Integer> INITIAL_SIZE = ConfigKeys.newConfigKeyWithDefault(DynamicCluster.INITIAL_SIZE, 6);

    @Override
    public void init() {
        String mainClass = Iterables.getLast(Splitter.on(".").split(getConfig(MAIN_CLASS)));

        EntitySpec application = EntitySpec.create(WaratekJavaApplication.class)
                .configure(WaratekJavaApplication.ARGS, getConfig(ARGS))
                .configure(WaratekJavaApplication.MAIN_CLASS, getConfig(MAIN_CLASS))
                .configure(WaratekJavaApplication.CLASSPATH, getConfig(CLASSPATH))
                .configure(WaratekJavaApplication.JVM_DEFINES, Maps.<String, Object>newHashMap())
                .configure(WaratekJavaApplication.JVM_XARGS, Lists.<String>newArrayList());

        addChild(EntitySpec.create(WaratekApplicationCluster.class)
                .configure(DynamicCluster.INITIAL_SIZE, getConfig(INITIAL_SIZE))
                .configure(DynamicCluster.MEMBER_SPEC, application)
                .displayName("Waratek " + mainClass + " Application"));
    }

}
