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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.enricher.Enrichers;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.BasicStartableImpl;
import brooklyn.entity.basic.DynamicGroup;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.group.Cluster;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.location.Location;
import brooklyn.location.LocationDefinition;
import brooklyn.location.LocationSpec;
import brooklyn.location.basic.BasicLocationDefinition;
import brooklyn.location.waratek.WaratekLocation;
import brooklyn.management.LocationManager;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

public class WaratekInfrastructureImpl extends BasicStartableImpl implements WaratekInfrastructure {

    private static final Logger log = LoggerFactory.getLogger(WaratekInfrastructureImpl.class);

    private DynamicCluster virtualMachines;
    private DynamicGroup fabric;

    @Override
    public void init() {
        int initialSize = getConfig(JVM_CLUSTER_SIZE);
        EntitySpec jvmSpec = EntitySpec.create(getConfig(JVM_SPEC))
                .configure(JavaVirtualMachine.WARATEK_INFRASTRUCTURE, this);

        virtualMachines = addChild(EntitySpec.create(DynamicCluster.class)
                .configure(Cluster.INITIAL_SIZE, initialSize)
                .configure(DynamicCluster.QUARANTINE_FAILED_ENTITIES, false)
                .configure(DynamicCluster.MEMBER_SPEC, jvmSpec)
                .displayName("Java Virtual Machines"));

        fabric = addChild(EntitySpec.create(DynamicGroup.class)
                .configure(DynamicGroup.ENTITY_FILTER, Predicates.instanceOf(JavaVirtualContainer.class))
                .displayName("Java Virtual Containers"));

        if (Entities.isManaged(this)) {
            Entities.manage(virtualMachines);
            Entities.manage(fabric);
        }

        virtualMachines.addEnricher(Enrichers.builder()
                .aggregating(WaratekAttributes.TOTAL_HEAP_MEMORY)
                .computingSum()
                .fromMembers()
                .publishing(WaratekAttributes.TOTAL_HEAP_MEMORY)
                .build());
        virtualMachines.addEnricher(Enrichers.builder()
                .aggregating(WaratekAttributes.HEAP_MEMORY_DELTA_PER_SECOND_IN_WINDOW)
                .computingSum()
                .fromMembers()
                .publishing(WaratekAttributes.HEAP_MEMORY_DELTA_PER_SECOND_IN_WINDOW)
                .build());
        virtualMachines.addEnricher(Enrichers.builder()
                .aggregating(WaratekAttributes.AVERAGE_CPU_USAGE)
                .computingAverage()
                .fromMembers()
                .publishing(WaratekAttributes.AVERAGE_CPU_USAGE)
                .build());
        virtualMachines.addEnricher(Enrichers.builder()
                .aggregating(JVC_COUNT)
                .computingSum()
                .fromMembers()
                .publishing(JVC_COUNT)
                .build());

        addEnricher(Enrichers.builder()
                .propagating(WaratekAttributes.TOTAL_HEAP_MEMORY, JVC_COUNT, WaratekAttributes.AVERAGE_CPU_USAGE, WaratekAttributes.HEAP_MEMORY_DELTA_PER_SECOND_IN_WINDOW)
                .from(virtualMachines)
                .build());
        addEnricher(Enrichers.builder()
                .propagating(ImmutableMap.of(DynamicCluster.GROUP_SIZE, JVM_COUNT))
                .from(virtualMachines)
                .build());
    }

    @Override
    public List<Entity> getJvmList() {
        if (virtualMachines == null) {
            return ImmutableList.of();
        } else {
            return ImmutableList.copyOf(virtualMachines.getMembers());
        }
    }

    @Override
    public DynamicCluster getVirtualMachineCluster() { return virtualMachines; }

    @Override
    public List<Entity> getJvcList() {
        if (fabric == null) {
            return ImmutableList.of();
        } else {
            return ImmutableList.copyOf(fabric.getMembers());
        }
    }

    @Override
    public DynamicGroup getContainerFabric() { return fabric; }

    /**
     * Create a new {@link WaratekLocation} wrapping these locations.
     */
    @Override
    public void start(Collection<? extends Location> locations) {
        Location provisioner = Iterables.getOnlyElement(locations);
        log.info("Creating new WaratekLocation wrapping {}", provisioner);
        String locationName = getConfig(LOCATION_NAME);
        if (locationName == null) {
            String prefix = getConfig(LOCATION_PREFIX);
            locationName = prefix + getId();
        }
        LocationSpec<WaratekLocation> waratekSpec = LocationSpec.create(WaratekLocation.class)
                .configure("provisioner", provisioner)
                .configure("infrastructure", this)
                .displayName("Waratek(" + locationName + ")")
                .id(locationName);
        WaratekLocation waratekLocation = getManagementContext().getLocationManager().createLocation(waratekSpec);
        String locationSpec = String.format("waratek:%s:(name=\"%s\")", getId(), locationName);
        LocationDefinition definition = new BasicLocationDefinition(locationName, locationSpec, Maps.<String, Object>newHashMap());
        getManagementContext().getLocationRegistry().updateDefinedLocation(definition);
        log.info("New location {} created", waratekLocation);
        setAttribute(WARATEK_LOCATION, waratekLocation);
        super.start(locations);
    }

    /**
     * De-register our {@link WaratekLocation} and its children.
     */
    public void stop() {
        super.stop();
        LocationManager mgr = getManagementContext().getLocationManager();
        WaratekLocation waratek = getAttribute(WARATEK_LOCATION);
        if (waratek != null && mgr.isManaged(waratek)) {
            mgr.unmanage(waratek);
            setAttribute(WARATEK_LOCATION,  null);
        }
    }

}
