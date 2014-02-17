package brooklyn.entity.waratek;

import java.util.List;

import javax.management.ObjectInstance;
import javax.management.ObjectName;

import brooklyn.entity.java.UsesJmx;
import brooklyn.entity.java.VanillaJavaAppSshDriver;
import brooklyn.event.feed.jmx.JmxHelper;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.GroovyJavaMethods;
import brooklyn.util.exceptions.Exceptions;
import brooklyn.util.os.Os;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * The SSH implementation of the {@link WaratekJavaAppDriver}.
 */
public class JavaContainerSshDriver extends VanillaJavaAppSshDriver implements JavaContainerDriver {

    private static final String VIRTUAL_MACHINE_MX_BEAN = "com.waratek:type=VirtualMachine";

    private volatile JmxHelper helper;

    public JavaContainerSshDriver(JavaContainerImpl entity, SshMachineLocation machine) {
        super(entity, machine);

        JavaVM jvm = getEntity().getConfig(JavaContainer.JVM);
        helper = new JmxHelper(jvm.getAttribute(UsesJmx.JMX_URL));
        try {
            helper.connect();
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
    }

    @Override
    protected String getLogFileLocation() {
        JavaVM jvm = getEntity().getConfig(JavaContainer.JVM);
        return Os.mergePaths(jvm.getRootDirectory(), "var/log/javad", jvm.getJvmName(), getEntity().getAttribute(JavaContainer.JVC_NAME), "console.log");
    }

    public String getJavaCommandLine() {
        String javaOpts = getShellEnvironment().get("JAVA_OPTS");
        String mainClass = getEntity().getMainClass();
        String args = getArgs();

        StringBuilder command = new StringBuilder()
                .append("java")
                .append(" ")
                .append(javaOpts)
                .append(" ")
                .append("-cp lib/*")
                .append(" ")
                .append(mainClass)
                .append(" ")
                .append(args);
        return command.toString();
    }

    @Override
    protected Optional<String> getCurrentJavaVersion() {
        // TODO call JMX to get Waratek JVM version details
        return Optional.of("1.6.0");
    }

    /*
     * Not required by JVCs using Waratek parent JVM.
     */

    @Override
    protected final List<String> getJmxJavaConfigOptions() { return Lists.newArrayList(); }

    @Override
    public final boolean installJava() { return true; }

    @Override
    public final void installJmxSupport() { }

    @Override
    public final boolean isJmxEnabled() { return false; }

    @Override
    public final boolean isJmxSslEnabled() { return false; }

    @Override
    public void install() {
        String jvc = getEntity().getAttribute(JavaContainer.JVC_NAME);
        log.info("Installing {}", jvc);

        String cmd = getJavaCommandLine();
        log.info("Command line: {}", cmd);
        try {
            ObjectInstance object = helper.findMBean(ObjectName.getInstance(VIRTUAL_MACHINE_MX_BEAN));
            helper.operation(object.getObjectName(), "defineContainer", jvc, cmd, getRootDirectory());
        } catch (Exception e) {
            Exceptions.propagate(e);
        }
    }

    @Override
    public void launch() {
        String jvc = getEntity().getAttribute(JavaContainer.JVC_NAME);
        log.info("Launching {}", jvc);

        try {
            ObjectInstance object = helper.findMBean(ObjectName.getInstance(VIRTUAL_MACHINE_MX_BEAN));
            helper.operation(object.getObjectName(), "startContainer", jvc);
        } catch (Exception e) {
            Exceptions.propagate(e);
        }
    }

    @Override
    public boolean isRunning() {
        String jvc = getEntity().getAttribute(JavaContainer.JVC_NAME);
        log.info("Checking {}", jvc);

        // JMX operation to get status
        return true;
    }

    @Override
    public void stop() {
        String jvc = getEntity().getAttribute(JavaContainer.JVC_NAME);
        log.info("Stopping {}", jvc);

        try {
            ObjectInstance object = helper.findMBean(ObjectName.getInstance(VIRTUAL_MACHINE_MX_BEAN));
            helper.operation(object.getObjectName(), "shutdownContainer", jvc);
        } catch (Exception e) {
            Exceptions.propagate(e);
        }
    }

    @Override
    public void kill() {
        String jvc = getEntity().getAttribute(JavaContainer.JVC_NAME);
        log.info("Killling {}", jvc);

        stop(); // Must stop JVC first
        try {
            ObjectInstance object = helper.findMBean(ObjectName.getInstance(VIRTUAL_MACHINE_MX_BEAN));
            helper.operation(object.getObjectName(), "undefineContainer", jvc);
        } catch (Exception e) {
            Exceptions.propagate(e);
        }
        if (helper != null) helper.disconnect();
    }

    @Override
    public String getHeapSize() {
        Long heapSize = getEntity().getConfig(JavaContainer.HEAP_SIZE, 512 * (1024L * 1024L));
        int megabytes = (int) (heapSize / (1024L * 1024L));
        return megabytes + "m";
    }

    @Override
    public String getRootDirectory() {
        return getRunDir();
    }
    
}
