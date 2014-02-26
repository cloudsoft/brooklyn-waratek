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

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.waratek.cloudvm.JavaVirtualMachine;
import brooklyn.entity.waratek.cloudvm.WaratekInfrastructure;
import brooklyn.location.MachineProvisioningLocation;
import brooklyn.location.NoMachinesAvailableException;
import brooklyn.location.OsDetails;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.location.cloud.AvailabilityZoneExtension;
import brooklyn.util.flags.SetFromFlag;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class WaratekMachineLocation extends SshMachineLocation implements MachineProvisioningLocation<WaratekContainerLocation>, WaratekVirtualLocation {

    @SetFromFlag("machine")
    private SshMachineLocation machine;

    @SetFromFlag("jvm")
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

    @Override
    public WaratekContainerLocation obtain(Map<?, ?> flags) throws NoMachinesAvailableException {
        return new WaratekContainerLocation(flags);
    }

    @Override
    public MachineProvisioningLocation<WaratekContainerLocation> newSubLocation(Map<?, ?> newFlags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void release(WaratekContainerLocation machine) {
    }

    @Override
    public Map<String, Object> getProvisioningFlags(Collection<String> tags) {
        return null;
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

    public SshMachineLocation getMachine() {
        return machine;
    }

    public JavaVirtualMachine getJavaVirtualMachine() {
        return jvm;
    }

}
