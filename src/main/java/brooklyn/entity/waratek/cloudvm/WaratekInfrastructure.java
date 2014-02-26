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
package brooklyn.entity.waratek.cloudvm;

import java.util.List;

import brooklyn.catalog.Catalog;
import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.BasicStartable;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.Sensors;
import brooklyn.location.waratek.WaratekLocation;
import brooklyn.util.flags.SetFromFlag;

@ImplementedBy(WaratekInfrastructureImpl.class)
@Catalog(name="WaratekInfrastructure", description="Waratek CloudVM Infrastructure.", iconUrl="classpath://waratek-logo.png")
public interface WaratekInfrastructure extends BasicStartable {

    @SetFromFlag("locationName")
    ConfigKey<String> LOCATION_NAME = ConfigKeys.newStringConfigKey(
            "waratek.location.name", "Name for new Waratek location");

    @SetFromFlag("locationPrefix")
    ConfigKey<String> LOCATION_PREFIX = ConfigKeys.newStringConfigKey(
            "waratek.location.prefix", "Prefix for new Waratek location (will have entity id appended)",
            WaratekLocation.PREFIX);

    @SetFromFlag("initialSize")
    ConfigKey<Integer> JVM_CLUSTER_SIZE = ConfigKeys.newConfigKeyWithPrefix("waratek.jvm", DynamicCluster.INITIAL_SIZE);

    @SetFromFlag("jvmSpec")
    BasicAttributeSensorAndConfigKey<EntitySpec> JVM_SPEC = new BasicAttributeSensorAndConfigKey<EntitySpec>(
            EntitySpec.class, "waratek.jvm.spec", "Specification to use when creating child JVMs",
            EntitySpec.create(JavaVirtualMachine.class));

    AttributeSensor<WaratekLocation> WARATEK_LOCATION = Sensors.newSensor(WaratekLocation.class,
            "waratek.location", "The Waratek location associated with this infrastructure");

    AttributeSensor<Integer> JVM_COUNT = WaratekAttributes.JVM_COUNT;
    AttributeSensor<Integer> JVC_COUNT = WaratekAttributes.JVC_COUNT;

    List<Entity> getJvmList();

    DynamicCluster getVirtualMachineCluster();

    List<Entity> getJvcList();

    WaratekFabric getContainerFabric();
}
