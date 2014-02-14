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

import brooklyn.entity.basic.EntityLocal;
import brooklyn.entity.java.JavaSoftwareProcessSshDriver;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.os.Os;
import brooklyn.util.text.Strings;

public class JavaContainerSshDriver extends JavaSoftwareProcessSshDriver implements JavaContainerDriver {

    public JavaContainerSshDriver(EntityLocal entity, SshMachineLocation machine) {
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

    protected String getJvcName() {
        return String.format(JavaVM.JVM_NAME_FORMAT, getEntity().getId());
    }

    protected String getJvmName() {
        return getEntity().getConfig(JavaContainer.JVM).getJvmName();
    }

    @Override
    public String getHeapSize() {
        Long size = getEntity().getConfig(JavaVM.HEAP_SIZE);
        int megabytes = (int) (size / (1024L * 1024L));
        log.info(String.format("Heap set to %d bytes (%s) - using '-X%sm' JVM argument", size, Strings.makeSizeString(size), megabytes));
        return megabytes + "m";
    }

    /** Does nothing; we are using the parent Waratek JVM instead. */
    @Override
    public boolean installJava() { return true; }

    @Override
    public void install() {
        log.info("Installing {}", getJvcName());
    }

    @Override
    public void customize() {
        log.info("Customizing {}", getJvcName());
    }

    @Override
    public void launch() {
        log.info("Launching {}", getJvcName());
        // sudo -u waratek JAVA_HOME/bin/java --prefix=RUN_DIR --jvm=JVM_NAME -cp CLASSPATH MAIN_CLASS
    }

    @Override
    public boolean isRunning() {
        return newScript(CHECK_RUNNING).execute() == 0;
    }

    @Override
    public void stop() {
        newScript(STOPPING).execute();
    }

}
