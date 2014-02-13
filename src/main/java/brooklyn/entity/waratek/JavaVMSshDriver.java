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

import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.entity.drivers.downloads.DownloadResolver;
import brooklyn.entity.java.JavaSoftwareProcessSshDriver;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.os.Os;
import brooklyn.util.ssh.BashCommands;
import brooklyn.util.text.Strings;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class JavaVMSshDriver extends JavaSoftwareProcessSshDriver implements JavaVMDriver {

    public JavaVMSshDriver(EntityLocal entity, SshMachineLocation machine) {
        super(entity, machine);

        entity.setAttribute(Attributes.LOG_FILE_LOCATION, getLogFileLocation());
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
        String javaHome = Os.mergePaths(getExpandedInstallDir(), "jdk", "jre");
        Map<String,String> env = super.getShellEnvironment();
        String path = env.remove("PATH");
        if (path != null) env.put("PATH", Os.mergePaths(javaHome, "bin") + ":" + path);
        env.put("JAVA_HOME", javaHome);
        return env;
    }

    // TODO check the JMX options are passed through OK
    // TODO -Dcom.waratek.javaagent=-javaagent:<jar file>

    @Override
    public Map<String, String> getCustomJavaSystemProperties() {
        Map<String,String> props = super.getCustomJavaSystemProperties();
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

        log.info("Java property map: " + Joiner.on(",").useForNull("").withKeyValueSeparator("=").join(props));
        return props;
    }

    @Override
    public void install() {
        DownloadResolver resolver = Entities.newDownloader(this);
        List<String> urls = resolver.getTargets();
        String saveAs = resolver.getFilename();

        List<String> commands = ImmutableList.<String>builder()
                .addAll(BashCommands.commandsToDownloadUrlsAs(urls, saveAs))
                .add(BashCommands.INSTALL_TAR)
                .add("tar zxvf " + saveAs)
                .build();

        newScript(INSTALLING)
                .updateTaskAndFailOnNonZeroResultCode()
                .body.append(commands)
                .execute();

        setExpandedInstallDir(Os.mergePaths(getInstallDir(), resolver.getUnpackedDirectoryName(format("waratek_release_%s_package", getVersion()))));
    }

    @Override
    public void customize() {
        String installScript = Os.mergePaths(getExpandedInstallDir(), "tools", "autoinstall.sh");
        newScript(CUSTOMIZING)
                .updateTaskAndFailOnNonZeroResultCode()
                .body.append(BashCommands.sudo(installScript + " -a -s -p " + getRunDir() + " -u " + getApplicationUser()))
                .execute();
    }

    @Override
    public void launch() {
        newScript(LAUNCHING)
                .updateTaskAndFailOnNonZeroResultCode()
                .body.append(BashCommands.sudoAsUser(getApplicationUser(), String.format("javad -Xdaemon $JAVA_OPTS -Xms%1$s -Xmx%1$s", getHeapSize())))
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
