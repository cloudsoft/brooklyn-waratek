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
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.entity.java.UsesJmx;
import brooklyn.entity.trait.StartableMethods;
import brooklyn.event.feed.jmx.JmxFeed;
import brooklyn.event.feed.jmx.JmxHelper;
import brooklyn.location.Location;
import brooklyn.location.LocationSpec;
import brooklyn.location.waratek.WaratekContainerLocation;
import brooklyn.location.waratek.WaratekMachineLocation;
import brooklyn.management.LocationManager;
import brooklyn.util.exceptions.Exceptions;
import brooklyn.util.os.Os;
import brooklyn.util.task.DynamicTasks;
import brooklyn.util.text.Strings;
import brooklyn.util.time.Duration;

public class JavaVirtualContainerImpl extends SoftwareProcessImpl implements JavaVirtualContainer {

    private static final Logger log = LoggerFactory.getLogger(JavaVirtualContainerImpl.class);
    private static final AtomicInteger counter = new AtomicInteger(0);

    private JmxHelper jmxHelper;
    private JmxFeed jmxMxBeanFeed;

    @Override
    public void init() {
        log.info("Starting JVC id {}", getId());

        String jvcName = String.format(getConfig(JavaVirtualContainer.JVC_NAME_FORMAT), getId(), counter.incrementAndGet());
        setDisplayName(jvcName);
        setAttribute(JVC_NAME, jvcName);
        setAttribute(Attributes.LOG_FILE_LOCATION, getLogFileLocation());
    }

    @Override
    public String getJvcName() { return getAttribute(JVC_NAME); }

    @Override
    public JavaVirtualMachine getJavaVirtualMachine() { return getConfig(JVM); }

    @Override
    protected void connectSensors() {
        jmxHelper = new JmxHelper(getJavaVirtualMachine().getAttribute(UsesJmx.JMX_URL));
        try {
            jmxHelper.connect();
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
        jmxMxBeanFeed = WaratekUtils.connectMXBeanSensors(jmxHelper, this, Duration.FIVE_SECONDS);
        WaratekUtils.connectEnrichers(this);
        connectServiceUpIsRunning();
    }

    /**
     * Create a new {@link WaratekContainerLocation} wrapping the JVM we are starting in.
     * <p>
     * Note that the JVC locations are not published to the registry.
     */
    @Override
    public void doStart(Collection<? extends Location> locations) {
        super.doStart(locations);

        JavaVirtualMachine jvm = getConfig(JVM);
        WaratekMachineLocation machine = jvm.getAttribute(JavaVirtualMachine.WARATEK_MACHINE_LOCATION);
        String locationName = machine.getId() + "-" + getId();
        LocationSpec<WaratekContainerLocation> spec = LocationSpec.create(WaratekContainerLocation.class)
                .parent(machine)
                .configure("jvc", this)
                .configure("address", machine.getAddress()) 
                .configure(machine.getMachine().getAllConfig(true))
                .displayName(getJvcName())
                .id(locationName);
        WaratekContainerLocation jvc = getManagementContext().getLocationManager().createLocation(spec);
        log.info("New JVC location {} created", jvc);
        setAttribute(WARATEK_CONTAINER_LOCATION, jvc);

        DynamicTasks.queue(StartableMethods.startingChildren(this));
    }

    @Override
    public void doStop() {
        DynamicTasks.queue(StartableMethods.stoppingChildren(this));

        LocationManager mgr = getManagementContext().getLocationManager();
        WaratekContainerLocation location = getAttribute(WARATEK_CONTAINER_LOCATION);
        if (location != null && mgr.isManaged(location)) {
            mgr.unmanage(location);
            setAttribute(WARATEK_CONTAINER_LOCATION,  null);
        }

        super.doStop();
    }

    @Override
    public void postStart() {
        Long heapSize = getConfig(MAX_HEAP_SIZE);
        allocateHeap(heapSize);
    }

    @Override
    public void disconnectSensors() {
        disconnectServiceUpIsRunning();
        if (jmxMxBeanFeed != null) jmxMxBeanFeed.stop();
        if (jmxHelper != null) jmxHelper.disconnect();
    }

    @Override
    public void pause() {
        String jvc = getAttribute(JavaVirtualContainer.JVC_NAME);
        log.info("Pausing {}", jvc);

        try {
            ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(WaratekUtils.waratekMXBeanName(jvc, "VirtualContainer")));
            jmxHelper.operation(object.getObjectName(), "suspendContainer");
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
    }

    @Override
    public void resume() {
        String jvc = getAttribute(JavaVirtualContainer.JVC_NAME);
        log.info("Resume {}", jvc);

        try {
            ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(WaratekUtils.waratekMXBeanName(jvc, "VirtualContainer")));
            jmxHelper.operation(object.getObjectName(), "resumeContainer");
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
    }

    @Override
    public Long allocateHeap(Long size) {
        String jvc = getAttribute(JavaVirtualContainer.JVC_NAME);
        log.info("Allocate {} to {}", Strings.makeSizeString(size), jvc);

        try {
            ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(WaratekUtils.waratekMXBeanName(jvc, "Memory")));
            Long oldSize = (Long) jmxHelper.getAttribute(object.getObjectName(), "MaximumHeapMemorySize");
            jmxHelper.setAttribute(object.getObjectName(), "MaximumHeapMemorySize", size);
            Long newSize = (Long) jmxHelper.getAttribute(object.getObjectName(), "MaximumHeapMemorySize");
            if (log.isDebugEnabled()) {
                log.debug("Changed max heap from {} to {}", Strings.makeSizeString(oldSize), Strings.makeSizeString(newSize));
            }
            setAttribute(MAX_HEAP_SIZE, newSize);
            return oldSize;
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
    }

    @Override
    public String getShortName() { return "JVC"; }

    @Override
    public String getLogFileLocation() {
        JavaVirtualMachine jvm = getJavaVirtualMachine();
        return Os.mergePaths(jvm.getRootDirectory(), "var/log/javad", jvm.getJvmName(), getAttribute(JVC_NAME), "console.log");
    }

    @Override
    public Class getDriverInterface() {
        return JavaVirtualContainerDriver.class;
    }

}
