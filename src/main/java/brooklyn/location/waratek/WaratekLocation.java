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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.java.UsesJava;
import brooklyn.entity.waratek.cloudvm.JavaVirtualMachine;
import brooklyn.entity.waratek.cloudvm.WaratekAttributes;
import brooklyn.entity.waratek.cloudvm.WaratekInfrastructure;
import brooklyn.location.MachineLocation;
import brooklyn.location.MachineProvisioningLocation;
import brooklyn.location.NoMachinesAvailableException;
import brooklyn.location.basic.AbstractLocation;
import brooklyn.location.basic.LocationConfigKeys;
import brooklyn.location.basic.Machines;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.location.cloud.AvailabilityZoneExtension;
import brooklyn.location.dynamic.DynamicLocation;
import brooklyn.util.flags.SetFromFlag;
import brooklyn.util.guava.Maybe;
import brooklyn.util.javalang.Reflections;

import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class WaratekLocation extends AbstractLocation implements WaratekVirtualLocation, MachineProvisioningLocation<MachineLocation>,
        DynamicLocation<WaratekInfrastructure, WaratekLocation> {

	private static final Logger LOG = LoggerFactory.getLogger(WaratekLocation.class);

    @SetFromFlag("mutex")
    private Object mutex;

    @SetFromFlag("provisioner")
    private MachineProvisioningLocation<SshMachineLocation> provisioner;

    @SetFromFlag("owner")
    private WaratekInfrastructure infrastructure;

    /* Mappings for provisioned locations */

    private final Set<SshMachineLocation> obtained = Sets.newHashSet();
    private final Multimap<SshMachineLocation, String> machines = HashMultimap.create();
    private final Map<String, SshMachineLocation> containers = Maps.newHashMap();

    public WaratekLocation() {
        this(Maps.newLinkedHashMap());
    }

    public WaratekLocation(Map properties) {
        super(properties);

        if (isLegacyConstruction()) {
            init();
        }
    }

    @Override
    public void init() {
        super.init();
        addExtension(AvailabilityZoneExtension.class, new WaratekMachineExtension(getManagementContext(), this));
    }

    @Override
    public void configure(Map properties) {
        if (mutex == null) {
            mutex = new Object[0];
        }
        super.configure(properties);
    }

    public MachineLocation obtain() throws NoMachinesAvailableException {
        return obtain(Maps.<String,Object>newLinkedHashMap());
    }

    @Override
    public MachineLocation obtain(Map<?,?> flags) throws NoMachinesAvailableException {
        LOG.info("XXX Obtain on WaratekLocation: {}", this);
        synchronized (mutex) {
            // Check context for entitiy implementing UsesJava interface
            Object context = flags.get(LocationConfigKeys.CALLER_CONTEXT.getName());
            if (context instanceof Entity) {
                List<Class<?>> implementations = Reflections.getAllInterfaces(context.getClass());
                boolean usesJava = Iterables.any(implementations, Predicates.<Class>equalTo(UsesJava.class));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Context {}: UsesJava {}", context.toString(), Boolean.toString(usesJava));
                }
                if (!usesJava) {
                    // Return an SshMachineLocation from the provisioner
                    SshMachineLocation machine = provisioner.obtain(flags);
                    obtained.add(machine);
                    return machine;
                }
            }

            // Look through existing infrastructure for non-empty JVMs
            JavaVirtualMachine jvm = null;
            for (Entity entity : infrastructure.getJvmList()) {
                Integer maxSize = entity.getConfig(JavaVirtualMachine.JVC_CLUSTER_MAX_SIZE);
                Integer currentSize = entity.getAttribute(WaratekAttributes.JVC_COUNT);

                // also try to satisfy the affinty rules etc.
                if (currentSize == null || currentSize < maxSize) {
                    jvm = (JavaVirtualMachine) entity;
                    break;
                }
            }

            // If we do not have a JVM, increase size of JVM cluster
            if (jvm == null) {
                DynamicCluster cluster = infrastructure.getVirtualMachineCluster();
                Optional<Entity> added = cluster.growByOne(provisioner, flags);
                if (added.isPresent()) {
                    jvm = (JavaVirtualMachine) added.get();
                } else {
                    throw new IllegalStateException("Failed to add JVM to cluster");
                }
            }

            // Now wait until the JVM has started up
            Entities.waitForServiceUp(jvm, jvm.getConfig(JavaVirtualMachine.START_TIMEOUT), TimeUnit.SECONDS);

            // Obtain a new JVC location, save and return it
            WaratekMachineLocation location = jvm.getDynamicLocation();
            WaratekContainerLocation container = location.obtain();
            Maybe<SshMachineLocation> deployed = Machines.findUniqueSshMachineLocation(jvm.getLocations());
            if (deployed.isPresent()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Storing container mapping: {}-{}", deployed.get(), container.getId());
                }
                machines.put(deployed.get(), container.getId());
                containers.put(container.getId(), deployed.get());
            }
            return container;
        }
    }

    @Override
    public void release(MachineLocation machine) {
        if (provisioner != null) {
            synchronized (mutex) {
                if (machine instanceof WaratekContainerLocation) {
                    String id = machine.getId();
                    SshMachineLocation ssh = containers.remove(id);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Request to release container mapping {}-{}", ssh, id);
                    }
                    if (ssh != null) {
                        machines.remove(ssh, id);
                        if (machines.get(ssh).isEmpty()) {
                            provisioner.release(ssh);
                        }
                    } else {
                        throw new IllegalArgumentException("Request to release "+machine+", but no SSH machine found");
                    }
                } else if (machine instanceof SshMachineLocation) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Request to release SSH machine {}", machine);
                    }
                    if (obtained.contains(machine)) {
                        provisioner.release((SshMachineLocation) machine);
                        obtained.remove(machine);
                    } else {
                        throw new IllegalArgumentException("Request to release "+machine+", but this machine is not currently allocated");
                    }
                } else {
                    throw new IllegalArgumentException("Request to release "+machine+", but location type is not supported");
                }
            }
        } else {
            throw new IllegalStateException("No provisioner available to release "+machine);
        }
    }

    @Override
    public Map<String,Object> getProvisioningFlags(Collection<String> tags) {
        return Maps.<String,Object>newLinkedHashMap();
    }

    public List<Entity> getJvcList() {
        return infrastructure.getJvcList();
    }

    public List<Entity> getJvmList() {
        return infrastructure.getJvmList();
    }

    public WaratekInfrastructure getWaratekInfrastructure() {
        return infrastructure;
    }

    @Override
    public MachineProvisioningLocation<MachineLocation> newSubLocation(Map<?, ?> newFlags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ToStringHelper string() {
        return super.string()
                .add("provisioner", provisioner)
                .add("infrastructure", infrastructure);
    }

    @Override
    public WaratekInfrastructure getOwner() {
        return infrastructure;
    }

}
