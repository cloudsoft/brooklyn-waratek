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

import brooklyn.entity.Entity;
import brooklyn.entity.waratek.cloudvm.JavaVirtualMachine;
import brooklyn.location.Location;
import brooklyn.location.cloud.AbstractAvailabilityZoneExtension;
import brooklyn.management.ManagementContext;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

public class WaratekMachineExtension extends AbstractAvailabilityZoneExtension {

    private final WaratekVirtualLocation location;

    public WaratekMachineExtension(ManagementContext managementContext, WaratekVirtualLocation location) {
        super(managementContext);
        this.location = Preconditions.checkNotNull(location, "location");
    }

    @Override
    protected List<Location> doGetAllSubLocations() {
        List<Location> result = Lists.newArrayList();
        for (Entity entity : location.getJvmList()) {
            JavaVirtualMachine jvm = (JavaVirtualMachine) entity;
            WaratekMachineLocation machine = jvm.getDynamicLocation();
            result.add(machine);
        }
        return result;
    }

    /** Forces call to {@link #doGetAllSubLocations()} each time. */
    @Override
    public List<Location> getAllSubLocations() {
        subLocations.set(null);
        return super.getAllSubLocations();
    }

    @Override
    protected boolean isNameMatch(Location loc, Predicate<? super String> namePredicate) {
        return namePredicate.apply(((WaratekMachineLocation) loc).getWaratekInfrastructure().getId());
    }

}
