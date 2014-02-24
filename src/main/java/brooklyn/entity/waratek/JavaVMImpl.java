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
import java.util.concurrent.atomic.AtomicInteger;

import org.jclouds.compute.domain.OsFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.enricher.Enrichers;
import brooklyn.enricher.RollingTimeWindowMeanEnricher;
import brooklyn.enricher.TimeWeightedDeltaEnricher;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.entity.group.Cluster;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.java.JavaAppUtils;
import brooklyn.entity.java.UsesJavaMXBeans;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.StartableMethods;
import brooklyn.event.feed.jmx.JmxFeed;
import brooklyn.location.Location;
import brooklyn.location.MachineProvisioningLocation;
import brooklyn.location.jclouds.templates.PortableTemplateBuilder;
import brooklyn.policy.PolicySpec;
import brooklyn.policy.ha.ServiceFailureDetector;
import brooklyn.policy.ha.ServiceReplacer;
import brooklyn.policy.ha.ServiceRestarter;
import brooklyn.util.task.DynamicTasks;
import brooklyn.util.time.Duration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class JavaVMImpl extends SoftwareProcessImpl implements JavaVM {

    private static final Logger log = LoggerFactory.getLogger(JavaVMImpl.class);
    private static final AtomicInteger counter = new AtomicInteger(0);

    private JmxFeed jmxMxBeanFeed;
    private DynamicCluster containers;

    @Override
    public void init() {
        log.info("Starting JVM id {}", getId());

        setAttribute(JVM_NAME, String.format(getConfig(JavaVM.JVM_NAME_FORMAT), getId(), counter.incrementAndGet()));

        int initialSize = getConfig(JVC_CLUSTER_SIZE);
        EntitySpec jvcSpec = EntitySpec.create(getConfig(JVC_SPEC))
                .configure(JavaContainer.JVM, this);
        if (getConfig(HA_POLICY_ENABLE)) {
            jvcSpec.policy(PolicySpec.create(ServiceFailureDetector.class));
            jvcSpec.policy(PolicySpec.create(ServiceRestarter.class)
                        .configure(ServiceRestarter.FAILURE_SENSOR_TO_MONITOR, ServiceFailureDetector.ENTITY_FAILED));
        }

        containers = addChild(EntitySpec.create(DynamicCluster.class)
                .configure(Cluster.INITIAL_SIZE, initialSize)
                .configure(DynamicCluster.MEMBER_SPEC, jvcSpec)
                .displayName("Java Containers"));
        if (getConfig(HA_POLICY_ENABLE)) {
            containers.addPolicy(PolicySpec.create(ServiceReplacer.class)
                    .configure(ServiceReplacer.FAILURE_SENSOR_TO_MONITOR, ServiceRestarter.ENTITY_RESTART_FAILED));
        }
        if (Entities.isManaged(this)) Entities.manage(containers);

        addEnricher(Enrichers.builder()
                .propagating(ImmutableMap.of(DynamicCluster.GROUP_SIZE, WaratekJavaApp.JVC_COUNT))
                .from(containers)
                .build());
    }

    @Override
    public Class<?> getDriverInterface() {
        return JavaVMDriver.class;
    }

    @Override
    public JavaVMDriver getDriver() {
        return (JavaVMDriver) super.getDriver();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected Map<String, Object> obtainProvisioningFlags(MachineProvisioningLocation location) {
        Map flags = super.obtainProvisioningFlags(location);
        Long heapSize = getConfig(HEAP_SIZE, 512 * (1024L * 1024L));
        int megabytes = (int) (heapSize / (1024L * 1024L));
        flags.put("templateBuilder", new PortableTemplateBuilder().os64Bit(true).osFamily(OsFamily.CENTOS).minRam(megabytes));
        return flags;
    }

    @Override
    public Integer getSshPort() { return getConfig(SSH_ADMIN_ENABLE) ? getAttribute(SSH_PORT) :  null; }

    @Override
    public Integer getHttpPort() { return getConfig(HTTP_ADMIN_ENABLE) ? getAttribute(HTTP_PORT) :  null; }

    @Override
    public String getJvmName() { return getAttribute(JVM_NAME); }

    @Override
    public List<Entity> getJvcList() { return ImmutableList.copyOf(containers.getMembers()); }

    @Override
    public Cluster getJvcCluster() { return containers; }

    /** The path to the root directory of the running CloudVM */
    @Override
    public String getRootDirectory() {
        return getAttribute(ROOT_DIRECTORY);
    }

    @Override
    protected void connectSensors() {
        super.connectSensors();
        jmxMxBeanFeed = JavaAppUtils.connectMXBeanSensors(this);
        addEnricher(TimeWeightedDeltaEnricher.getPerSecondDeltaEnricher(this, UsesJavaMXBeans.USED_HEAP_MEMORY, WaratekJavaApp.HEAP_MEMORY_DELTA_PER_SECOND_LAST));
        addEnricher(new RollingTimeWindowMeanEnricher<Double>(this, WaratekJavaApp.HEAP_MEMORY_DELTA_PER_SECOND_LAST, WaratekJavaApp.HEAP_MEMORY_DELTA_PER_SECOND_IN_WINDOW, Duration.ONE_MINUTE));
        connectServiceUpIsRunning();
    }

    @Override
    protected void disconnectSensors() {
        disconnectServiceUpIsRunning();
        if (jmxMxBeanFeed != null) jmxMxBeanFeed.stop();
        super.disconnectSensors();
    }

    @Override
    public void doStart(Collection<? extends Location> locations) {
        super.doStart(locations);
        DynamicTasks.queue(StartableMethods.startingChildren(this));
    }

    @Override
    public void doStop() {
        DynamicTasks.queue(StartableMethods.stoppingChildren(this));
        super.doStop();
    }

    @Override
    public Integer resize(Integer desiredSize) {
        Integer maxSize = getConfig(JVC_CLUSTER_MAX_SIZE);
        if (desiredSize > maxSize) {
            return getJvcCluster().resize(maxSize);
        } else {
            return getJvcCluster().resize(desiredSize);
        }
    }

    @Override
    public Integer getCurrentSize() {
        return getJvcCluster().getCurrentSize();
    }

    @Override
    public String getShortName() { return "JVM"; }

}
