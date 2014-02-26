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
package brooklyn.entity.waratek;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.group.DynamicClusterImpl;

public class WaratekApplicationClusterImpl extends DynamicClusterImpl implements WaratekApplicationCluster {

    private static final Logger log = LoggerFactory.getLogger(WaratekApplicationClusterImpl.class);

    @Override
    public void init() {
        log.info("Starting a Waratek cluster of Java applications");
    }

    @Override
    public String getShortName() { return "WaratekCluster"; }

}
