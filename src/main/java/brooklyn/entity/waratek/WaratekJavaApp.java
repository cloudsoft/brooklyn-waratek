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

import java.util.Collection;
import java.util.List;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.BasicEntity;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.HasShortName;
import brooklyn.entity.trait.Startable;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.util.flags.SetFromFlag;

@ImplementedBy(WaratekJavaAppImpl.class)
public interface WaratekJavaApp extends BasicEntity, Startable, HasShortName {

    @SetFromFlag("args")
    ConfigKey<List> ARGS = JavaContainer.ARGS;

    @SetFromFlag("main")
    ConfigKey<String> MAIN_CLASS = JavaContainer.MAIN_CLASS;

    @SetFromFlag("classpath")
    ConfigKey<List> CLASSPATH = JavaContainer.CLASSPATH;

    @SetFromFlag("initialSize")
    ConfigKey<Integer> JVM_CLUSTER_SIZE = ConfigKeys.newConfigKeyWithPrefix("waratek.jvm", DynamicCluster.INITIAL_SIZE);

    @SetFromFlag("jvmSpec")
    BasicAttributeSensorAndConfigKey<EntitySpec> JVM_SPEC = new BasicAttributeSensorAndConfigKey<EntitySpec>(
            EntitySpec.class, "waratek.jvm.spec", "Specification to use when creating child JVMs",
            EntitySpec.create(JavaVM.class));

    Collection<Entity> getJvmList();

}
