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

        List<WaratekMachineLocation> available = Lists.newArrayList(Iterables.filter(locs,  WaratekMachineLocation.class));
        int remaining = numToAdd;
        for (WaratekMachineLocation machine : available) {
            remaining -= machine.getAvailableJvcCount();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Requested {}, Need {} more from new JVMs, Current JVMs {}",
                    new Object[] { numToAdd, remaining, Iterables.toString(Iterables.transform(locs, identity())) });
        }

        if (remaining > 0) {
            // FIXME what happens if there are no JVMs available?
            WaratekMachineLocation machine = Iterables.get(available, 0);

            // Grow the JVM cluster; based on max number of JVCs
            int maxSize = machine.getMaxSize();
            int delta = (remaining / maxSize) + (remaining % maxSize > 0 ? 1 : 0);
            Collection<Entity> added = machine.getWaratekInfrastructure().getVirtualMachineCluster().resizeByDelta(delta);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Added {} JVMs: {}", delta, Iterables.toString(Iterables.transform(added, identity())));
            }

            // Add the newly created locations for each JVM
            // TODO wait until all JVMs have started up?
            Collection<WaratekMachineLocation> jvms = Collections2.transform(added, new Function<Entity, WaratekMachineLocation>() {
                @Override
                public WaratekMachineLocation apply(@Nullable Entity input) {
                    return ((JavaVirtualMachine) input).getDynamicLocation();
                }
            });
            available.addAll(jvms);
        }

        // Logic from parent, with enhancements and types
        List<Location> result = Lists.newArrayList();
        Map<WaratekMachineLocation, Integer> sizes = toAvailableLocationSizes(available);
        for (int i = 0; i < numToAdd; i++) {
            WaratekMachineLocation smallest = null;
            int minSize = 0;
            for (WaratekMachineLocation loc : sizes.keySet()) {
                int size = sizes.get(loc);
                if (smallest == null || size < minSize) {
                    smallest = loc;
                    minSize = size;
                }
            }
            Preconditions.checkState(smallest != null, "smallest was null; locs=%s", sizes.keySet());
            result.add(smallest);

            // Update population in locations, removing if maximum reached
            int maxSize = smallest.getMaxSize();
            int currentSize = sizes.get(smallest) + 1;
            if (currentSize < maxSize) {
                sizes.put(smallest, currentSize);
            } else {
                sizes.remove(smallest);
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Placement for {} nodes: {}", numToAdd, Iterables.toString(Iterables.transform(result, identity())));
        }
        return result;
    }

    protected Map<WaratekMachineLocation, Integer> toAvailableLocationSizes(Iterable<WaratekMachineLocation> locs) {
        Map<WaratekMachineLocation, Integer> result = Maps.newLinkedHashMap();
        for (WaratekMachineLocation loc : locs) {
            result.put(loc, loc.getCurrentJvcCount());
        }
        return result;
    }

    @Override
    public String toString() {
        return "Waratek aware NodePlacementStrategy";
    }

}