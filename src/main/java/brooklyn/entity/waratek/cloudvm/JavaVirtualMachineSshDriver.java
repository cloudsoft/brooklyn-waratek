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

import static java.lang.String.format;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.entity.drivers.downloads.DownloadResolver;
import brooklyn.entity.java.JavaSoftwareProcessSshDriver;
import brooklyn.entity.java.UsesJmx;
import brooklyn.location.basic.PortRanges;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.ResourceUtils;
import brooklyn.util.collections.MutableList;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.collections.MutableSet;
import brooklyn.util.net.Networking;
import brooklyn.util.os.Os;
import brooklyn.util.ssh.BashCommands;
import brooklyn.util.task.DynamicTasks;
import brooklyn.util.task.ssh.SshTasks;
import brooklyn.util.text.ByteSizeStrings;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class JavaVirtualMachineSshDriver extends JavaSoftwareProcessSshDriver implements JavaVirtualMachineDriver {

    private AtomicBoolean installed = new AtomicBoolean(false);
    private static final String LICENSE_KEY_NAME = "LICENSE_KEY";

    public JavaVirtualMachineSshDriver(EntityLocal entity, SshMachineLocation machine) {
        super(entity, machine);

        entity.setAttribute(JavaVirtualMachine.ROOT_DIRECTORY, getRootDirectory());
        entity.setAttribute(JavaVirtualMachine.JAVA_HOME, getJavaHome());
    }

    /** The path to the root directory of the running CloudVM */
    @Override
    public String getRootDirectory() {
        return getRunDir();
    }
    @Override
    public String getJavaHome() {
        return Os.mergePaths(getRunDir(), "usr/lib/jvm", String.format("java-1.6.0-waratek-%s.x86_64", getVersion()), "jre");
    }

    protected String getLibDirectory() {
        return Os.mergePaths(getRootDirectory(), "var", "lib");
    }

    protected String getLogDirectory() {
        return Os.mergePaths(getRootDirectory(), "var", "log");
    }

    @Override
    protected String getLogFileLocation() {
        return Os.mergePaths(getLogDirectory(), "javad.err");
    }

    protected String getPidFile() {
        return Os.mergePaths(getLibDirectory(), "javad", getEntity().getAttribute(JavaVirtualMachine.JVM_NAME), "jvm.pid");
    }

    @Override
    public boolean useWaratekUser() {
        return getEntity().getConfig(JavaVirtualMachine.USE_WARATEK_USER);
    }

    @Override
    public String getWaratekUsername() {
        return getEntity().getConfig(JavaVirtualMachine.WARATEK_USER);
    }

    @Override
    public String getLicenseUrl()
    {
        return getEntity().getConfig(JavaVirtualMachine.WARATEK_LICENSE_URL);
    }

    @Override
    public String getHeapSize() {
        Long size = getEntity().getConfig(JavaVirtualMachine.HEAP_SIZE);
        String xmx = ByteSizeStrings.java().apply(size);
        log.info(String.format("Heap set to %d bytes (%s) - using '-X%s' JVM argument", size, ByteSizeStrings.iso().apply(size), xmx));
        return xmx;
    }

    @Override
    public Map<String, String> getShellEnvironment() {
        MutableMap.Builder<String, String> builder = MutableMap.<String, String>builder()
                .putAll(super.getShellEnvironment());
        if (installed.get()) {
            builder.put("JAVA_HOME", getJavaHome());
        }
        return builder.build();
    }

    @Override
    protected List<String> getJmxJavaConfigOptions() {
        return MutableList.copyOf(Iterables.filter(super.getJmxJavaConfigOptions(), Predicates.not(Predicates.containsPattern("javaagent"))));
    }

    @Override
    protected Map<String, ?> getJmxJavaSystemProperties() {
        MutableMap.Builder<String, Object> builder = MutableMap.<String, Object>builder()
                .putAll(super.getJmxJavaSystemProperties())
                .put("com.sun.management.jmxremote.registry.ssl", "false");
        if (getEntity().getConfig(JavaVirtualMachine.HTTP_ADMIN_ENABLE)) {
            // jolokia wants a locally accessible JMX port set here, we don't need to allow external access
            builder.put("com.sun.management.jmxremote.port", getMachine().obtainPort(PortRanges.ANY_HIGH_PORT));
            // TODO add the rest of the required JAAS properties
        }
        return builder.build();
    }

    @Override
    public Map<String, String> getCustomJavaSystemProperties() {
        MutableMap.Builder<String, String> builder = MutableMap.<String, String>builder()
                .putAll(super.getCustomJavaSystemProperties());
        if (installed.get()) {
            // Java options needed for launch only
            builder.put("com.waratek.jvm.name", getEntity().getAttribute(JavaVirtualMachine.JVM_NAME));
            builder.put("com.waratek.rootdir", getRootDirectory());
            // TODO JMXRMI debugging
            // builder.put("sun.rmi.transport.logLevel", "VERBOSE");
            // builder.put("sun.rmi.transport.tcp.logLevel", "VERBOSE");
            // builder.put("sun.rmi.server.logLevel", "VERBOSE");
            // builder.put("sun.rmi.client.logCalls", "true");
            if (getEntity().getConfig(JavaVirtualMachine.DEBUG)) {
                builder.put("com.waratek.debug.log", "guest.log");
                builder.put("com.waratek.debug.log_level", "debug");
            }
            if (getEntity().getConfig(JavaVirtualMachine.SSH_ADMIN_ENABLE)) {
                builder.put("com.waratek.ssh.server", "on");
                builder.put("com.waratek.ssh.port", getEntity().getAttribute(JavaVirtualMachine.SSH_PORT).toString());
                builder.put("com.waratek.ssh.ip", getMachine().getAddress().getHostAddress());
            } else {
                builder.put("com.waratek.ssh.server", "off");
            }
            if (getEntity().getConfig(JavaVirtualMachine.HTTP_ADMIN_ENABLE)) {
                // TODO extra properties and configuration required?
                builder.put("com.waratek.jmxhttp.jolokia", "port=" + getEntity().getAttribute(JavaVirtualMachine.HTTP_PORT).toString());
            }
            String javaagent = Iterables.find(super.getJmxJavaConfigOptions(), Predicates.containsPattern("javaagent"));
            builder.put("com.waratek.javaagent", javaagent);

        }
        return builder.build();
    }

    @Override
    public Set<Integer> getPortsUsed() {
        return MutableSet.<Integer>builder()
                .addAll(super.getPortsUsed())
                .addAll(getPortMap().values())
                .build();
    }

    protected Map<String, Integer> getPortMap() {
        MutableMap.Builder<String, Integer> builder = MutableMap.<String, Integer>builder()
                .put("jmxPort", getEntity().getAttribute(UsesJmx.JMX_PORT))
                .put("rmiPort", getEntity().getAttribute(UsesJmx.RMI_REGISTRY_PORT));
        if (getEntity().getConfig(JavaVirtualMachine.SSH_ADMIN_ENABLE)) {
            builder.put("sshPort", getEntity().getAttribute(JavaVirtualMachine.SSH_PORT));
        }
        if (getEntity().getConfig(JavaVirtualMachine.HTTP_ADMIN_ENABLE)) {
            builder.put("httpPort", getEntity().getAttribute(JavaVirtualMachine.HTTP_PORT));
        }
        return builder.build();
    }

    /** Does nothing; we are installing the Waratek JVM instead. */
    @Override
    public boolean installJava() { return true; }

    @Override
    public void install() {
        log.info("Installing {} to {}", getEntity().getAttribute(JavaVirtualMachine.JVM_NAME),
                getEntity().getAttribute(JavaVirtualMachine.INSTALL_DIR));

        DownloadResolver resolver = Entities.newDownloader(this);
        List<String> urls = resolver.getTargets();
        String saveAs = resolver.getFilename();
        setExpandedInstallDir(Os.mergePaths(getInstallDir(), resolver.getUnpackedDirectoryName(format("waratek_release_%s_package", getVersion()))));

        // We must be able to run sudo, to customize and launch
        DynamicTasks.queueIfPossible(SshTasks.dontRequireTtyForSudo(getMachine(), true)).orSubmitAndBlock();

        List<String> commands = ImmutableList.<String>builder()
                .addAll(BashCommands.commandsToDownloadUrlsAs(urls, saveAs))
                .add(BashCommands.INSTALL_TAR)
                .add("tar zxvf " + saveAs)
                .build();

        newScript(INSTALLING)
                .body.append(commands)
                .execute();

        getMachine().copyTo(ResourceUtils.create(this).getResourceFromUrl("classpath://brooklyn-waratek-container.jar"),
                Os.mergePaths(getInstallDir(), "brooklyn-waratek-container.jar"));
    }

    @Override
    public void customize() {
        log.info("Setup JVM {}", getEntity().getAttribute(JavaVirtualMachine.JVM_NAME));

        Networking.checkPortsValid(getPortMap());

        String installScript = Os.mergePaths(getExpandedInstallDir(), "tools", "autoinstall.sh");
        StringBuilder autoinstall = new StringBuilder(installScript);
        if (entity.getConfig(JavaVirtualMachine.DEBUG)) {
            autoinstall.append(" -x");
        }
        autoinstall.append(" -s");
        autoinstall.append(" -p ").append(getRunDir());
        if (!useWaratekUser()) {
            autoinstall.append(" -u ").append(getMachine().getUser());
        }
        if (log.isDebugEnabled()) {
            log.debug("Running command: {}", autoinstall.toString());
        }

        newScript(CUSTOMIZING)
                .failOnNonZeroResultCode().body.append(
                "sed -i.bak \"s/fail \\\"Could not set access control lists/echo \\\"Could not set access control lists/g\" " + installScript,
                BashCommands.sudo(autoinstall.toString()))
                .closeSshConnection()
                .execute();

        installLicenseFile();

        installed.set(true);
    }

    private void installLicenseFile()
    {
        String moveLicenseFileCommands = new String();
        if (getLicenseUrl() != null && !getLicenseUrl().isEmpty()) {
            //the directory we want the license file in require root permission (which cannot be obtained through ssh)
            //so first copy the file down
            getMachine().copyTo(ResourceUtils.create(this).getResourceFromUrl(getLicenseUrl()),
                    Os.mergePaths(getInstallDir(), LICENSE_KEY_NAME));
            //then sudo move the file to the correct directory.
            moveLicenseFileCommands = "mv " + Os.mergePaths(getInstallDir(), LICENSE_KEY_NAME) + " "
                    + Os.mergePaths(getLibDirectory(), "javad", LICENSE_KEY_NAME);
            moveLicenseFileCommands = BashCommands.sudo(moveLicenseFileCommands);

            newScript(CUSTOMIZING)
            .failOnNonZeroResultCode().body.append( moveLicenseFileCommands)
            .closeSshConnection()
            .execute();
        }

    }

    @Override
    public void launch() {
        log.info("Launching {}", getEntity().getAttribute(JavaVirtualMachine.JVM_NAME));

        String javad = String.format("%1$s -Xdaemon $JAVA_OPTS -Xms%2$s -Xmx%2$s %3$s",
                Os.mergePaths("$JAVA_HOME", "bin", "javad"), getHeapSize(),
                getEntity().getConfig(JavaVirtualMachine.DEBUG) ? "-Xverboselog:debug.log -Xtrace:management" : "");
        if (log.isDebugEnabled()) {
            log.debug("JVM command (as {}): {}", useWaratekUser() ? getWaratekUsername() : "brooklyn user", javad);
        }
        newScript(MutableMap.of(DEBUG, true, USE_PID_FILE, false), LAUNCHING)
                .body.append(useWaratekUser() ? BashCommands.sudoAsUser(getWaratekUsername(), javad) : javad)
                .uniqueSshConnection()
                .execute();
    }

    private Map<String, ?> getScriptFlags() {
        MutableMap.Builder<String, Object> builder = MutableMap.builder();
        builder.put(USE_PID_FILE, getPidFile());
        if (getEntity().getConfig(JavaVirtualMachine.DEBUG)) {
            builder.put(DEBUG, true);
        }
        if (useWaratekUser()) {
            builder.put(PROCESS_OWNER, getWaratekUsername());
        }
        Map<String, ?> flags = builder.build();
        return flags;
    }

    @Override
    public boolean isRunning() {
        return newScript(getScriptFlags(), CHECK_RUNNING).execute() == 0;
    }

    @Override
    public void stop() {
        newScript(getScriptFlags(), STOPPING).execute();
    }

}
