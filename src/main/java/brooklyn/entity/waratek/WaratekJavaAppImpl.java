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
package brooklyn.entity.waratek;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.enricher.Enrichers;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.BasicEntityImpl;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.group.Cluster;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.java.UsesJavaMXBeans;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.StartableMethods;
import brooklyn.location.Location;
import brooklyn.util.task.DynamicTasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

public class WaratekJavaAppImpl extends BasicEntityImpl implements WaratekJavaApp {

    private static final Logger log = LoggerFactory.getLogger(WaratekJavaAppImpl.class);

    private Location location;
    private DynamicCluster virtualMachines;

    @Override
    public void init() {
        int initialSize = getConfig(JVM_CLUSTER_SIZE);
        EntitySpec jvmSpec = getConfig(JVM_SPEC);

        virtualMachines = addChild(EntitySpec.create(DynamicCluster.class)
                .configure(Cluster.INITIAL_SIZE, initialSize)
                .configure(DynamicCluster.MEMBER_SPEC, jvmSpec)
                .displayName("Java Virtual Machines"));
        if (Entities.isManaged(this)) Entities.manage(virtualMachines);

        virtualMachines.addEnricher(Enrichers.builder()
                .aggregating(UsesJavaMXBeans.USED_HEAP_MEMORY)
                .computingSum()
                .fromMembers()
                .publishing(TOTAL_HEAP_MEMORY)
                .build());
        virtualMachines.addEnricher(Enrichers.builder()
                .aggregating(JVC_COUNT)
                .computingSum()
                .fromMembers()
                .publishing(JVC_COUNT)
                .build());

        addEnricher(Enrichers.builder()
                .propagating(TOTAL_HEAP_MEMORY, JVC_COUNT)
                .from(virtualMachines)
                .build());
        addEnricher(Enrichers.builder()
                .propagating(ImmutableMap.of(DynamicCluster.GROUP_SIZE, JVM_COUNT))
                .from(virtualMachines)
                .build());

//      virtualMachines.addPolicy(AutoScalerPolicy.builder().
//              metric(HEAP_MEMORY_DELTA_PER_SECOND_IN_WINDOW).
//              metricRange(1000, 10000).
//              sizeRange(1, 4).
//              build());
    }

    @Override
    public Collection<Entity> getJvmList() { return virtualMachines.getMembers(); }

    @Override
    public void start(Collection<? extends Location> locations) {
        location = Iterables.getOnlyElement(locations);
        log.info("Starting application in {}", location);
        DynamicTasks.queue(StartableMethods.startingChildren(this, location));
    }

    @Override
    public void stop() {
        DynamicTasks.queue(StartableMethods.stoppingChildren(this));
    }

    @Override
    public void restart() {
        stop();
        start(ImmutableList.of(location));
    }

    @Override
    public String getShortName() { return "JVM"; }

}
