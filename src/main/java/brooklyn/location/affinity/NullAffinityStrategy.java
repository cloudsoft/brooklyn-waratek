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

import java.util.Map;

import javax.annotation.Nullable;

import brooklyn.location.Location;

public class NullAffinityStrategy extends AbstractAffinityStrategy {

    public NullAffinityStrategy(Map<String, ?> properties) {
        super(properties);
    }

    @Override
    public int compare(Location o1, Location o2) {
        return 0;
    }

    @Override
    public boolean apply(@Nullable Location input) {
        return true;
    }

}
