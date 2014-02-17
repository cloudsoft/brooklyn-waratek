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

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.java.VanillaJavaAppImpl;
import brooklyn.location.Location;

public class JavaContainerImpl extends VanillaJavaAppImpl implements JavaContainer {

    private static final Logger log = LoggerFactory.getLogger(JavaContainerImpl.class);
    private static final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public void init() {
        log.info("Starting JVC id {}", getId());

        setAttribute(JVC_NAME, String.format(getConfig(JavaContainer.JVC_NAME_FORMAT), getId(), counter.incrementAndGet()));
    }

    @Override
    public String getJvcName() { return getAttribute(JVC_NAME); }

    @Override
    public JavaVM getJavaVM() { return getConfig(JVM); }

    @Override
    protected void connectSensors() {
        connectServiceUpIsRunning();
    }

    @Override
    public void disconnectSensors() {
        disconnectServiceUpIsRunning();
        
        
        
    }

    @Override
    protected void doStart(Collection<? extends Location> locations) {
        super.doStart(locations);
    }

    @Override
    protected void doStop() {
        super.doStop();
    }

    @Override
    public void doRestart() {
        super.doRestart();
    }

    @Override
    public Class<? extends JavaContainerDriver> getDriverInterface() {
        return JavaContainerDriver.class;
    }

    @Override
    public String getShortName() { return "JVC"; }

}
