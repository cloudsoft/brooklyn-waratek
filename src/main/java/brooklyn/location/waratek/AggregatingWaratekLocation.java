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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.location.MachineLocation;
import brooklyn.location.MachineProvisioningLocation;
import brooklyn.location.NoMachinesAvailableException;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.flags.SetFromFlag;

import com.google.common.base.Objects.ToStringHelper;
import com.google.common.collect.Maps;

public class AggregatingWaratekLocation extends WaratekLocation {

    private static final Logger LOG = LoggerFactory.getLogger(AggregatingWaratekLocation.class);

    @SetFromFlag("provisioners")
    protected List<MachineProvisioningLocation<SshMachineLocation>> provisioners;

    public AggregatingWaratekLocation() {
        super(Maps.newLinkedHashMap());
    }

    public AggregatingWaratekLocation(Map properties) {
        super(properties);

        if (isLegacyConstruction()) {
            init();
        }
    }

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void configure(Map properties) {
        super.configure(properties);
    }

    @Override
    public MachineLocation obtain(Map<?,?> flags) throws NoMachinesAvailableException {
        return super.obtain(flags);
    }

    @Override
    public void release(MachineLocation machine) {
        super.release(machine);
    }

    @Override
    public ToStringHelper string() {
        return super.string()
                .add("provisioners", provisioners);
    }

}
