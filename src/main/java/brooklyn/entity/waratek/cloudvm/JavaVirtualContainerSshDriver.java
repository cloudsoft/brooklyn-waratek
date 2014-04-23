package brooklyn.entity.waratek.cloudvm;

import static brooklyn.entity.waratek.cloudvm.WaratekUtils.VIRTUAL_CONTAINER_MX_BEAN;
import static brooklyn.entity.waratek.cloudvm.WaratekUtils.VIRTUAL_MACHINE_MX_BEAN;

import java.util.concurrent.TimeUnit;

import javax.management.ObjectInstance;
import javax.management.ObjectName;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.java.UsesJmx;
import brooklyn.event.feed.jmx.JmxHelper;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.exceptions.Exceptions;
import brooklyn.util.os.Os;

/**
 * The SSH implementation of the {@link WaratekJavaAppDriver}.
 */
public class JavaVirtualContainerSshDriver extends AbstractSoftwareProcessSshDriver implements JavaVirtualContainerDriver {

    private volatile JmxHelper jmxHelper;

    public JavaVirtualContainerSshDriver(JavaVirtualContainerImpl entity, SshMachineLocation machine) {
        super(entity, machine);

        // Wait until the JVM has started up
        JavaVirtualMachine jvm = getEntity().getConfig(JavaVirtualContainer.JVM);
        Entities.waitForServiceUp(jvm, jvm.getConfig(JavaVirtualMachine.START_TIMEOUT), TimeUnit.SECONDS);

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

    public String getJvcName() { return getEntity().getAttribute(JavaVirtualContainer.JVC_NAME); }

    @Override
    public void install() {
        // Does nothing
    }

    @Override
    public void customize() {
        try {
            getMachine().acquireMutex("exec", "customize");

            String jvc = getJvcName();
            if (log.isDebugEnabled()) log.debug("Creating {}", jvc);

            String command = String.format("java -cp %s com.waratek.Brooklyn %s", Os.mergePaths(getInstallDir(), "brooklyn-waratek-container.jar"), jvc);
            try {
                ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(VIRTUAL_MACHINE_MX_BEAN));
                jmxHelper.operation(object.getObjectName(), "defineContainer", jvc, command, getInstallDir());
            } catch (Exception e) {
                throw Exceptions.propagate(e);
            }
        } catch (InterruptedException ie) {
            throw Exceptions.propagate(ie);
        } finally {
            getMachine().releaseMutex("exec");
        }
    }

    @Override
    public void launch() {
        try {
            getMachine().acquireMutex("exec", "customize");

            String jvc = getJvcName();
            if (log.isDebugEnabled()) log.debug("Starting {}", jvc);

            try {
                ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(VIRTUAL_MACHINE_MX_BEAN));
                jmxHelper.operation(object.getObjectName(), "startContainer", jvc);
            } catch (Exception e) {
                throw Exceptions.propagate(e);
            }
        } catch (InterruptedException ie) {
            throw Exceptions.propagate(ie);
        } finally {
            getMachine().releaseMutex("exec");
        }
    }

    @Override
    public boolean isRunning() {
        try {
            getMachine().acquireMutex("exec", "customize");

            String jvc = getJvcName();
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
        } catch (InterruptedException ie) {
            throw Exceptions.propagate(ie);
        } finally {
            getMachine().releaseMutex("exec");
        }
    }

    @Override
    public void stop() {
        try {
            getMachine().acquireMutex("exec", "customize");

            String jvc = getJvcName();
            if (log.isDebugEnabled()) log.debug("Stopping {}", jvc);

            try {
                ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(String.format(VIRTUAL_CONTAINER_MX_BEAN, jvc)));
                if (object != null) {
                    getEntity().shutDown();
                    jmxHelper.operation(object.getObjectName(), "undefineContainer");
                }
            } catch (Exception e) {
                throw Exceptions.propagate(e);
            }
            if (jmxHelper != null) jmxHelper.disconnect();
        } catch (InterruptedException ie) {
            throw Exceptions.propagate(ie);
        } finally {
            getMachine().releaseMutex("exec");
        }
    }

    @Override
    public String getHeapSize() {
        Long heapSize = getEntity().getConfig(JavaVirtualContainer.MAX_HEAP_SIZE);
        int megabytes = (int) (heapSize / (1024L * 1024L));
        return megabytes + "m";
    }

    @Override
    public JavaVirtualContainerImpl getEntity() {
        return (JavaVirtualContainerImpl) super.getEntity();
    }

    @Override
    public String getRootDirectory() {
        return getRunDir();
    }

}
