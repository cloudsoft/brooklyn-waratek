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

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.Entity;
import brooklyn.entity.group.DynamicCluster.NodePlacementStrategy;
import brooklyn.location.Location;

import com.google.common.collect.Multimap;

/**
 * Placement strategy that checks {@link AffinityRule}s on the {@link Location}s.
 */
public class AffinityRulePlacementStrategy implements NodePlacementStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(AffinityRulePlacementStrategy.class);

    @Override
    public List<Location> locationsForAdditions(Multimap<Location, Entity> currentMembers, Collection<? extends Location> locs, int numToAdd) {
        return null;
    }

    @Override
    public List<Entity> entitiesToRemove(Multimap<Location, Entity> currentMembers, int numToRemove) {
        return null;
    }

}