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

import java.util.Map;

import org.jclouds.compute.domain.OsFamily;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.entity.java.JavaAppUtils;
import brooklyn.entity.java.UsesJmx;
import brooklyn.event.feed.jmx.JmxFeed;
import brooklyn.location.MachineProvisioningLocation;
import brooklyn.location.jclouds.templates.PortableTemplateBuilder;
import brooklyn.util.text.ByteSizeStrings;
import brooklyn.util.text.Strings;

public class JavaVMImpl extends SoftwareProcessImpl implements JavaVM, UsesJmx {

    private static final Logger log = LoggerFactory.getLogger(JavaVMImpl.class);

    private volatile JmxFeed jmxFeed;
    private JmxFeed jmxMxBeanFeed;

    @Override
    public void init() {
        log.info("Starting CloudVM id %s", getId());
    }

    @Override
    public Class<?> getDriverInterface() {
        return JavaVMDriver.class;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Map<String,Object> obtainProvisioningFlags(MachineProvisioningLocation location) {
        Map flags = super.obtainProvisioningFlags(location);
        flags.put("templateBuilder", new PortableTemplateBuilder().os64Bit(true).osFamily(OsFamily.CENTOS));
        return flags;
    }

    @Override
    public Integer getSshPort() { return getConfig(SSH_ADMIN_ENABLE) ? getAttribute(SSH_PORT) :  null; }

    @Override
    public Integer getHttpPort() { return getConfig(HTTP_ADMIN_ENABLE) ? getAttribute(HTTP_PORT) :  null; }

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

}
