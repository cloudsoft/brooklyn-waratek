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

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.Application;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.BasicGroupImpl;
import brooklyn.entity.group.AbstractMembershipTrackingPolicy;
import brooklyn.location.Location;
import brooklyn.location.waratek.WaratekContainerLocation;
import brooklyn.policy.Policy;
import brooklyn.util.collections.MutableMap;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class WaratekApplicationGroupImpl extends BasicGroupImpl implements WaratekApplicationGroup {

    private static final Logger log = LoggerFactory.getLogger(WaratekApplicationGroupImpl.class);

    private Policy policy;
    private AtomicReference<Application> application = new AtomicReference();
    private Multimap<JavaVirtualMachine, JavaVirtualContainer> layout = Multimaps.synchronizedMultimap(HashMultimap.<JavaVirtualMachine, JavaVirtualContainer>create());

    @Override
    public void init() {
        log.info("Created Waratek Application Group");

        // Track Application JVCs
        policy = new AbstractMembershipTrackingPolicy(MutableMap.of("name", "Waratek Application Tracker")) {
            @Override
            protected void onEntityChange(Entity member) {
                if (log.isDebugEnabled()) log.debug("Member {} updated for {} appliation", member, WaratekApplicationGroupImpl.this);
                update();
            }
            @Override
            protected void onEntityAdded(Entity member) {
                if (log.isDebugEnabled()) log.debug("Member {} added to {} appliation", member, WaratekApplicationGroupImpl.this);
                update();
            }
            @Override
            protected void onEntityRemoved(Entity member) {
                if (log.isDebugEnabled()) log.debug("Member {} removed from {} appliation", member, WaratekApplicationGroupImpl.this);
                update();
            }
        };
        addPolicy(policy);
    }

    @Override
    public String getShortName() {
        return "ApplicationGroup";
    }

    public void update() {
        synchronized (layout) {
            layout.clear();
            if (getMembers().isEmpty()) return;
            for (Entity member : getMembers()) {
                if (application.compareAndSet(null, member.getApplication())) {
                    setAttribute(WARATEK_APPLICATION, application.get());
                }
                Optional<Location> found = Iterables.tryFind(member.getLocations(), Predicates.instanceOf(WaratekContainerLocation.class));
                if (found.isPresent()) {
                    WaratekContainerLocation container = (WaratekContainerLocation) found.get();
                    JavaVirtualContainer jvc = container.getOwner();
                    JavaVirtualMachine jvm = container.getJavaVirtualMachine();
                    layout.put(jvm, jvc);
                }
            }
        }
    }

    @Override
    public void setElasticGroup(Integer groupId) {
        setAttribute(ELASTIC_GROUP_ID, groupId);
        for (JavaVirtualMachine jvm : layout.keySet()) {
            // create the elastic group if not exists
        }
        for (JavaVirtualContainer jvc : layout.values()) {
            // add it to the elstic group
        }
    }

    @Override
    public Integer getElasticGroup() { return getAttribute(ELASTIC_GROUP_ID); }

    @Override
    public void setElasticGroupHeap(Long heapSize) {
        setAttribute(ELASTIC_MEMORY, heapSize);
        for (JavaVirtualMachine jvm : layout.keySet()) {
            // create the elastic group if not exists
            // set the elastic group size
        }
        for (JavaVirtualContainer jvc : layout.values()) {
            // add it to the elstic group
        }
    }

    @Override
    public Long getElasticGroupHeap() { return getAttribute(ELASTIC_MEMORY); }

}
