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

import java.util.Comparator;
import java.util.SortedSet;

import brooklyn.entity.trait.Configurable;
import brooklyn.location.Location;

import com.google.common.annotations.Beta;
import com.google.common.base.Predicate;

@Beta
public interface AffinityRule extends Comparator<Location>, Predicate<Location>, Configurable {

    SortedSet<Location> checkLocations(Iterable<Location> locs);

}
