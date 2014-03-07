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
package brooklyn.location.waratek;

import groovy.lang.Closure;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.Entity;
import brooklyn.entity.waratek.cloudvm.JavaVirtualContainer;
import brooklyn.entity.waratek.cloudvm.JavaVirtualMachine;
import brooklyn.entity.waratek.cloudvm.WaratekAttributes;
import brooklyn.entity.waratek.cloudvm.WaratekInfrastructure;
import brooklyn.location.PortRange;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.flags.SetFromFlag;
import brooklyn.util.os.Os;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class WaratekContainerLocation extends SshMachineLocation implements WaratekVirtualLocation {

    private static final Logger LOG = LoggerFactory.getLogger(WaratekContainerLocation.class);

    @SetFromFlag("machine")
    private SshMachineLocation machine;

    @SetFromFlag("jvc")
    private JavaVirtualContainer jvc;

    public WaratekContainerLocation() {
        this(Maps.newLinkedHashMap());
    }

    public WaratekContainerLocation(Map properties) {
        super(properties);

        if (isLegacyConstruction()) {
            init();
        }
    }

    @Override
    public List<Entity> getJvcList() {
        return Lists.<Entity>newArrayList(jvc);
    }

    @Override
    public List<Entity> getJvmList() {
        return Lists.<Entity>newArrayList(jvc.getJavaVirtualMachine());
    }

    @Override
    public WaratekInfrastructure getWaratekInfrastructure() {
        return ((WaratekVirtualLocation) getParent()).getWaratekInfrastructure();
    }

    public JavaVirtualContainer getJavaVirtualContainer() {
        return jvc;
    }

    public JavaVirtualMachine getJavaVirtualMachine() {
        return jvc.getJavaVirtualMachine();
    }

    public Map<String,?> injectWaratekOptions(Map<String,?> env) {
        List<String> opts = Lists.newArrayList();
        opts.add("--prefix=" + jvc.getJavaVirtualMachine().getRootDirectory());
        opts.add("--async");
        opts.add("--jvm=" + jvc.getJavaVirtualMachine().getJvmName());
        opts.add("--jvc=" + jvc.getJvcName());
        if (env.containsKey(JavaVirtualMachine.JAVA_OPTS)) {
            opts.add(env.get(JavaVirtualMachine.JAVA_OPTS).toString());
        }

        String javaHome = getJavaVirtualMachine().getJavaHome();
        // TODO fix PATH search if necessary?

        Map<String, Object> updated = MutableMap.<String, Object>builder()
                .putAll(env)
                .put(JavaVirtualMachine.JAVA_OPTS, Joiner.on(" ").join(opts))
                .put("JAVA_HOME", javaHome)
                .build();
        LOG.info("Updated JAVA_OPTS in environment to '{}'", updated.get(JavaVirtualMachine.JAVA_OPTS));
        return updated;
    }

    /* Delegate port operations to machine */

    @Override
    public boolean obtainSpecificPort(int portNumber) {
        return machine.obtainSpecificPort(portNumber);
    }

    @Override
    public int obtainPort(PortRange range) {
        return machine.obtainPort(range);
    }

    @Override
    public void releasePort(int portNumber) {
        machine.releasePort(portNumber);
    }

    @Override
    protected int execWithLogging(Map<String,?> props, String summaryForLogging, List<String> commands, Map env, final Closure<Integer> execCommand) {
        LOG.info("Intercepted execWithLogging: {}", summaryForLogging);
        return super.execWithLogging(props, summaryForLogging, commands, injectWaratekOptions(env), execCommand);
    }
    @Override
    public int execScript(Map<String,?> props, String summaryForLogging, List<String> commands, Map<String,?> env) {
        // Handle check-running by retrieving JVC status directly
        LOG.info("Intercepted execScript: {}", summaryForLogging);
        if (summaryForLogging != null && summaryForLogging.startsWith("check-running")) {
            String status = jvc.getAttribute(WaratekAttributes.STATUS);
            LOG.info("Status is: {}", status);
            return JavaVirtualContainer.STATUS_SHUT_OFF.equals(status) ? 1 : 0;
        }
        return super.execScript(props, summaryForLogging, commands, injectWaratekOptions(env));
    }
    @Override
    public int execCommands(Map<String,?> props, String summaryForLogging, List<String> commands, Map<String,?> env) {
        LOG.info("Intercepted execCommands: {}", summaryForLogging);
        return super.execCommands(props, summaryForLogging, commands, injectWaratekOptions(env));
    }

}
