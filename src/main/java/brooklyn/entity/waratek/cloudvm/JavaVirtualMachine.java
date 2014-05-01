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

import java.util.List;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.BrooklynConfigKeys;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.java.UsesJavaMXBeans;
import brooklyn.entity.java.UsesJmx;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.HasShortName;
import brooklyn.entity.trait.Resizable;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.PortAttributeSensorAndConfigKey;
import brooklyn.event.basic.Sensors;
import brooklyn.location.basic.PortRanges;
import brooklyn.location.dynamic.LocationOwner;
import brooklyn.location.waratek.WaratekMachineLocation;
import brooklyn.util.flags.SetFromFlag;
import brooklyn.util.time.Duration;

@ImplementedBy(JavaVirtualMachineImpl.class)
public interface JavaVirtualMachine extends SoftwareProcess, UsesJmx, UsesJavaMXBeans, Resizable, HasShortName, LocationOwner<WaratekMachineLocation, JavaVirtualMachine> {

    String JAVA_OPTS_VAR = "JAVA_OPTS";
    String JAVA_HOME_VAR = "JAVA_HOME";

    String WARATEK_USERNAME = "waratek";

    String DEFAULT_JVM_NAME_FORMAT = "jvm-brooklyn-%1$s";
    String ALTERNATIVE_JVM_NAME_FORMAT = "jvm-%2$d";

    @SetFromFlag("version")
    ConfigKey<String> SUGGESTED_VERSION = ConfigKeys.newConfigKeyWithDefault(SoftwareProcess.SUGGESTED_VERSION,
            "2.5.6.GA.1-98");

    @SetFromFlag("downloadUrl")
    BasicAttributeSensorAndConfigKey<String> DOWNLOAD_URL = new BasicAttributeSensorAndConfigKey<String>(SoftwareProcess.DOWNLOAD_URL,
            "https://s3-eu-west-1.amazonaws.com/brooklyn-waratek/waratek_release_${version}_package.tar.gz");
//            "http://download.waratek.com/brooklyn/waratek_release_${version}_package.tar.gz?src=brooklyn");

    @SetFromFlag("debug")
    ConfigKey<Boolean> DEBUG = ConfigKeys.newBooleanConfigKey("waratek.debug", "Enable debug options", false);

    @SetFromFlag("highAvailabilty")
    ConfigKey<Boolean> HA_POLICY_ENABLE = ConfigKeys.newBooleanConfigKey("waratek.policy.ha.enable", "Enable high-availability and resilience/restart policies", false);

    // give it 5m to start up, by default
    @SetFromFlag("startTimeout")
    ConfigKey<Duration> START_TIMEOUT = ConfigKeys.newConfigKeyWithDefault(BrooklynConfigKeys.START_TIMEOUT, Duration.FIVE_MINUTES);

    @SetFromFlag("runAs")
    ConfigKey<Boolean> USE_WARATEK_USER = ConfigKeys.newBooleanConfigKey("waratek.runAs", "Run the JVM process as the waratek user", false);

    @SetFromFlag("runAsUser")
    ConfigKey<String> WARATEK_USER = ConfigKeys.newStringConfigKey("waratek.runAs.username", "User to use when running the JVM process", WARATEK_USERNAME);

    @SetFromFlag("heapSize")
    ConfigKey<Long> HEAP_SIZE = ConfigKeys.newLongConfigKey("waratek.jvm.heapSize", "Size of heap memory to allocate (in bytes, default 1GiB)", 1000000000L);

    @SetFromFlag("sshAdmin")
    ConfigKey<Boolean> SSH_ADMIN_ENABLE = ConfigKeys.newBooleanConfigKey("waratek.admin.ssh.enable", "Enable JVM administration using SSH", false);

    @SetFromFlag("sshPort")
    PortAttributeSensorAndConfigKey SSH_PORT = new PortAttributeSensorAndConfigKey(
            "waratek.admin.ssh.port", "Port to use for JVM administration over SSH", PortRanges.fromString("2222+"));

    @SetFromFlag("httpAdmin")
    ConfigKey<Boolean> HTTP_ADMIN_ENABLE = ConfigKeys.newBooleanConfigKey("waratek.admin.http.enable", "Enable JVM administration using HTTP", false);

    @SetFromFlag("httpPort")
    PortAttributeSensorAndConfigKey HTTP_PORT = new PortAttributeSensorAndConfigKey(
            "waratek.admin.http.port", "Port to use for JVM administration over HTTP", PortRanges.fromString("7777+"));

    @SetFromFlag("licenceUrl")
    ConfigKey<String> WARATEK_LICENSE_URL = ConfigKeys.newStringConfigKey(
            "waratek.license.url", "The url for the license file on the local machine");

    @SetFromFlag("maxSize")
    ConfigKey<Integer> JVC_CLUSTER_MAX_SIZE = WaratekInfrastructure.JVC_CLUSTER_MAX_SIZE;

    @SetFromFlag("jvcSpec")
    BasicAttributeSensorAndConfigKey<EntitySpec> JVC_SPEC = new BasicAttributeSensorAndConfigKey<EntitySpec>(
            EntitySpec.class, "waratek.jvc.spec", "Specification to use when creating child JVCs",
            EntitySpec.create(JavaVirtualContainer.class));

    @SetFromFlag("infrastructure")
    ConfigKey<WaratekInfrastructure> WARATEK_INFRASTRUCTURE = ConfigKeys.newConfigKey(WaratekInfrastructure.class, "waratek.infrastructure", "The parent Waratek infrastructure");

    ConfigKey<String> JVM_NAME_FORMAT = ConfigKeys.newStringConfigKey("waratek.jvm.nameFormat", "Format for generating JVM names", DEFAULT_JVM_NAME_FORMAT);

    AttributeSensor<String> JVM_NAME = Sensors.newStringSensor("waratek.jvm.name", "The name of the JVM");

    AttributeSensor<String> ROOT_DIRECTORY = Sensors.newStringSensor("waratek.jvm.rootDirectory", "The JVM installation root directory");
    AttributeSensor<String> LIB_DIRECTORY = Sensors.newStringSensor("waratek.jvm.libDirectory", "The JVM installation log directory");
    AttributeSensor<String> LOG_DIRECTORY = Sensors.newStringSensor("waratek.jvm.logtDirectory", "The JVM installation lib directory");
    AttributeSensor<String> JAVA_HOME = Sensors.newStringSensor("waratek.jvm.javaHome", "The JVM JAVA_HOME directory");

    List<Entity> getJvcList();

    DynamicCluster getJvcCluster();

    WaratekInfrastructure getInfrastructure();

    String getJvmName();

    String getJavaHome();

    String getRootDirectory();

    String getLibDirectory();

    String getLogDirectory();

    Integer getSshPort();

    Integer getHttpPort();

    AttributeSensor<Integer> STOPPED_JVCS = Sensors.newIntegerSensor("waratek.jvm.stopped", "The number of stopped JVCs in the JVM");
    AttributeSensor<Integer> RUNNING_JVCS = Sensors.newIntegerSensor("waratek.jvm.running", "The number of running JVCs in the JVM");
    AttributeSensor<Integer> PAUSED_JVCS = Sensors.newIntegerSensor("waratek.jvm.paused", "The number of paused JVCs in the JVM");

    Iterable<Entity> getAvailableJvcs();

    Integer getRunningJvcs();
    Integer getPausedJvcs();

}
