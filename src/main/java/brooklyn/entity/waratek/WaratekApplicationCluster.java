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
package brooklyn.entity.waratek;

import java.util.List;

import brooklyn.catalog.Catalog;
import brooklyn.config.ConfigKey;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.java.VanillaJavaApp;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.HasShortName;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.Sensors;
import brooklyn.util.flags.SetFromFlag;

@ImplementedBy(WaratekApplicationClusterImpl.class)
@Catalog(name="WaratekApplicationCluster", description="Clustered Waratek Java Application.", iconUrl="classpath://waratek-logo.png")
public interface WaratekApplicationCluster extends DynamicCluster, HasShortName {

    @SetFromFlag("args")
    ConfigKey<List> ARGS = VanillaJavaApp.ARGS;

    @SetFromFlag("main")
    ConfigKey<String> MAIN_CLASS = VanillaJavaApp.MAIN_CLASS;

    @SetFromFlag("classpath")
    ConfigKey<List> CLASSPATH = VanillaJavaApp.CLASSPATH;

    AttributeSensor<Integer> JVM_COUNT = Sensors.newIntegerSensor("waratek.jvmCount", "Number of JVMs");
    AttributeSensor<Integer> JVC_COUNT = Sensors.newIntegerSensor("waratek.jvcCount", "Number of JVCs");


}
