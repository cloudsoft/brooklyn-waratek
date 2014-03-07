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
import java.util.concurrent.atomic.AtomicInteger;

import org.jclouds.compute.domain.OsFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.enricher.Enrichers;
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
import brooklyn.location.LocationDefinition;
import brooklyn.location.LocationSpec;
import brooklyn.location.MachineProvisioningLocation;
import brooklyn.location.basic.BasicLocationDefinition;
import brooklyn.location.basic.Machines;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.location.jclouds.templates.PortableTemplateBuilder;
import brooklyn.location.waratek.WaratekLocation;
import brooklyn.location.waratek.WaratekMachineLocation;
import brooklyn.management.LocationManager;
import brooklyn.policy.PolicySpec;
import brooklyn.policy.ha.ServiceFailureDetector;
import brooklyn.policy.ha.ServiceReplacer;
import brooklyn.policy.ha.ServiceRestarter;
import brooklyn.util.task.DynamicTasks;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class JavaVirtualMachineImpl extends SoftwareProcessImpl implements JavaVirtualMachine {

    private static final Logger log = LoggerFactory.getLogger(JavaVirtualMachineImpl.class);
    private static final AtomicInteger counter = new AtomicInteger(0);

    private JmxFeed jmxMxBeanFeed;
    private DynamicCluster containers;

    @Override
    public void init() {
        log.info("Starting JVM id {}", getId());

        String jvmName = String.format(getConfig(JavaVirtualMachine.JVM_NAME_FORMAT), getId(), counter.incrementAndGet());
        setDisplayName(jvmName);
        setAttribute(JVM_NAME, jvmName);

        int initialSize = getConfig(JVC_CLUSTER_SIZE);
        EntitySpec jvcSpec = EntitySpec.create(getConfig(JVC_SPEC))
                .configure(JavaVirtualContainer.JVM, this);
        if (getConfig(HA_POLICY_ENABLE)) {
            jvcSpec.policy(PolicySpec.create(ServiceFailureDetector.class));
            jvcSpec.policy(PolicySpec.create(ServiceRestarter.class)
                        .configure(ServiceRestarter.FAILURE_SENSOR_TO_MONITOR, ServiceFailureDetector.ENTITY_FAILED));
        }

        containers = addChild(EntitySpec.create(DynamicCluster.class)
                .configure(Cluster.INITIAL_SIZE, initialSize)
                .configure(DynamicCluster.QUARANTINE_FAILED_ENTITIES, false)
                .configure(DynamicCluster.MEMBER_SPEC, jvcSpec)
                .displayName("Guest Java Virtual Machines"));
        if (getConfig(HA_POLICY_ENABLE)) {
            containers.addPolicy(PolicySpec.create(ServiceReplacer.class)
                    .configure(ServiceReplacer.FAILURE_SENSOR_TO_MONITOR, ServiceRestarter.ENTITY_RESTART_FAILED));
        }

        if (Entities.isManaged(this)) Entities.manage(containers);

        containers.addEnricher(Enrichers.builder()
                .aggregating(WaratekAttributes.HEAP_MEMORY_DELTA_PER_SECOND_IN_WINDOW)
                .computingSum()
                .fromMembers()
                .publishing(WaratekAttributes.HEAP_MEMORY_DELTA_PER_SECOND_IN_WINDOW)
                .build());
        containers.addEnricher(Enrichers.builder()
                .aggregating(UsesJavaMXBeans.USED_HEAP_MEMORY)
                .computingSum()
                .fromMembers()
                .publishing(WaratekAttributes.TOTAL_HEAP_MEMORY)
                .build());
        containers.addEnricher(Enrichers.builder()
                .aggregating(WaratekAttributes.CPU_USAGE)
                .computingAverage()
                .fromMembers()
                .publishing(WaratekAttributes.AVERAGE_CPU_USAGE)
                .build());

        addEnricher(Enrichers.builder()
                .propagating(ImmutableMap.of(DynamicCluster.GROUP_SIZE, WaratekAttributes.JVC_COUNT))
                .from(containers)
                .build());
        addEnricher(Enrichers.builder()
                .propagating(WaratekAttributes.TOTAL_HEAP_MEMORY, WaratekAttributes.AVERAGE_CPU_USAGE, WaratekAttributes.HEAP_MEMORY_DELTA_PER_SECOND_IN_WINDOW)
                .from(containers)
                .build());
    }

    @Override
    public Class<?> getDriverInterface() {
        return JavaVirtualMachineDriver.class;
    }

    @Override
    public JavaVirtualMachineDriver getDriver() {
        return (JavaVirtualMachineDriver) super.getDriver();
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
    public DynamicCluster getJvcCluster() { return containers; }

    /** The path to the root directory of the running CloudVM */
    @Override
    public String getRootDirectory() {
        return getAttribute(ROOT_DIRECTORY);
    }

    @Override
    public String getJavaHome() {
        return getAttribute(JAVA_HOME);
    }

    @Override
    protected void connectSensors() {
        super.connectSensors();
        jmxMxBeanFeed = JavaAppUtils.connectMXBeanSensors(this);
        connectServiceUpIsRunning();
    }

    @Override
    protected void disconnectSensors() {
        disconnectServiceUpIsRunning();
        if (jmxMxBeanFeed != null) jmxMxBeanFeed.stop();
        super.disconnectSensors();
    }

    /**
     * Create a new {@link WaratekMachineLocation} wrapping the machine we are starting in.
     */
    @Override
    public void doStart(Collection<? extends Location> locations) {
        super.doStart(locations);

        Optional<SshMachineLocation> machine = Machines.findUniqueSshMachineLocation(locations);
        WaratekInfrastructure infrastructure = getConfig(WARATEK_INFRASTRUCTURE);
        WaratekLocation waratek = infrastructure.getAttribute(WaratekInfrastructure.WARATEK_LOCATION);
        String locationName = waratek.getId() + "-" + getId();
        LocationSpec<WaratekMachineLocation> spec = LocationSpec.create(WaratekMachineLocation.class)
                .parent(waratek)
                .configure("jvm", this)
                .configure("machine", machine.get())
                .displayName(getJvmName())
                .id(locationName);
        WaratekMachineLocation location = getManagementContext().getLocationManager().createLocation(spec);
        String locationSpec = String.format("waratek:%s:%s", infrastructure.getId(), getJvmName());
        LocationDefinition definition = new BasicLocationDefinition(locationName, locationSpec, Maps.<String, Object>newHashMap());
        getManagementContext().getLocationRegistry().updateDefinedLocation(definition);
        log.info("New JVM location {} created", location);
        setAttribute(WARATEK_MACHINE_LOCATION, location);

        DynamicTasks.queue(StartableMethods.startingChildren(this));
    }

    @Override
    public void doStop() {
        DynamicTasks.queue(StartableMethods.stoppingChildren(this));

        LocationManager mgr = getManagementContext().getLocationManager();
        WaratekMachineLocation location = getAttribute(WARATEK_MACHINE_LOCATION);
        if (location != null && mgr.isManaged(location)) {
            mgr.unmanage(location);
            setAttribute(WARATEK_MACHINE_LOCATION,  null);
        }

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
