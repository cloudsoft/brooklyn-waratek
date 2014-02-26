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
import brooklyn.entity.annotation.Effector;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.MethodEffector;
import brooklyn.entity.java.UsesJavaMXBeans;
import brooklyn.entity.java.VanillaJavaApp;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.HasShortName;
import brooklyn.entity.trait.Startable;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.Sensors;
import brooklyn.util.flags.SetFromFlag;

@ImplementedBy(JavaContainerImpl.class)
public interface JavaContainer extends VanillaJavaApp, Startable, UsesJavaMXBeans, HasShortName {

    String DEFAULT_JVC_NAME_FORMAT = "jvc-brooklyn-%1$s";
    String ALTERNATIVE_JVC_NAME_FORMAT = "jvc-%2$d";

    @SetFromFlag("maxHeapSize")
    BasicAttributeSensorAndConfigKey<Long> MAX_HEAP_SIZE = new BasicAttributeSensorAndConfigKey(Long.class,
            "waratek.jvc.maxHeapSize", "Maximum heap memory to allocate (in bytes, default to 0; or unlimited)", 0L);

    @SetFromFlag("jvm")
    ConfigKey<JavaVM> JVM = ConfigKeys.newConfigKey(JavaVM.class, "waratek.jvm", "The parent JVM");

    ConfigKey<String> JVC_NAME_FORMAT = ConfigKeys.newStringConfigKey("waratek.jvc.nameFormat", "Format for generating JVC names", DEFAULT_JVC_NAME_FORMAT);

    AttributeSensor<String> JVC_NAME = Sensors.newStringSensor("waratek.jvc.name", "The name of the JVC");

    AttributeSensor<Long> BYTES_SENT = Sensors.newLongSensor("waratek.jvc.bytesSent", "Total network bytes sent");
    AttributeSensor<Long> BYTES_RECEIVED = Sensors.newLongSensor("waratek.jvc.bytesReceived", "Total network bytes received");
    AttributeSensor<Integer> FILE_DESCRIPTOR_COUNT = Sensors.newIntegerSensor("waratek.jvc.fileDescriptorCount", "Current open file descriptors");
    AttributeSensor<Double> CPU_USAGE = Sensors.newDoubleSensor("waratek.jvc.cpuUsage", "Current CPU usage");
    AttributeSensor<String> STATUS = Sensors.newStringSensor("waratek.jvc.status", "Current JVC status");

    MethodEffector<Void> PAUSE = new MethodEffector<Void>(JavaContainer.class, "pause");
    MethodEffector<Void> RESUME = new MethodEffector<Void>(JavaContainer.class, "resume");
    MethodEffector<Long> ALLOCATE_HEAP = new MethodEffector<Long>(JavaContainer.class, "allocateHeap");

    /**
     * Pause the JVC.
     */
    @Effector(description="Pause the JVC")
    void pause();

    /**
     * Resume the JVC.
     */
    @Effector(description="Resume the JVC")
    void resume();

    /**
     * Change the allocated heap memory for this JVC.
     */
    @Effector(description="Change the allocated heap memory for this JVC")
    Long allocateHeap(@EffectorParam(name="size") Long size);

    String getJvcName();

    JavaVM getJavaVM();

}
