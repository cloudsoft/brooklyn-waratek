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

import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.java.UsesJmx;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.HasShortName;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.Sensors;
import brooklyn.util.flags.SetFromFlag;

import com.google.common.reflect.TypeToken;

@ImplementedBy(JavaContainerImpl.class)
public interface JavaContainer extends SoftwareProcess, UsesJmx, HasShortName {

    String JVC_NAME_FORMAT = "jvc-brooklyn-%s";

    @SetFromFlag("heapSize")
    ConfigKey<Long> HEAP_SIZE = ConfigKeys.newLongConfigKey(
            "waratek.jvc.heap.size", "Size of heap memory to allocate (in bytes, default to 0; or unlimited)", 0L);

    @SetFromFlag("jvm")
    ConfigKey<JavaVM> JVM = ConfigKeys.newConfigKey(JavaVM.class,
            "waratek.jvc.jvm", "The parent JVM");
    
    @SetFromFlag("classpath")
    ConfigKey<List<String>> CLASSPATH = ConfigKeys.newConfigKey(new TypeToken<List<String>>() { },
            "waratek.jvc.classpath", "The JVC classpath to load");
    
    @SetFromFlag("mainClass")
    ConfigKey<String> MAIN_CLASS = ConfigKeys.newStringConfigKey(
            "waratek.jvc.mainClass", "The JVC main class to execute");

    AttributeSensor<String> JVC_NAME = Sensors.newStringSensor("waratek.jvc.name", "The name of the JVC");

    String getJvcName();

    JavaVM getJavaVM();

}
