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
import java.util.List;
import java.util.Map;

import brooklyn.entity.Entity;
import brooklyn.entity.waratek.cloudvm.JavaVirtualContainer;
import brooklyn.entity.waratek.cloudvm.JavaVirtualMachine;
import brooklyn.entity.waratek.cloudvm.WaratekInfrastructure;
import brooklyn.location.OsDetails;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.flags.SetFromFlag;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class WaratekContainerLocation extends SshMachineLocation implements WaratekVirtualLocation {

    @SetFromFlag("machine")
    private SshMachineLocation machine;

    @SetFromFlag("jvc")
    private JavaVirtualContainer jvc;

    public WaratekContainerLocation() {
        this(Maps.newLinkedHashMap());
    }

    public WaratekContainerLocation(Map properties) {
        super(properties);

        if (isLegacyConstruction()) {
            init();
        }
    }

    @Override
    public InetAddress getAddress() {
        return ((WaratekMachineLocation) getParent()).getAddress();
    }

    @Override
    public OsDetails getOsDetails() {
        return ((WaratekMachineLocation) getParent()).getOsDetails();
    }

    @Override
    public List<Entity> getJvcList() {
        return Lists.<Entity>newArrayList(jvc);
    }

    @Override
    public List<Entity> getJvmList() {
        return Lists.<Entity>newArrayList(jvc.getJavaVirtualMachine());
    }

    @Override
    public WaratekInfrastructure getWaratekInfrastructure() {
        return ((WaratekVirtualLocation) getParent()).getWaratekInfrastructure();
    }

    public JavaVirtualContainer getJavaVirtualContainer() {
        return jvc;
    }

    public JavaVirtualMachine getJavaVirtualMachine() {
        return jvc.getJavaVirtualMachine();
    }

    public SshMachineLocation getMachine() {
        return machine;
    }

}
