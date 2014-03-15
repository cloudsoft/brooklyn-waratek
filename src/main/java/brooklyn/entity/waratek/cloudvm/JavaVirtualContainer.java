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

import brooklyn.config.ConfigKey;
import brooklyn.entity.annotation.Effector;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.MethodEffector;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.HasShortName;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.Sensors;
import brooklyn.location.dynamic.LocationOwner;
import brooklyn.location.waratek.WaratekContainerLocation;
import brooklyn.util.flags.SetFromFlag;

@ImplementedBy(JavaVirtualContainerImpl.class)
public interface JavaVirtualContainer extends SoftwareProcess, HasShortName, LocationOwner<WaratekContainerLocation, JavaVirtualContainer> {

    String STATUS_RUNNING = "Running";
    String STATUS_SHUT_OFF = "Shut Off";
    String STATUS_PAUSED = "Paused";

    String DEFAULT_JVC_NAME_FORMAT = "jvc-brooklyn-%1$s";
    String ALTERNATIVE_JVC_NAME_FORMAT = "jvc-%2$d";

    @SetFromFlag("maxHeapSize")
    BasicAttributeSensorAndConfigKey<Long> MAX_HEAP_SIZE = new BasicAttributeSensorAndConfigKey(Long.class,
            "waratek.jvc.maxHeapSize", "Maximum heap memory to allocate (in bytes, default to 0; or unlimited)", 0L);

    @SetFromFlag("jvm")
    ConfigKey<JavaVirtualMachine> JVM = ConfigKeys.newConfigKey(JavaVirtualMachine.class, "waratek.jvm", "The parent JVM");

    ConfigKey<String> JVC_NAME_FORMAT = ConfigKeys.newStringConfigKey("waratek.jvc.nameFormat", "Format for generating JVC names", DEFAULT_JVC_NAME_FORMAT);

    AttributeSensor<String> JVC_NAME = Sensors.newStringSensor("waratek.jvc.name", "The name of the JVC");

    MethodEffector<Void> PAUSE = new MethodEffector<Void>(JavaVirtualContainer.class, "pause");
    MethodEffector<Void> RESUME = new MethodEffector<Void>(JavaVirtualContainer.class, "resume");
    MethodEffector<Long> ALLOCATE_HEAP = new MethodEffector<Long>(JavaVirtualContainer.class, "allocateHeap");

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

    JavaVirtualMachine getJavaVirtualMachine();

    String getLogFileLocation();

}
