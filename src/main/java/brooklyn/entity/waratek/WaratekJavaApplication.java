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
import brooklyn.entity.basic.BrooklynConfigKeys;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.java.VanillaJavaApp;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.util.flags.SetFromFlag;
import brooklyn.util.time.Duration;

@ImplementedBy(WaratekJavaApplicationImpl.class)
public interface WaratekJavaApplication extends VanillaJavaApp {

    @SetFromFlag("startTimeout")
    ConfigKey<Duration> START_TIMEOUT = ConfigKeys.newConfigKeyWithDefault(BrooklynConfigKeys.START_TIMEOUT, Duration.FIVE_MINUTES);

    @SetFromFlag("maxHeapSize")
    BasicAttributeSensorAndConfigKey<Long> MAX_HEAP_SIZE = new BasicAttributeSensorAndConfigKey(Long.class,
            "waratek.jvc.maxHeapSize", "Maximum heap memory to allocate (in bytes, default to 0; or unlimited)", 0L);

    @SetFromFlag("jvm")
    ConfigKey<String> JVM = ConfigKeys.newStringConfigKey("waratek.jvm.name", "The parent JVM");

    @SetFromFlag("jvc")
    ConfigKey<String> JVC = ConfigKeys.newStringConfigKey("waratek.jvc.name", "The parent JVM");

}
