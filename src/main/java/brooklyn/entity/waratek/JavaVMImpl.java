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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jclouds.compute.domain.OsFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.entity.group.Cluster;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.java.JavaAppUtils;
import brooklyn.entity.java.UsesJmx;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.StartableMethods;
import brooklyn.event.feed.jmx.JmxFeed;
import brooklyn.location.Location;
import brooklyn.location.MachineProvisioningLocation;
import brooklyn.location.jclouds.templates.PortableTemplateBuilder;
import brooklyn.util.task.DynamicTasks;

public class JavaVMImpl extends SoftwareProcessImpl implements JavaVM, UsesJmx {

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

        containers = addChild(EntitySpec.create(DynamicCluster.class)
                .configure(Cluster.INITIAL_SIZE, initialSize)
                .configure(DynamicCluster.MEMBER_SPEC, jvcSpec)
                .displayName("Java Containers"));
        if (Entities.isManaged(this)) Entities.manage(containers);
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
    protected Map<String,Object> obtainProvisioningFlags(MachineProvisioningLocation location) {
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
    public Collection<Entity> getJvcList() { return containers.getMembers(); }

    /** The path to the root directory of the running CloudVM */
    @Override
    public String getRootDirectory() {
        return getAttribute(ROOT_DIRECTORY);
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
    public String getShortName() { return "JVM"; }

}
