package brooklyn.entity.waratek.cloudvm;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import brooklyn.entity.Entity;
import brooklyn.entity.group.zoneaware.BalancingNodePlacementStrategy;
import brooklyn.location.Location;
import brooklyn.location.waratek.WaratekMachineLocation;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * Placement strategy that adds more JVMs if existing locations run out of capacity.
 *
 * @see BalancingNodePlacementStrategy
 */
public class WaratekNodePlacementStrategy extends BalancingNodePlacementStrategy {

    @Override
    public List<Location> locationsForAdditions(Multimap<Location, Entity> currentMembers, Collection<? extends Location> locs, int numToAdd) {
        ImmutableList.Builder<Location> result = ImmutableList.builder();
        List<Location> balanced = super.locationsForAdditions(currentMembers, locs, numToAdd);
        int remaining = numToAdd;
        for (Location loc : balanced) {
            WaratekMachineLocation machine = (WaratekMachineLocation) loc;
            Integer maxSize = machine.getOwner().getConfig(JavaVirtualMachine.JVC_CLUSTER_MAX_SIZE);
            Integer currentSize = machine.getOwner().getCurrentSize();
            remaining -= (maxSize - currentSize);
        }
        if (remaining > 0) {
            WaratekMachineLocation machine = Iterables.filter(currentMembers.keySet(), WaratekMachineLocation.class).iterator().next();
            Integer maxSize = machine.getOwner().getConfig(JavaVirtualMachine.JVC_CLUSTER_MAX_SIZE);
            Collection<Entity> added = machine.getWaratekInfrastructure().getVirtualMachineCluster().grow(remaining / maxSize);
            Iterable<Location> jvms = Iterables.transform(added, new Function<Entity, Location>() {
                @Override
                public Location apply(@Nullable Entity input) {
                    return ((JavaVirtualMachine) input).getDynamicLocation();
                }
            });
            result.addAll(jvms);
        }
        result.addAll(balanced);
        return result.build();
    }

    @Override
    protected Map<Location,Integer> toMutableLocationSizes(Multimap<Location, Entity> currentMembers, Iterable<? extends Location> otherLocs) {
        Map<Location,Integer> locationSizes = super.toMutableLocationSizes(currentMembers, otherLocs);
        Map<Location,Integer> machineSizes = Maps.filterKeys(locationSizes, Predicates.instanceOf(WaratekMachineLocation.class));
        return Maps.filterEntries(machineSizes, new Predicate<Map.Entry<Location, Integer>>() {
            @Override
            public boolean apply(@Nullable Entry<Location, Integer> input) {
                WaratekMachineLocation machine = (WaratekMachineLocation) input.getKey();
                Integer maxSize = machine.getOwner().getConfig(JavaVirtualMachine.JVC_CLUSTER_MAX_SIZE);
                return input.getValue() < maxSize;
            }
        });
    }
}