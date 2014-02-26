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
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.enricher.Enrichers;
import brooklyn.entity.Effector;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.effector.Effectors;
import brooklyn.entity.group.Cluster;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.group.DynamicClusterImpl;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.Resizable;
import brooklyn.management.Task;
import brooklyn.management.internal.EffectorUtils;
import brooklyn.util.task.DynamicTasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class WaratekJavaAppImpl extends DynamicClusterImpl implements WaratekJavaApp {

    private static final Logger log = LoggerFactory.getLogger(WaratekJavaAppImpl.class);

    private DynamicCluster virtualMachines;

    @Override
    public void init() {
        int initialSize = getConfig(JVM_CLUSTER_SIZE);
        EntitySpec jvmSpec = getConfig(JVM_SPEC);

        virtualMachines = addChild(EntitySpec.create(DynamicCluster.class)
                .configure(Cluster.INITIAL_SIZE, initialSize)
                .configure(DynamicCluster.QUARANTINE_FAILED_ENTITIES, false)
                .configure(DynamicCluster.MEMBER_SPEC, jvmSpec)
                .displayName("Java Virtual Machines"));
        if (Entities.isManaged(this)) Entities.manage(virtualMachines);

        virtualMachines.addEnricher(Enrichers.builder()
                .aggregating(TOTAL_HEAP_MEMORY)
                .computingSum()
                .fromMembers()
                .publishing(TOTAL_HEAP_MEMORY)
                .build());
        virtualMachines.addEnricher(Enrichers.builder()
                .aggregating(HEAP_MEMORY_DELTA_PER_SECOND_IN_WINDOW)
                .computingSum()
                .fromMembers()
                .publishing(HEAP_MEMORY_DELTA_PER_SECOND_IN_WINDOW)
                .build());
        virtualMachines.addEnricher(Enrichers.builder()
                .aggregating(AVERAGE_CPU_USAGE)
                .computingAverage()
                .fromMembers()
                .publishing(AVERAGE_CPU_USAGE)
                .build());
        virtualMachines.addEnricher(Enrichers.builder()
                .aggregating(JVC_COUNT)
                .computingSum()
                .fromMembers()
                .publishing(JVC_COUNT)
                .build());

        addEnricher(Enrichers.builder()
                .propagating(TOTAL_HEAP_MEMORY, JVC_COUNT, AVERAGE_CPU_USAGE, HEAP_MEMORY_DELTA_PER_SECOND_IN_WINDOW)
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
    public List<Entity> getJvmList() {
        if (virtualMachines == null) {
            return ImmutableList.of();
        } else {
            return ImmutableList.copyOf(virtualMachines.getMembers());
        }
    }

    @Override
    public DynamicCluster getJvmCluster() { return virtualMachines; }

    @Override
    protected EntitySpec<?> getMemberSpec() {
        return getConfig(MEMBER_SPEC);
    }

    private Task<?> invoke(Entity entity, Effector<?> effector, Object...args) {
        return Effectors.invocation(entity, effector, EffectorUtils.prepareArgsForEffectorAsMapFromArray(effector, args)).asTask();
    }

    @Override
    protected Collection<Entity> grow(int delta) {
        for (Entity entity : getJvmList()) {
            int maxJvcs = entity.getConfig(JavaVM.JVC_CLUSTER_MAX_SIZE);
            int jvcCount = entity.getAttribute(JVC_COUNT);
            if (jvcCount < maxJvcs) {
                int jvcDelta = Math.min(delta, maxJvcs - jvcCount);
                DynamicTasks.queue(invoke(entity, Resizable.RESIZE, jvcCount + jvcDelta));
                delta -= jvcDelta;
            }
        }
        if (delta > 0) {
            int jvmClusterSize = getJvmCluster().getCurrentSize();
            DynamicTasks.queue(invoke(getJvmCluster(), Resizable.RESIZE, jvmClusterSize + 1));
            DynamicTasks.queue(invoke(this, Resizable.RESIZE, getCurrentSize() + delta));
        }
        return Collections.emptyList();
    }

    @Override
    protected void shrink(int delta) {
        for (Entity entity : getJvmList()) {
            int jvcCount = entity.getAttribute(JVC_COUNT);
            if (jvcCount > 0) {
                int jvcDelta = -Math.min(Math.abs(delta), jvcCount);
                DynamicTasks.queue(invoke(entity, Resizable.RESIZE, jvcCount - jvcDelta));
                delta -= jvcDelta;
            }
        }
    }

    @Override
    public synchronized Integer getCurrentSize() {
        Integer size = getAttribute(JVC_COUNT);
        if (size == null) return 0;
        return size;
    }

    @Override
    public String getShortName() { return "JVM"; }

}
