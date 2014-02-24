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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.enricher.Enrichers;
import brooklyn.entity.Effector;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.BrooklynTasks;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.entity.effector.Effectors;
import brooklyn.entity.group.Cluster;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.group.DynamicClusterImpl;
import brooklyn.entity.java.UsesJavaMXBeans;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.Resizable;
import brooklyn.entity.trait.StartableMethods;
import brooklyn.location.Location;
import brooklyn.management.Task;
import brooklyn.management.TaskAdaptable;
import brooklyn.management.internal.EffectorUtils;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.task.DynamicTasks;
import brooklyn.util.task.ParallelTask;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class WaratekJavaAppImpl extends DynamicClusterImpl implements WaratekJavaApp {

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
    public List<Entity> getJvmList() {
        if (virtualMachines == null) {
            return ImmutableList.of();
        } else {
            return ImmutableList.copyOf(virtualMachines.getMembers());
        }
    }

    @Override
    public Cluster getJvmCluster() { return virtualMachines; }

    @Override
    public void start(Collection<? extends Location> locations) {
        log.info("Starting application in {}", locations);
        location = Iterables.getOnlyElement(locations);
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

    private Task<?> invoke(Entity entity, Effector<?> effector, Object...args) {
        return Effectors.invocation(entity, effector, EffectorUtils.prepareArgsForEffectorAsMapFromArray(effector, args)).asTask();
    }

    @Override
    public synchronized Integer resize(Integer desiredSize) {
        List<TaskAdaptable<?>> tasks = Lists.newArrayList();
        int delta = desiredSize - getCurrentSize();
        if (delta > 0) {
            for (Entity entity : getJvmList()) {
                int maxJvcs = entity.getConfig(JavaVM.JVC_CLUSTER_MAX_SIZE);
                int jvcCount = entity.getAttribute(JVC_COUNT);
                if (jvcCount < maxJvcs) {
                    int jvcDelta = Math.min(delta, maxJvcs - jvcCount);
                    tasks.add(invoke(entity, Resizable.RESIZE, jvcCount + jvcDelta));
                    delta -= jvcDelta;
                }
            }
            if (delta > 0) {
                int jvmClusterSize = getJvmCluster().getCurrentSize();
                tasks.add(invoke(getJvmCluster(), Resizable.RESIZE, jvmClusterSize + 1));
            }
        } else if (delta < 0) {
            for (Entity entity : getJvmList()) {
                int jvcCount = entity.getAttribute(JVC_COUNT);
                if (jvcCount > 0) {
                    int jvcDelta = -Math.min(Math.abs(delta), jvcCount);
                    tasks.add(invoke(entity, Resizable.RESIZE, jvcCount - jvcDelta));
                    delta += jvcDelta;
                }
            }
            if (delta < 0) {
                int jvmClusterSize = getJvmCluster().getCurrentSize();
                if (jvmClusterSize > 0) {
                    tasks.add(invoke(getJvmCluster(), Resizable.RESIZE, jvmClusterSize - 1));
                }
            }
        }
        if (tasks.size() > 0) {
            ParallelTask<?> invoke = new ParallelTask(
                    MutableMap.of(
                            "displayName", "Resize",
                            "description", "Invoking resize across cluster",
                            "tag", BrooklynTasks.tagForCallerEntity(this)),
                    tasks);
            DynamicTasks.queue(invoke);
            if (delta != 0) {
                DynamicTasks.queue(invoke(this, Resizable.RESIZE, desiredSize));
            }
        }
        return getCurrentSize();
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
