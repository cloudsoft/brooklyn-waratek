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
package brooklyn.location;

import java.util.Map;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.Sensors;
import brooklyn.util.flags.SetFromFlag;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;

public interface LocationOwner<L extends Location & DynamicLocation<E, L>, E extends Entity & LocationOwner<L, E>> {

    @SetFromFlag("locationPrefix")
    ConfigKey<String> LOCATION_NAME_PREFIX = ConfigKeys.newStringConfigKey(
            "entity.dynamicLocation.prefix", "The name prefix for the location owned by this entity", "dynamic");

    @SetFromFlag("locationSuffix")
    ConfigKey<String> LOCATION_NAME_SUFFIX = ConfigKeys.newStringConfigKey(
            "entity.dynamicLocation.suffix", "The name suffix for the location owned by this entity");

    @SetFromFlag("locationName")
    BasicAttributeSensorAndConfigKey<String> LOCATION_NAME = new BasicAttributeSensorAndConfigKey<String>(String.class,
            "entity.dynamicLocation.name", "The name of the location owned by this entity (default is auto-generated using prefix and suffix keys)");

    ConfigKey<Map<String, Object>> LOCATION_FLAGS = ConfigKeys.newConfigKey(new TypeToken<Map<String, Object>>() { },
            "entity.dynamicLocation.flags", "Extra creation flags for the Location owned by this entity",
            ImmutableMap.<String, Object>of());

    AttributeSensor<Location> DYNAMIC_LOCATION = Sensors.newSensor(Location.class,
            "entity.dynamicLocation", "The location owned by this entity");

    AttributeSensor<String> LOCATION_SPEC = Sensors.newStringSensor(
            "entity.dynamicLocation.spec", "The specification string for the location owned by this entity");

    AttributeSensor<Boolean> DYNAMIC_LOCATION_STATUS = Sensors.newBooleanSensor(
            "entity.dynamicLocation.status", "The status of the location owned by this entity");

    L getDynamicLocation();

    L createLocation(Map<String, ?> flags);

    boolean isLocationAvailable();

}
