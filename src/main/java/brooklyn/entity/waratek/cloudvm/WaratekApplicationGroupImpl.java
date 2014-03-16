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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.annotation.Effector;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.basic.BasicGroupImpl;

public class WaratekApplicationGroupImpl extends BasicGroupImpl implements WaratekApplicationGroup {

    private static final Logger log = LoggerFactory.getLogger(WaratekApplicationGroupImpl.class);

    public void init() {
        log.info("Created Waratek JVC Fabric");
    }

    @Override
    public String getShortName() {
        return "ApplicationGroup";
    }

    @Override
    public void setElasticGroup(int groupId) {
        setAttribute(ELASTIC_GROUP_ID, groupId);
    }

    @Override
    public void setElasticGroupHeap(long heapSize) {
        setAttribute(ELASTIC_MEMORY, heapSize);
    }

}
