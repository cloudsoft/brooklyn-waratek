package brooklyn.entity.waratek;

import static java.lang.String.format;

import java.util.List;

import javax.management.ObjectInstance;
import javax.management.ObjectName;

import brooklyn.entity.java.UsesJmx;
import brooklyn.entity.java.VanillaJavaAppSshDriver;
import brooklyn.event.feed.jmx.JmxHelper;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.exceptions.Exceptions;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * The SSH implementation of the {@link WaratekJavaAppDriver}.
 */
public class JavaContainerSshDriver extends VanillaJavaAppSshDriver implements JavaContainerDriver {

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

    protected String getLogFileLocation() {
        return format("%s/console", getRunDir());
    }

    public String getJavaCommandLine() {
        String javaOpts = getShellEnvironment().get("JAVA_OPTS");
        String args = getArgs();
        
        StringBuilder command = new StringBuilder()
                .append("java")
                .append(" ")
                .append(javaOpts)
                .append(" ")
                .append("-cp lib/*")
                .append(" ")
                .append(args);
        return command.toString();
    }

    @Override
    protected Optional<String> getCurrentJavaVersion() {
        // TODO call JMX to get Waratek JVM version details
        return Optional.of("1.6.0");
    }

    // Not used by JVC
    @Override
    protected List<String> getJmxJavaConfigOptions() { return Lists.newArrayList(); }

    // Not used by JVC
    @Override
    public boolean installJava() { return true; }

    // Not used by JVC
    @Override
    public void installJmxSupport() { }

    @Override
    public void install() {
        String jvc = getEntity().getAttribute(JavaContainer.JVC_NAME);
        log.info("Installing {}", jvc);

        String cmd = getJavaCommandLine();
        log.info("Command line: {}", cmd);
        try {
            ObjectInstance object = helper.findMBean(ObjectName.getInstance(""));
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
            ObjectInstance object = helper.findMBean(ObjectName.getInstance(""));
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

        // JMX operation to destroy JVC
    }

    @Override
    public void kill() {
        String jvc = getEntity().getAttribute(JavaContainer.JVC_NAME);
        log.info("Killling {}", jvc);

        // JMX operation to undefine JVC
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
