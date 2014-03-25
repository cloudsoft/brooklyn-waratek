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
import java.util.Map;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.Entity;
import brooklyn.entity.group.zoneaware.BalancingNodePlacementStrategy;
import brooklyn.entity.trait.Identifiable;
import brooklyn.location.Location;
import brooklyn.location.waratek.WaratekMachineLocation;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
        if (locs.isEmpty() && numToAdd > 0) {
            throw new IllegalArgumentException("No locations supplied, when requesting locations for "+numToAdd+" nodes");
        }

        List<Location> available = Lists.newArrayList(locs);
        int remaining = numToAdd;

        for (WaratekMachineLocation machine : Iterables.filter(locs, WaratekMachineLocation.class)) {
            int maxSize = machine.getOwner().getConfig(JavaVirtualMachine.JVC_CLUSTER_MAX_SIZE);
            int currentSize = machine.getOwner().getCurrentSize();
            remaining -= (maxSize - currentSize);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Requested {}/{}, Available JVMs: {}",
                    new Object[] { numToAdd, remaining, Iterables.toString(Iterables.transform(locs, identity())) });
        }

        if (remaining > 0) {
            // FIXME what happens if there are no locations available?
            WaratekMachineLocation machine = Iterables.filter(locs, WaratekMachineLocation.class).iterator().next();
            int maxSize = machine.getOwner().getConfig(JavaVirtualMachine.JVC_CLUSTER_MAX_SIZE);

            int delta = (remaining / maxSize) + (remaining % maxSize > 0 ? 1 : 0);
            Collection<Entity> added = machine.getWaratekInfrastructure().getVirtualMachineCluster().grow(delta);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Added {} JVMs: {}", delta, Iterables.toString(Iterables.transform(added, identity())));
            }
            // TODO wait until all JVMs have started up?
            Collection<Location> jvms = Collections2.transform(added, new Function<Entity, Location>() {
                @Override
                public Location apply(@Nullable Entity input) {
                    return ((JavaVirtualMachine) input).getDynamicLocation();
                }
            });
            available.addAll(jvms);
        }

        // Logic from parent, with enhancements.
        List<Location> result = Lists.newArrayList();
        Map<Location, Integer> locSizes = toAvailableLocationSizes(available);
        for (int i = 0; i < numToAdd; i++) {
            // TODO Inefficient to loop this many times! But not called with big numbers.
            Location leastPopulatedLoc = null;
            int leastPopulatedLocSize = 0;
            for (Location loc : locSizes.keySet()) {
                int locSize = locSizes.get(loc);
                if (leastPopulatedLoc == null || locSize < leastPopulatedLocSize) {
                    leastPopulatedLoc = loc;
                    leastPopulatedLocSize = locSize;
                }
            }
            Preconditions.checkState(leastPopulatedLoc != null, "leastPopulatedLoc was null; locs=%s", locSizes.keySet());
            result.add(leastPopulatedLoc);

            // Update population in locations, removing if maximum reached
            int maxSize = ((WaratekMachineLocation) leastPopulatedLoc).getOwner().getConfig(JavaVirtualMachine.JVC_CLUSTER_MAX_SIZE);
            int currentSize = locSizes.get(leastPopulatedLoc) + 1;
            if (currentSize < maxSize) {
                locSizes.put(leastPopulatedLoc, currentSize);
            } else {
                locSizes.remove(leastPopulatedLoc);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Placement for {} nodes: {}", numToAdd, Iterables.toString(Iterables.transform(result, identity())));
        }
        return result;
    }

    protected Map<Location,Integer> toAvailableLocationSizes(Iterable<? extends Location> locs) {
        Map<Location,Integer> result = Maps.newLinkedHashMap();
        for (Location loc : locs) {
            int currentSize = ((WaratekMachineLocation) loc).getOwner().getCurrentSize();
            result.put(loc, currentSize);
        }
        return result;
    }

    @Override
    public String toString() {
        return "Waratek aware NodePlacementStrategy";
    }

}