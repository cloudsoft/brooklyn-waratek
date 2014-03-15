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

import java.util.List;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.waratek.cloudvm.JavaVirtualContainer;
import brooklyn.entity.waratek.cloudvm.JavaVirtualMachine;
import brooklyn.entity.waratek.cloudvm.WaratekInfrastructure;
import brooklyn.location.Location;
import brooklyn.location.MachineProvisioningLocation;
import brooklyn.location.basic.SshMachineLocation;

import com.google.common.reflect.TypeToken;

public interface WaratekVirtualLocation extends Location {

    ConfigKey<MachineProvisioningLocation<SshMachineLocation>> PROVISIONER =
            ConfigKeys.newConfigKey(new TypeToken<MachineProvisioningLocation<SshMachineLocation>>() { },
                    "waratek.provisioner", "The underlying provisioner for VMs");

    ConfigKey<WaratekInfrastructure> INFRASTRUCTURE =
            ConfigKeys.newConfigKey(WaratekInfrastructure.class, "waratek.infrastructure", "The Waratek infrastructure entity");

    ConfigKey<SshMachineLocation> MACHINE =
            ConfigKeys.newConfigKey(SshMachineLocation.class, "waratek.machine", "The underlying SSHable VM");

    ConfigKey<JavaVirtualMachine> JVM =
            ConfigKeys.newConfigKey(JavaVirtualMachine.class, "waratek.jvm", "The underlying Waratek JVM entity");

    ConfigKey<JavaVirtualContainer> JVC =
            ConfigKeys.newConfigKey(JavaVirtualContainer.class, "waratek.jvc", "The underlying Waratek JVC entity");

    String PREFIX = "waratek-";

    List<Entity> getJvcList();

    List<Entity> getJvmList();

    WaratekInfrastructure getWaratekInfrastructure();

}
