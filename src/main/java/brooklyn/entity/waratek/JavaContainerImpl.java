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

import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.entity.java.JavaAppUtils;
import brooklyn.entity.java.UsesJmx;
import brooklyn.event.feed.jmx.JmxFeed;

public class JavaContainerImpl extends SoftwareProcessImpl implements JavaContainer, UsesJmx {

    private static final Logger log = LoggerFactory.getLogger(JavaContainerImpl.class);

    private volatile JmxFeed jmxFeed;
    private JmxFeed jmxMxBeanFeed;

    @Override
    public void init() {
        log.info("Starting JVC id {}", getId());
    }

    @Override
    public Class<?> getDriverInterface() {
        return JavaContainerDriver.class;
    }

    @Override
    public String getJvcName() { return getAttribute(JVC_NAME); }

    @Override
    protected void connectSensors() {
        super.connectSensors();

//        String waratekMBean = "com.waratek:type=Management";
//        jmxFeed = JmxFeed.builder().entity(this).period(Duration.ONE_SECOND)
//                    .pollAttribute(new JmxAttributePollConfig<Boolean>(SERVICE_UP)
//                            .objectName(waratekMBean)
//                            .attributeName("Running")
//                            .setOnFailureOrException(false))
//                    .build();

        jmxMxBeanFeed = JavaAppUtils.connectMXBeanSensors(this);
    }
    
    @Override
    protected void disconnectSensors() {
        super.disconnectSensors();
        if (jmxFeed != null) jmxFeed.stop();
        if (jmxMxBeanFeed != null) jmxMxBeanFeed.stop();
    }

    @Override
    public String getShortName() { return "JVC"; }

    public JavaVM getJavaVM() {
        
    }

}
