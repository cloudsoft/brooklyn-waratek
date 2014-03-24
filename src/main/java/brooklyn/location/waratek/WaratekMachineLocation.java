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
package brooklyn.location.waratek;

import java.net.InetAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.Entity;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.waratek.cloudvm.JavaVirtualContainer;
import brooklyn.entity.waratek.cloudvm.JavaVirtualMachine;
import brooklyn.entity.waratek.cloudvm.WaratekAttributes;
import brooklyn.entity.waratek.cloudvm.WaratekInfrastructure;
import brooklyn.location.MachineLocation;
import brooklyn.location.MachineProvisioningLocation;
import brooklyn.location.NoMachinesAvailableException;
import brooklyn.location.OsDetails;
import brooklyn.location.basic.AbstractLocation;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.location.cloud.AvailabilityZoneExtension;
import brooklyn.location.dynamic.DynamicLocation;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.flags.SetFromFlag;

import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class WaratekMachineLocation extends AbstractLocation implements MachineLocation, MachineProvisioningLocation<WaratekContainerLocation>, WaratekVirtualLocation,
        DynamicLocation<JavaVirtualMachine, WaratekMachineLocation> {

    private static final Logger LOG = LoggerFactory.getLogger(WaratekMachineLocation.class);

    @SetFromFlag("machine")
    private SshMachineLocation machine;

    @SetFromFlag("owner")
    private JavaVirtualMachine jvm;

    public WaratekMachineLocation() {
        this(Maps.newLinkedHashMap());
    }

    public WaratekMachineLocation(Map properties) {
        super(properties);

        if (isLegacyConstruction()) {
            init();
        }
    }

    @Override
    public void init() {
        super.init();
        addExtension(AvailabilityZoneExtension.class, new WaratekContainerExtension(getManagementContext(), this));
    }

    public WaratekContainerLocation obtain() throws NoMachinesAvailableException {
        return obtain(Maps.<String,Object>newLinkedHashMap());
    }

    @Override
    public WaratekContainerLocation obtain(Map<?,?> flags) throws NoMachinesAvailableException {
        LOG.info("XXX Obtain on WaratekMachineLocation: {}", this);
        Integer maxSize = jvm.getConfig(JavaVirtualMachine.JVC_CLUSTER_MAX_SIZE);
        Integer currentSize = jvm.getAttribute(WaratekAttributes.JVC_COUNT);
        if (LOG.isDebugEnabled()) {
            LOG.debug("JVM {}: {} containers, max {}", new Object[] { jvm.getJvmName(), currentSize, maxSize });
        }

        // also try to satisfy the affinty rules etc.
        if (currentSize != null && currentSize >= maxSize) {
            throw new NoMachinesAvailableException(String.format("Limit of %d containers reached at %s", maxSize, jvm.getJvmName()));
        }

        // increase size of JVC cluster
        DynamicCluster cluster = jvm.getJvcCluster();
        Optional<Entity> added = cluster.growByOne(machine, flags);
        if (!added.isPresent()) {
            throw new NoMachinesAvailableException(String.format("Failed to create containers reached in %s", jvm.getJvmName()));
        }
        JavaVirtualContainer jvc = (JavaVirtualContainer) added.get();
        WaratekContainerLocation location = jvc.getDynamicLocation();
        return location;
    }

    @Override
    public MachineProvisioningLocation<WaratekContainerLocation> newSubLocation(Map<?, ?> newFlags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void release(WaratekContainerLocation machine) {
        LOG.info("JVM {}: releasing {}", new Object[] { jvm.getJvmName(), machine });
        DynamicCluster cluster = jvm.getJvcCluster();
        if (cluster.removeMember(machine.getOwner())) {
            LOG.info("JVM {}: member {} released", new Object[] { jvm.getJvmName(), machine });
        } else {
            LOG.warn("JVM {}: member {} not found for release", new Object[] { jvm.getJvmName(), machine });
        }
    }

    @Override
    public Map<String, Object> getProvisioningFlags(Collection<String> tags) {
        return MutableMap.<String, Object>of();
    }

    @Override
    public InetAddress getAddress() {
        return machine.getAddress();
    }

    @Override
    public OsDetails getOsDetails() {
        return machine.getOsDetails();
    }

    @Override
    public List<Entity> getJvcList() {
        return jvm.getJvcList();
    }

    @Override
    public List<Entity> getJvmList() {
        return Lists.<Entity>newArrayList(jvm);
    }

    @Override
    public WaratekInfrastructure getWaratekInfrastructure() {
        return ((WaratekVirtualLocation) getParent()).getWaratekInfrastructure();
    }

    @Override
    public JavaVirtualMachine getOwner() {
        return jvm;
    }

    public SshMachineLocation getMachine() {
        return machine;
    }

    @Override
    public ToStringHelper string() {
        return super.string()
                .add("machine", machine)
                .add("jvm", jvm);
    }

}
