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

import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.java.UsesJmx;
import brooklyn.entity.java.VanillaJavaApp;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.HasShortName;
import brooklyn.entity.trait.Startable;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.Sensors;
import brooklyn.util.flags.SetFromFlag;

@ImplementedBy(JavaContainerImpl.class)
public interface JavaContainer extends VanillaJavaApp, Startable, UsesJmx, HasShortName {

    String DEFAULT_JVC_NAME_FORMAT = "jvc-brooklyn-%1$s";
    String ALTERNATIVE_JVC_NAME_FORMAT = "jvc-brooklyn-%2$d";

    @SetFromFlag("heapSize")
    ConfigKey<Long> HEAP_SIZE = ConfigKeys.newLongConfigKey(
            "waratek.jvc.heap.size", "Size of heap memory to allocate (in bytes, default to 0; or unlimited)", 0L);

    @SetFromFlag("jvm")
    ConfigKey<JavaVM> JVM = ConfigKeys.newConfigKey(JavaVM.class, "waratek.jvm", "The parent JVM");

    ConfigKey<String> JVC_NAME_FORMAT = ConfigKeys.newStringConfigKey("waratek.jvc.nameFormat", "Format for generating JVC names", DEFAULT_JVC_NAME_FORMAT);

    AttributeSensor<String> JVC_NAME = Sensors.newStringSensor("waratek.jvc.name", "The name of the JVC");

    String getJvcName();

    JavaVM getJavaVM();

}
