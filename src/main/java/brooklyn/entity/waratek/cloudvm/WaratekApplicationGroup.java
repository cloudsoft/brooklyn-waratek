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

import brooklyn.entity.Application;
import brooklyn.entity.annotation.Effector;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.basic.BasicGroup;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.MethodEffector;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.HasShortName;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.AttributeSensorAndConfigKey;
import brooklyn.event.basic.Sensors;

@ImplementedBy(WaratekApplicationGroupImpl.class)
public interface WaratekApplicationGroup extends BasicGroup, HasShortName {

    MethodEffector<Void> SET_ELASTIC_GROUP = new MethodEffector<Void>(WaratekApplicationGroup.class, "setElasticGroup");
    MethodEffector<Void> SET_ELASTIC_GROUP_HEAP = new MethodEffector<Void>(WaratekApplicationGroup.class, "setElasticGroupHeap");

    AttributeSensorAndConfigKey<Long, Long> ELASTIC_MEMORY = ConfigKeys.newSensorAndConfigKey(Long.class,
            "waratek.elasticGroup.heapMemory", "The heap memory allcoated to the elastic group", 0L);

    AttributeSensor<Integer> ELASTIC_GROUP_ID = Sensors.newIntegerSensor("waratek.elasticGroup.id", "The ID of the elastic group");

    AttributeSensor<Application> WARATEK_APPLICATION = Sensors.newSensor(Application.class,
            "waratek.application.entity", "The parent application entity");

    @Effector(description="Set the elastic memory group for this applications JVCs")
    void setElasticGroup(@EffectorParam(name="groupId") Integer groupId);

    @Effector(description="Change the allocated heap memory for this elastic group")
    void setElasticGroupHeap(@EffectorParam(name="heapSize") Long heapSize);

    Integer getElasticGroup();
    Long getElasticGroupHeap();

}
