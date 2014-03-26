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
package brooklyn.location.affinity;

import java.util.List;

import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.ConfigKeys;

import com.google.common.annotations.Beta;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

@Beta
public interface LocationAffinityConfig {

    ConfigKey<Boolean> ENABLE_AFFINITY = ConfigKeys.newBooleanConfigKey(
            "location.affinity.enable", "Enable the use of affinity rules for a location",
            Boolean.FALSE);

    ConfigKey<List<AffinityRule>> AFFINITY_RULES = ConfigKeys.newConfigKey(
            new TypeToken<List<AffinityRule>>() { },
            "location.affinity.rules", "List of affinity rules for a location",
            Lists.<AffinityRule>newArrayList());

    ConfigKey<Boolean> DEFAULT_EMPTY_LOCATION_ALLOW = ConfigKeys.newBooleanConfigKey(
            "location.affinity.emptyLocationAllow", "Allow adding entities to empty locations regardless of rules",
            Boolean.FALSE);

}
