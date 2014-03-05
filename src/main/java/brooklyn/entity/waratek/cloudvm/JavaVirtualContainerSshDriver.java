package brooklyn.entity.waratek.cloudvm;

import javax.management.ObjectInstance;
import javax.management.ObjectName;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.java.UsesJmx;
import brooklyn.event.feed.jmx.JmxHelper;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.exceptions.Exceptions;

/**
 * The SSH implementation of the {@link WaratekJavaAppDriver}.
 */
public class JavaVirtualContainerSshDriver extends AbstractSoftwareProcessSshDriver implements JavaVirtualContainerDriver {

    public static final String VIRTUAL_MACHINE_MX_BEAN = "com.waratek:type=VirtualMachine";
    public static final String VIRTUAL_CONTAINER_MX_BEAN = "com.waratek:type=%s,name=VirtualContainer";
    public static final String STATUS_RUNNING = "Running";
    public static final String STATUS_SHUT_OFF = "Shut Off";

    private volatile JmxHelper jmxHelper;

    public JavaVirtualContainerSshDriver(JavaVirtualContainerImpl entity, SshMachineLocation machine) {
        super(entity, machine);

        JavaVirtualMachine jvm = getEntity().getConfig(JavaVirtualContainer.JVM);
        jmxHelper = new JmxHelper(jvm.getAttribute(UsesJmx.JMX_URL));
        try {
            jmxHelper.connect();
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
    }

    /** Does nothing; we are using the Waratek JVM instead. */
    @Override
    public final boolean isJmxEnabled() { return false; }

    @Override
    public void install() {
        String jvc = getEntity().getAttribute(JavaVirtualContainer.JVC_NAME);
        log.info("Installing {}", jvc);

        try {
            ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(VIRTUAL_MACHINE_MX_BEAN));
            jmxHelper.operation(object.getObjectName(), "defineContainer", jvc, "java", getRootDirectory());
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
    }

    @Override
    public void launch() {
        String jvc = getEntity().getAttribute(JavaVirtualContainer.JVC_NAME);
        log.info("Launching {}", jvc);

        try {
            ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(VIRTUAL_MACHINE_MX_BEAN));
            jmxHelper.operation(object.getObjectName(), "startContainer", jvc);
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
    }

    @Override
    public boolean isRunning() {
        String jvc = getEntity().getAttribute(JavaVirtualContainer.JVC_NAME);
        if (log.isTraceEnabled()) log.trace("Checking {}", jvc);

        try {
            ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(String.format(VIRTUAL_CONTAINER_MX_BEAN, jvc)));
            if (object != null) {
                String status = (String) jmxHelper.getAttribute(object.getObjectName(), "Status");
                return status != null; // As long as a status is returned, OK
            } else {
                return false;
            }
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
    }

    @Override
    public void stop() {
        String jvc = getEntity().getAttribute(JavaVirtualContainer.JVC_NAME);
        log.info("Stopping {}", jvc);

        try {
            ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(String.format(VIRTUAL_CONTAINER_MX_BEAN, jvc)));
            if (object != null) {
                String status = (String) jmxHelper.getAttribute(object.getObjectName(), "Status");
                if (!STATUS_SHUT_OFF.equals(status)) {
                    jmxHelper.operation(object.getObjectName(), "shutdownContainer");
                }
                jmxHelper.operation(object.getObjectName(), "undefineContainer");
            }
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
        if (jmxHelper != null) jmxHelper.disconnect();
    }

    @Override
    public String getHeapSize() {
        Long heapSize = getEntity().getConfig(JavaVirtualContainer.MAX_HEAP_SIZE);
        int megabytes = (int) (heapSize / (1024L * 1024L));
        return megabytes + "m";
    }

    @Override
    public String getRootDirectory() {
        return getRunDir();
    }

    @Override
    public void customize() {

    }

}
