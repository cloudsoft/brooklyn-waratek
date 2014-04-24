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

import brooklyn.enricher.Enrichers;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.BasicStartableImpl;
import brooklyn.entity.basic.DynamicGroup;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.basic.SoftwareProcess.ChildStartableMode;
import brooklyn.entity.group.Cluster;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.group.DynamicMultiGroup;
import brooklyn.entity.java.UsesJmx;
import brooklyn.entity.java.UsesJmx.JmxAgentModes;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.event.SensorEvent;
import brooklyn.event.SensorEventListener;
import brooklyn.location.Location;
import brooklyn.location.LocationDefinition;
import brooklyn.location.basic.BasicLocationDefinition;
import brooklyn.location.waratek.WaratekContainerLocation;
import brooklyn.location.waratek.WaratekLocation;
import brooklyn.location.waratek.WaratekResolver;
import brooklyn.management.LocationManager;
import brooklyn.util.collections.MutableMap;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

public class WaratekInfrastructureImpl extends BasicStartableImpl implements WaratekInfrastructure {

    static {
        WaratekAttributes.init();
    }

    private static final Logger log = LoggerFactory.getLogger(WaratekInfrastructureImpl.class);

    private DynamicCluster virtualMachines;
    private DynamicGroup fabric;
    private DynamicMultiGroup buckets;
    private WaratekLocation waratek;

    private Predicate<Entity> sameInfrastructure = new Predicate<Entity>() {
        @Override
        public boolean apply(@Nullable Entity input) {
            // Check if entity is deployed to a WaratekContainerLocation
            Optional<Location> lookup = Iterables.tryFind(input.getLocations(), Predicates.instanceOf(WaratekContainerLocation.class));
            if (lookup.isPresent()) {
                WaratekContainerLocation container = (WaratekContainerLocation) lookup.get();
                // Only containers that are part of this infrastructure
                return getId().equals(container.getJavaVirtualMachine().getInfrastructure().getId());
            } else {
                return false;
            }
        }
    };

    @Override
    public void init() {
        int initialSize = getConfig(JVM_CLUSTER_MIN_SIZE);
        EntitySpec<?> jvmSpec = EntitySpec.create(getConfig(JVM_SPEC))
                .configure(JavaVirtualMachine.WARATEK_INFRASTRUCTURE, this)
                .configure(UsesJmx.USE_JMX, Boolean.TRUE)
                .configure(UsesJmx.JMX_AGENT_MODE, JmxAgentModes.JMX_RMI_CUSTOM_AGENT)
                .configure(SoftwareProcess.CHILDREN_STARTABLE_MODE, ChildStartableMode.BACKGROUND_LATE);

        virtualMachines = addChild(EntitySpec.create(DynamicCluster.class)
                .configure(Cluster.INITIAL_SIZE, initialSize)
                .configure(DynamicCluster.QUARANTINE_FAILED_ENTITIES, true)
                .configure(DynamicCluster.MEMBER_SPEC, jvmSpec)
                .displayName("Java Virtual Machines"));

        fabric = addChild(EntitySpec.create(DynamicGroup.class)
                .configure(DynamicGroup.ENTITY_FILTER, Predicates.instanceOf(JavaVirtualContainer.class))
                .displayName("All Java Virtual Containers"));

        buckets = addChild(EntitySpec.create(DynamicMultiGroup.class)
                .configure(DynamicMultiGroup.ENTITY_FILTER, sameInfrastructure)
                .configure(DynamicMultiGroup.RESCAN_INTERVAL, 15L)
                .configure(DynamicMultiGroup.BUCKET_SPEC, EntitySpec.create(WaratekApplicationGroup.class))
                .configure(DynamicMultiGroup.BUCKET_FUNCTION, new Function<Entity, String>() {
                        @Override
                        public String apply(@Nullable Entity input) {
                            return input.getApplication().getDisplayName();
                        }
                    })
                .displayName("Waratek Java Applications"));

        if (Entities.isManaged(this)) {
            Entities.manage(virtualMachines);
            Entities.manage(fabric);
            Entities.manage(buckets);
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

        subscribeToMembers(virtualMachines, SERVICE_UP, new SensorEventListener<Boolean>() {
            @Override
            public void onEvent(SensorEvent<Boolean> event) {
                setAttribute(SERVICE_UP, calculateServiceUp());
            }
        });
    }

    private boolean calculateServiceUp() {
        List<Entity> jvms = getJvmList();
        if (jvms.isEmpty()) return true;
        for (Entity jvm : jvms) {
            if (Boolean.TRUE.equals(jvm.getAttribute(SERVICE_UP))) return true;
        }
        return false; // No JVMs running
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

    @Override
    public void start(Collection<? extends Location> locations) {
        Location provisioner = Iterables.getOnlyElement(locations);
        log.info("Creating new WaratekLocation wrapping {}", provisioner);

        Map<String, ?> flags = MutableMap.<String, Object>builder()
                .putAll(getConfig(LOCATION_FLAGS))
                .put("provisioner", provisioner)
                .build();
        waratek = createLocation(flags);
        log.info("New Waratek location {} created", waratek);

        super.start(locations);
    }

    /**
     * De-register our {@link WaratekLocation} and its children.
     */
    public void stop() {
        super.stop();

        deleteLocation();
    }

    @Override
    public WaratekLocation getDynamicLocation() { return waratek; }

    @Override
    public void deleteLocation() {
        LocationManager mgr = getManagementContext().getLocationManager();
        if (waratek != null && mgr.isManaged(waratek)) {
            mgr.unmanage(waratek);
            setAttribute(DYNAMIC_LOCATION,  null);
        }
    }

    /**
     * Create a new {@link WaratekLocation} wrapping the provided provisioner.
     */
    @Override
    public WaratekLocation createLocation(Map<String, ?> flags) {
        String locationName = getConfig(LOCATION_NAME);
        if (locationName == null) {
            String prefix = getConfig(LOCATION_NAME_PREFIX);
            String suffix = getConfig(LOCATION_NAME_SUFFIX);
            locationName = Joiner.on("-").skipNulls().join(prefix, getId(), suffix);
        }

        String locationSpec = String.format(WaratekResolver.WARATEK_INFRASTRUCTURE_SPEC, getId()) + String.format(":(name=\"%s\")", locationName);
        setAttribute(LOCATION_SPEC, locationSpec);
        LocationDefinition definition = new BasicLocationDefinition(locationName, locationSpec, flags);
        Location location = getManagementContext().getLocationRegistry().resolve(definition);
        setAttribute(DYNAMIC_LOCATION, location);
        setAttribute(LOCATION_NAME, location.getId());
        getManagementContext().getLocationRegistry().updateDefinedLocation(definition);

        return (WaratekLocation) location;
    }

    @Override
    public boolean isLocationAvailable() {
        // TODO implementation
        return waratek != null;
    }

    @Override
    public Integer resize(Integer desiredSize) {
        return virtualMachines.resize(desiredSize);
    }

    @Override
    public Integer getCurrentSize() {
        return virtualMachines.getCurrentSize();
    }

}
