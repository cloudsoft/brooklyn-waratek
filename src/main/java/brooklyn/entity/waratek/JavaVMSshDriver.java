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
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.collections.MutableList;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.net.Networking;
import brooklyn.util.os.Os;
import brooklyn.util.ssh.BashCommands;
import brooklyn.util.task.DynamicTasks;
import brooklyn.util.task.ssh.SshTasks;
import brooklyn.util.text.Strings;

import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class JavaVMSshDriver extends JavaSoftwareProcessSshDriver implements JavaVMDriver {

    private AtomicBoolean installed = new AtomicBoolean(false);

    public JavaVMSshDriver(EntityLocal entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    /** The path to the root directory of the running CloudVM */
    protected String getWaratekDirectory() {
        return getRunDir();
    }

    protected String getLibDirectory() {
        return Os.mergePaths(getWaratekDirectory(), "var", "lib");
    }

    protected String getLogDirectory() {
        return Os.mergePaths(getWaratekDirectory(), "var", "log");
    }

    @Override
    protected String getLogFileLocation() {
        return Os.mergePaths(getLogDirectory(), "javad.err");
    }

    protected String getPidFile() {
        return Os.mergePaths(getLibDirectory(), "javad", getJvmName(), "jvm.pid");
    }

    protected String getJvmName() {
        return String.format(JavaVM.JVM_NAME_FORMAT, getEntity().getId());
    }

    @Override
    public String getApplicationUser() {
        return getEntity().getConfig(JavaVM.WARATEK_USER) ? "waratek" : getMachine().getUser();
    }

    @Override
    public String getHeapSize() {
        Long size = getEntity().getConfig(JavaVM.HEAP_SIZE);
        int megabytes = (int) (size / (1024L * 1024L));
        log.info(String.format("Heap set to %d bytes (%s) - using '-X%sm' JVM argument", size, Strings.makeSizeString(size), megabytes));
        return megabytes + "m";
    }

    @Override
    public Map<String, String> getShellEnvironment() {
        Map<String,String> env = super.getShellEnvironment();
        if (installed.get()) {
            // Environment needed for launch only
            // String javaHome = Os.mergePaths(getExpandedInstallDir(), "jdk", "jre");
            String javaHome = Os.mergePaths(getRunDir(), "usr/lib/jvm", String.format("java-1.6.0-waratek-%s.x86_64", getVersion()), "jre");
            env.put("JAVA_HOME", javaHome);
        }
        return env;
    }

    // TODO check the JMX options are passed through OK

    @Override
    protected List<String> getJmxJavaConfigOptions() {
        return MutableList.copyOf(Iterables.filter(super.getJmxJavaConfigOptions(), Predicates.not(Predicates.containsPattern("javaagent"))));
    }

    @Override
    public Map<String, String> getCustomJavaSystemProperties() {
        Map<String,String> props = super.getCustomJavaSystemProperties();
        if (installed.get()) {
            // Java options needed for launch only
            props.put("com.waratek.jvm.name", getJvmName());
            props.put("com.waratek.rootdir", getWaratekDirectory());
            if (getEntity().getConfig(JavaVM.SSH_ADMIN_ENABLE)) {
                props.put("com.waratek.ssh.server", "on");
                props.put("com.waratek.ssh.port", getEntity().getAttribute(JavaVM.SSH_PORT).toString());
                props.put("com.waratek.jirsh.shell", "ascii");
                /* -Dcom.waratek.ssh.ip=n.n.n.n */
            } else {
                props.put("com.waratek.ssh.server", "off");
            }
            if (getEntity().getConfig(JavaVM.HTTP_ADMIN_ENABLE)) {
                props.put("com.waratek.jmxhttp.jolokia", "port=" + getEntity().getAttribute(JavaVM.SSH_PORT).toString());
            }
            String javaagent = Iterables.find(super.getJmxJavaConfigOptions(), Predicates.containsPattern("javaagent"));
            props.put("com.waratek.javaagent", javaagent);

            log.info("Java property map: " + Joiner.on(",").useForNull("").withKeyValueSeparator("=").join(props));
        }
        return props;
    }

    @Override
    public Set<Integer> getPortsUsed() {
        Set<Integer> result = Sets.newLinkedHashSet(super.getPortsUsed());
        result.addAll(getPortMap().values());
        return result;
    }

    protected Map<String, Integer> getPortMap() {
        ImmutableMap.Builder<String, Integer> builder = ImmutableMap.<String, Integer>builder()
                .put("jmxPort", entity.getAttribute(UsesJmx.JMX_PORT))
                .put("rmiPort", entity.getAttribute(UsesJmx.RMI_REGISTRY_PORT));
        if (entity.getConfig(JavaVM.SSH_ADMIN_ENABLE)) {
            builder.put("sshPort", entity.getAttribute(JavaVM.SSH_PORT));
        }
        if (entity.getConfig(JavaVM.SSH_ADMIN_ENABLE)) {
            builder.put("httpPort:", entity.getAttribute(JavaVM.SSH_PORT));
        }
        return builder.build();
    }

    /** Does nothing; we are installing the Waratek JVM instead. */
    @Override
    public boolean installJava() { return true; }

    @Override
    public void install() {
        log.info("Installing {}", getJvmName());

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
    }

    @Override
    public void customize() {
        log.info("Customizing {}", getJvmName());

        Networking.checkPortsValid(getPortMap());
        String installScript = Os.mergePaths(getExpandedInstallDir(), "tools", "autoinstall.sh");
        String debug = entity.getConfig(JavaVM.DEBUG) ? " -x" : "";
        newScript(CUSTOMIZING)
                 /* .failOnNonZeroResultCode() */ // FIXME issue with autoinstall.sh script
                .body.append(BashCommands.sudo(installScript + debug + " -s -p " + getRunDir() + " -u " + getApplicationUser()))
                .execute();

        installed.set(true);
    }

    @Override
    public void launch() {
        log.info("Launching {}", getJvmName());

        String javad = String.format("%1$s -Xdaemon $JAVA_OPTS -Xms%2$s -Xmx%2$s",
                Os.mergePaths("$JAVA_HOME", "bin", "javad"), getHeapSize());
        if (log.isDebugEnabled()) {
            log.debug("JVM command: {}", javad);
        }
        newScript(LAUNCHING)
                .body.append(BashCommands.sudoAsUser(getApplicationUser(), javad))
                .execute();
    }

    @Override
    public boolean isRunning() {
        return newScript(MutableMap.of("usePidFile", getPidFile()), CHECK_RUNNING).execute() == 0;
    }

    @Override
    public void stop() {
        newScript(MutableMap.of("usePidFile", getPidFile(), "processOwner", getApplicationUser()), STOPPING).execute();
    }

}
