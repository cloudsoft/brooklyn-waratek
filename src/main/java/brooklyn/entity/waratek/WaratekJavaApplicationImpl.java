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

import brooklyn.entity.java.UsesJmx;
import brooklyn.entity.java.VanillaJavaAppImpl;
import brooklyn.entity.waratek.cloudvm.JavaVirtualContainer;
import brooklyn.entity.waratek.cloudvm.JavaVirtualMachine;
import brooklyn.entity.waratek.cloudvm.WaratekUtils;
import brooklyn.event.feed.jmx.JmxFeed;
import brooklyn.event.feed.jmx.JmxHelper;
import brooklyn.location.waratek.WaratekContainerLocation;
import brooklyn.util.exceptions.Exceptions;
import brooklyn.util.time.Duration;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

public class WaratekJavaApplicationImpl extends VanillaJavaAppImpl implements WaratekJavaApplication {

    private static final Logger log = LoggerFactory.getLogger(WaratekJavaApplicationImpl.class);

    private JmxHelper jmxHelper;
    private JmxFeed jmxMxBeanFeed;

    @Override
    public void init() {
        log.info("Starting Waratek java application id {}", getId());
    }

    @Override
    protected void preStart() {
        WaratekContainerLocation location = (WaratekContainerLocation) Iterables.find(getLocations(), Predicates.instanceOf(WaratekContainerLocation.class));
        setAttribute(JavaVirtualContainer.JVC_NAME, location.getJavaVirtualContainer().getJvcName());
    }

    @Override
    protected void connectSensors() {
        jmxHelper = new JmxHelper(getJavaVirtualMachine().getAttribute(UsesJmx.JMX_URL));
        try {
            jmxHelper.connect();
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
        jmxMxBeanFeed = WaratekUtils.connectMXBeanSensors(jmxHelper, this, Duration.FIVE_SECONDS);
        WaratekUtils.connectEnrichers(this);
        connectServiceUpIsRunning();
    }

    public JavaVirtualContainer getJavaVirtualContainer() {
        WaratekContainerLocation location = (WaratekContainerLocation) Iterables.find(getLocations(), Predicates.instanceOf(WaratekContainerLocation.class));
        return location.getJavaVirtualContainer();
    }

    public JavaVirtualMachine getJavaVirtualMachine() {
        WaratekContainerLocation location = (WaratekContainerLocation) Iterables.find(getLocations(), Predicates.instanceOf(WaratekContainerLocation.class));
        return location.getJavaVirtualMachine();
    }

    @Override
    public void disconnectSensors() {
        disconnectServiceUpIsRunning();
        if (jmxMxBeanFeed != null) jmxMxBeanFeed.stop();
        if (jmxHelper != null) jmxHelper.disconnect();
    }

    @Override
    public Class<? extends WaratekJavaApplicationDriver> getDriverInterface() {
        return WaratekJavaApplicationDriver.class;
    }

}
