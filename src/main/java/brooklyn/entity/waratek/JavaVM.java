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

import java.util.List;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Effector;
import brooklyn.entity.basic.BrooklynConfigKeys;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.effector.Effectors;
import brooklyn.entity.group.Cluster;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.java.UsesJmx;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.HasShortName;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.PortAttributeSensorAndConfigKey;
import brooklyn.event.basic.Sensors;
import brooklyn.location.basic.PortRanges;
import brooklyn.util.flags.SetFromFlag;

@ImplementedBy(JavaVMImpl.class)
public interface JavaVM extends SoftwareProcess, UsesJmx, HasShortName {

    String JVM_NAME_FORMAT = "jvm-brooklyn-%s";

    @SetFromFlag("version")
    ConfigKey<String> SUGGESTED_VERSION = ConfigKeys.newConfigKeyWithDefault(SoftwareProcess.SUGGESTED_VERSION, "2.5.4.GA.2-86");

    @SetFromFlag("downloadUrl")
    BasicAttributeSensorAndConfigKey<String> DOWNLOAD_URL = new BasicAttributeSensorAndConfigKey<String>(
            SoftwareProcess.DOWNLOAD_URL, "http://download.waratek.com/tgz/waratek_release_${version}_package.tar.gz?src=brooklyn");

    @SetFromFlag("debug")
    ConfigKey<Boolean> DEBUG = ConfigKeys.newBooleanConfigKey("waratek.debug", "Enable debug options", false);

    @SetFromFlag("highAvailabilty")
    ConfigKey<Boolean> HA_POLICY_ENABLE = ConfigKeys.newBooleanConfigKey("waratek.policy.ha.enable", "Enable high-availability and resilience/restart policies", false);

    // give it 2m to start up, by default
    @SetFromFlag("startTimeout")
    ConfigKey<Integer> START_TIMEOUT = ConfigKeys.newConfigKeyWithDefault(BrooklynConfigKeys.START_TIMEOUT, 2*60);

    @SetFromFlag("runAs")
    ConfigKey<Boolean> WARATEK_USER = ConfigKeys.newBooleanConfigKey("waratek.runAs", "Run the JVM process as the waratek user", true);

    @SetFromFlag("heapSize")
    ConfigKey<Long> HEAP_SIZE = ConfigKeys.newLongConfigKey(
            "waratek.jvm.heap.size", "Size of heap memory to allocate (in bytes)");

    @SetFromFlag("jmxAgentMode")
    ConfigKey<JmxAgentModes> JMX_AGENT_MODE = ConfigKeys.newConfigKeyWithDefault(UsesJmx.JMX_AGENT_MODE, JmxAgentModes.JMX_RMI_CUSTOM_AGENT);

    @SetFromFlag("sshAdmin")
    ConfigKey<Boolean> SSH_ADMIN_ENABLE = ConfigKeys.newBooleanConfigKey("waratek.admin.ssh.enable", "Enable JVM administration using SSH", false);

    @SetFromFlag("sshPort")
    AttributeSensor<Integer> SSH_PORT = new PortAttributeSensorAndConfigKey(
            "waratek.admin.ssh.port", "Port to use for JVM administration over SSH", PortRanges.fromString("2222+"));

    @SetFromFlag("httpAdmin")
    ConfigKey<Boolean> HTTP_ADMIN_ENABLE = ConfigKeys.newBooleanConfigKey("waratek.admin.http.enable", "Enable JVM administration using HTTP", false);

    @SetFromFlag("httpPort")
    AttributeSensor<Integer> HTTP_PORT = new PortAttributeSensorAndConfigKey(
            "waratek.admin.http.port", "Port to use for JVM administration over HTTP", PortRanges.fromString("7777+"));

    @SetFromFlag("initialSize")
    ConfigKey<Integer> JVC_CLUSTER_SIZE = ConfigKeys.newConfigKeyWithDefault(DynamicCluster.INITIAL_SIZE, 1);

    @SetFromFlag("jvcSpec")
    BasicAttributeSensorAndConfigKey<EntitySpec> JVC_SPEC = new BasicAttributeSensorAndConfigKey<EntitySpec>(
            EntitySpec.class, "waratek.jvc.spec", "Specification to use when creating child JVCs",
            EntitySpec.create(JavaContainer.class));

    AttributeSensor<String> JVM_NAME = Sensors.newStringSensor("waratek.jvm.name", "The name of the JVM");

    Effector<String> NEW_CONTAINER = Effectors.effector(String.class, "newContainer")
            .description("Creates a new JavaContainer inside this JVM")
            .buildAbstract();

    String newContainer();

    Cluster getJvcList();

    String getJvmName();

    Integer getSshPort();

    Integer getHttpPort();

}
