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
import brooklyn.entity.basic.DynamicGroup;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.Resizable;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.location.dynamic.LocationOwner;
import brooklyn.location.jclouds.JcloudsLocationConfig;
import brooklyn.location.waratek.WaratekLocation;
import brooklyn.util.flags.SetFromFlag;

@ImplementedBy(WaratekInfrastructureImpl.class)
@Catalog(name="WaratekInfrastructure", description="Waratek CloudVM Infrastructure.", iconUrl="classpath://waratek-logo.png")
public interface WaratekInfrastructure extends BasicStartable, Resizable, LocationOwner<WaratekLocation, WaratekInfrastructure> {

    @SetFromFlag("securityGroup")
    ConfigKey<String> SECURITY_GROUP = ConfigKeys.newStringConfigKey(
            "waratek.jvm.securityGroup", "Set a network security group for cloud servers to use; (null to use default configuration)");

    @SetFromFlag("openIptables")
    ConfigKey<Boolean> OPEN_IPTABLES = ConfigKeys.newConfigKeyWithPrefix("waratek.jvm.", JcloudsLocationConfig.OPEN_IPTABLES);

    @SetFromFlag("registerJvms")
    ConfigKey<Boolean> REGISTER_JVM_LOCATIONS = ConfigKeys.newBooleanConfigKey("waratek.jvm.register", "Register new JVM locations for deployment", Boolean.FALSE);

    @SetFromFlag("minJvm")
    ConfigKey<Integer> JVM_CLUSTER_MIN_SIZE = ConfigKeys.newConfigKeyWithPrefix("waratek.jvm.", DynamicCluster.INITIAL_SIZE);

    @SetFromFlag("maxJvc")
    ConfigKey<Integer> JVC_CLUSTER_MAX_SIZE = ConfigKeys.newIntegerConfigKey("waratek.jvc.maxSize", "Maximum size of a JVC cluster", 4);

    @SetFromFlag("jvmSpec")
    BasicAttributeSensorAndConfigKey<EntitySpec> JVM_SPEC = new BasicAttributeSensorAndConfigKey<EntitySpec>(
            EntitySpec.class, "waratek.jvm.spec", "Specification to use when creating child JVMs",
            EntitySpec.create(JavaVirtualMachine.class));

    AttributeSensor<Integer> JVM_COUNT = WaratekAttributes.JVM_COUNT;
    AttributeSensor<Integer> JVC_COUNT = WaratekAttributes.JVC_COUNT;

    List<Entity> getJvmList();

    DynamicCluster getVirtualMachineCluster();

    List<Entity> getJvcList();

    DynamicGroup getContainerFabric();
}
