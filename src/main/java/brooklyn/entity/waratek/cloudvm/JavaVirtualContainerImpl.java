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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.Lifecycle;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.entity.java.UsesJmx;
import brooklyn.event.feed.jmx.JmxFeed;
import brooklyn.event.feed.jmx.JmxHelper;
import brooklyn.location.Location;
import brooklyn.location.LocationSpec;
import brooklyn.location.dynamic.DynamicLocation;
import brooklyn.location.waratek.WaratekContainerLocation;
import brooklyn.location.waratek.WaratekMachineLocation;
import brooklyn.management.LocationManager;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.exceptions.Exceptions;
import brooklyn.util.os.Os;
import brooklyn.util.text.Strings;
import brooklyn.util.time.Duration;
import brooklyn.util.time.Time;

public class JavaVirtualContainerImpl extends SoftwareProcessImpl implements JavaVirtualContainer {

    private static final Logger log = LoggerFactory.getLogger(JavaVirtualContainerImpl.class);
    private static final AtomicInteger counter = new AtomicInteger(0);

    private JmxHelper jmxHelper;
    private JmxFeed jmxMxBeanFeed;

    @Override
    public void init() {
        log.info("Starting JVC id {}", getId());

        // Format JVC name. Arguments are: %1$s JVC id, %2$d JVC number, %3$s JVM id.
        String jvcName = String.format(getConfig(JavaVirtualContainer.JVC_NAME_FORMAT), getId(), counter.incrementAndGet(), getJavaVirtualMachine().getId());
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
        super.connectSensors();
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

    @Override
    public void doStart(Collection<? extends Location> locations) {
        super.doStart(locations);

        Map<String, ?> flags = MutableMap.<String, Object>builder()
                .putAll(getConfig(LOCATION_FLAGS))
                .build();

        createLocation(flags);
    }

    @Override
    public void doStop() {
        disconnectSensors();
        deleteLocation();

        setAttribute(SoftwareProcess.SERVICE_STATE, Lifecycle.STOPPING);
        getDriver().stop();
        setAttribute(SoftwareProcess.SERVICE_UP, false);
        setAttribute(SoftwareProcess.SERVICE_STATE, Lifecycle.STOPPED);
    }

    @Override
    public void deleteLocation() {
        WaratekContainerLocation location = getDynamicLocation();
        log.info("Deleting JVC location {}", location);

        if (location != null) {
            LocationManager mgr = getManagementContext().getLocationManager();
            if (mgr.isManaged(location)) {
                mgr.unmanage(location);
            }
        }
        setAttribute(DYNAMIC_LOCATION,  null);
        setAttribute(LOCATION_NAME,  null);
    }

    @Override
    public void postStart() {
        Long heapSize = getConfig(MAX_HEAP_SIZE);
        allocateHeap(heapSize);
        shutDown();
    }

    @Override
    public void disconnectSensors() {
        disconnectServiceUpIsRunning();
        if (jmxMxBeanFeed != null) jmxMxBeanFeed.stop();
        if (jmxHelper != null) jmxHelper.disconnect();
        super.disconnectSensors();
    }

    @Override
    public void shutDown() {
        String jvc = getAttribute(JavaVirtualContainer.JVC_NAME);
        log.info("Shut-Down {}", jvc);

        try {
            ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(WaratekUtils.waratekMXBeanName(jvc, "VirtualContainer")));
            String status = (String) jmxHelper.getAttribute(object.getObjectName(), "Status");
            if (!JavaVirtualContainer.STATUS_SHUT_OFF.equals(status)) {
                jmxHelper.operation(object.getObjectName(), "shutdownContainer");
                do {
                    Time.sleep(Duration.seconds(0.1d));
                    status = (String) jmxHelper.getAttribute(object.getObjectName(), "Status");
                } while (!JavaVirtualContainer.STATUS_SHUT_OFF.equals(status));
            }
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
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
    public void updateJafRules(String fileUrl) {
        String jvc = getAttribute(JavaVirtualContainer.JVC_NAME);
        log.info("Loading JAF file {}", fileUrl);

        setAttribute(JavaVirtualContainer.JAF_RULES_FILE_URL, fileUrl);

        //first update the JAF rule file itself
        ((JavaVirtualContainerDriver)getDriver()).updateJafRuleFile();

        //now make the JMX call to have the JVC load the new file
        try {
            ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(WaratekUtils.waratekMXBeanName(jvc, "VirtualContainer")));
            jmxHelper.operation(object.getObjectName(), "loadFirewall");
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
    public String getLogFileLocation() {
        JavaVirtualMachine jvm = getJavaVirtualMachine();
        return Os.mergePaths(jvm.getRootDirectory(), "var/log/javad", jvm.getJvmName(), getAttribute(JVC_NAME), "console.log");
    }

    @Override
    public Entity getRunningEntity() {
        return getAttribute(ENTITY);
    }

    public void setRunningEntity(Entity entity) {
        setAttribute(ENTITY, entity);
    }

    @Override
    public Class getDriverInterface() {
        return JavaVirtualContainerDriver.class;
    }

    @Override
    public WaratekContainerLocation getDynamicLocation() {
        return (WaratekContainerLocation) getAttribute(DYNAMIC_LOCATION);
    }

    /**
     * Create a new {@link WaratekContainerLocation} wrapping the JVM we are starting in.
     * <p>
     * Note that the JVC locations are not published to the registry.
     */
    @Override
    public WaratekContainerLocation createLocation(Map<String, ?> flags) {
        JavaVirtualMachine jvm = getConfig(JVM);
        WaratekMachineLocation machine = jvm.getDynamicLocation();
        String locationName = machine.getId() + "-" + getId();
        LocationSpec<WaratekContainerLocation> spec = LocationSpec.create(WaratekContainerLocation.class)
                .parent(machine)
                .configure(flags)
                .configure(DynamicLocation.OWNER, this)
                .configure("machine", machine.getMachine()) // The underlying SshMachineLocation
                .configure("address", machine.getAddress()) // FIXME
                .configure(machine.getMachine().getAllConfig(true))
                .displayName(getJvcName())
                .id(locationName);
        WaratekContainerLocation location = getManagementContext().getLocationManager().createLocation(spec);
        setAttribute(DYNAMIC_LOCATION, location);
        setAttribute(LOCATION_NAME, location.getId());

        log.info("New JVC location {} created", location);
        return location;
    }

    @Override
    public boolean isLocationAvailable() {
        return getDynamicLocation() != null;
    }

    @Override
    public String getShortName() { return "JVC"; }

}
