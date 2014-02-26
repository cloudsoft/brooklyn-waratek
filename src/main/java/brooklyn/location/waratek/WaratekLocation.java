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

import java.util.Collection;
import java.util.Map;

import brooklyn.location.MachineProvisioningLocation;
import brooklyn.location.NoMachinesAvailableException;
import brooklyn.location.basic.AbstractLocation;

public class WaratekLocation extends AbstractLocation implements MachineProvisioningLocation<WaratekVirtualMachineLocation> {

    @Override
    public WaratekVirtualMachineLocation obtain(Map<?, ?> flags) throws NoMachinesAvailableException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MachineProvisioningLocation<WaratekVirtualMachineLocation> newSubLocation(Map<?, ?> newFlags) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void release(WaratekVirtualMachineLocation machine) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Map<String, Object> getProvisioningFlags(Collection<String> tags) {
        // TODO Auto-generated method stub
        return null;
    }

}
