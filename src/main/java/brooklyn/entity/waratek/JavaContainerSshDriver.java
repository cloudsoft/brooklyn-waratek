package brooklyn.entity.waratek;

import java.util.List;

import javax.management.ObjectInstance;
import javax.management.ObjectName;

import brooklyn.entity.java.UsesJmx;
import brooklyn.entity.java.VanillaJavaAppSshDriver;
import brooklyn.event.feed.jmx.JmxHelper;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.exceptions.Exceptions;
import brooklyn.util.os.Os;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * The SSH implementation of the {@link WaratekJavaAppDriver}.
 */
public class JavaContainerSshDriver extends VanillaJavaAppSshDriver implements JavaContainerDriver {

    private static final String VIRTUAL_MACHINE_MX_BEAN = "com.waratek:type=VirtualMachine";
    private static final String VIRTUAL_CONTAINER_MX_BEAN = "com.waratek:type=%s,name=VirtualContainer";
    private static final String STATUS_RUNNING = "Running";

    private volatile JmxHelper jmxHelper;

    public JavaContainerSshDriver(JavaContainerImpl entity, SshMachineLocation machine) {
        super(entity, machine);

        JavaVM jvm = getEntity().getConfig(JavaContainer.JVM);
        jmxHelper = new JmxHelper(jvm.getAttribute(UsesJmx.JMX_URL));
        try {
            jmxHelper.connect();
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

    /** Does nothing; we are using the Waratek JVM instead. */
    @Override
    protected final List<String> getJmxJavaConfigOptions() { return Lists.newArrayList(); }

    /** Does nothing; we are using the Waratek JVM instead. */
    @Override
    public final boolean installJava() { return true; }

    /** Does nothing; we are using the Waratek JVM instead. */
    @Override
    public final void installJmxSupport() { }

    /** Does nothing; we are using the Waratek JVM instead. */
    @Override
    public final boolean isJmxEnabled() { return false; }

    /** Does nothing; we are using the Waratek JVM instead. */
    @Override
    public final boolean isJmxSslEnabled() { return false; }

    @Override
    public void install() {
        String jvc = getEntity().getAttribute(JavaContainer.JVC_NAME);
        log.info("Installing {}", jvc);

        String cmd = getJavaCommandLine();
        log.info("Command line: {}", cmd);
        try {
            ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(VIRTUAL_MACHINE_MX_BEAN));
            jmxHelper.operation(object.getObjectName(), "defineContainer", jvc, cmd, getRootDirectory());
        } catch (Exception e) {
            Exceptions.propagate(e);
        }
    }

    @Override
    public void launch() {
        String jvc = getEntity().getAttribute(JavaContainer.JVC_NAME);
        log.info("Launching {}", jvc);

        try {
            ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(VIRTUAL_MACHINE_MX_BEAN));
            jmxHelper.operation(object.getObjectName(), "startContainer", jvc);
        } catch (Exception e) {
            Exceptions.propagate(e);
        }
    }

    @Override
    public boolean isRunning() {
        String jvc = getEntity().getAttribute(JavaContainer.JVC_NAME);
        if (log.isTraceEnabled()) log.trace("Checking {}", jvc);

        try {
            ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(String.format(VIRTUAL_CONTAINER_MX_BEAN, jvc)));
            String status = (String) jmxHelper.getAttribute(object.getObjectName(), "Status");
            return STATUS_RUNNING.equals(status);
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
    }

    @Override
    public void stop() {
        String jvc = getEntity().getAttribute(JavaContainer.JVC_NAME);
        log.info("Stopping {}", jvc);

        try {
            ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(VIRTUAL_MACHINE_MX_BEAN));
            if (isRunning()) {
                jmxHelper.operation(object.getObjectName(), "shutdownContainer", jvc);
            }
            jmxHelper.operation(object.getObjectName(), "undefineContainer", jvc);
        } catch (Exception e) {
            Exceptions.propagate(e);
        }
        if (jmxHelper != null) jmxHelper.disconnect();
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
