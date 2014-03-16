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

import brooklyn.entity.Group;
import brooklyn.entity.annotation.Effector;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.basic.MethodEffector;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.HasShortName;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.Sensors;

@ImplementedBy(WaratekApplicationGroupImpl.class)
public interface WaratekApplicationGroup extends Group, HasShortName {

    MethodEffector<Void> SET_ELASTIC_GROUP = new MethodEffector<Void>(WaratekApplicationGroup.class, "setElasticGroup");
    MethodEffector<Void> SET_ELASTIC_GROUP_HEAP = new MethodEffector<Void>(WaratekApplicationGroup.class, "setElasticGroupHeap");

    AttributeSensor<Integer> ELASTIC_GROUP_ID = Sensors.newIntegerSensor("waratek.elasticGroup.id", "The ID of the elastic group");
    AttributeSensor<Long> ELASTIC_MEMORY = Sensors.newLongSensor("waratek.elasticGroup.heapMemory", "The heap memory allcoated to the elastic group");

    @Effector(description="Set the elastic memory group for this applications JVCs")
    void setElasticGroup(@EffectorParam(name="groupId") int groupId);

    @Effector(description="Change the allocated heap memory for this elastic group")
    void setElasticGroupHeap(@EffectorParam(name="heapSize") long heapSize);

}
