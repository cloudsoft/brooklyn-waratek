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

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.Entity;
import brooklyn.entity.group.zoneaware.BalancingNodePlacementStrategy;
import brooklyn.entity.trait.Identifiable;
import brooklyn.location.Location;
import brooklyn.location.waratek.WaratekMachineLocation;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

/**
 * Placement strategy that adds more JVMs if existing locations run out of capacity.
 *
 * @see BalancingNodePlacementStrategy
 */
public class WaratekNodePlacementStrategy extends BalancingNodePlacementStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(WaratekNodePlacementStrategy.class);

    public static final Function<Identifiable, String> identity() { return identity; }

    private static final Function<Identifiable, String> identity = new Function<Identifiable, String>() {
        @Override
        public String apply(@Nullable Identifiable input) {
            return input.getClass().getSimpleName() + ":" + input.getId();
        }
    };

    @Override
    public List<Location> locationsForAdditions(Multimap<Location, Entity> currentMembers, Collection<? extends Location> locs, int numToAdd) {
        ImmutableList.Builder<Location> result = ImmutableList.<Location>builder().addAll(locs);
        int remaining = numToAdd;

        for (WaratekMachineLocation machine : Iterables.filter(locs, WaratekMachineLocation.class)) {
            Integer maxSize = machine.getOwner().getConfig(JavaVirtualMachine.JVC_CLUSTER_MAX_SIZE);
            Integer currentSize = machine.getOwner().getCurrentSize();
            remaining -= (maxSize - currentSize);
        }

        if (remaining > 0) {
            WaratekMachineLocation machine = Iterables.filter(currentMembers.keySet(), WaratekMachineLocation.class).iterator().next();
            Integer maxSize = machine.getOwner().getConfig(JavaVirtualMachine.JVC_CLUSTER_MAX_SIZE);
            Collection<Entity> added = machine.getWaratekInfrastructure().getVirtualMachineCluster().grow(remaining / maxSize);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Added new JVMs: {}", Iterables.toString(Iterables.transform(added, identity())));
            }
            Iterable<Location> jvms = Iterables.transform(added, new Function<Entity, Location>() {
                @Override
                public Location apply(@Nullable Entity input) {
                    return ((JavaVirtualMachine) input).getDynamicLocation();
                }
            });
            result.addAll(jvms);
        }

        return super.locationsForAdditions(currentMembers, result.build(), numToAdd);
    }

}