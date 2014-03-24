package brooklyn.entity.waratek.cloudvm;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectInstance;
import javax.management.ObjectName;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.java.UsesJmx;
import brooklyn.event.feed.jmx.JmxHelper;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.ResourceUtils;
import brooklyn.util.exceptions.Exceptions;
import brooklyn.util.os.Os;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;

/**
 * The SSH implementation of the {@link WaratekJavaAppDriver}.
 */
public class JavaVirtualContainerSshDriver extends AbstractSoftwareProcessSshDriver implements JavaVirtualContainerDriver {

    public static final String VIRTUAL_MACHINE_MX_BEAN = "com.waratek:type=VirtualMachine";
    public static final String VIRTUAL_CONTAINER_MX_BEAN = "com.waratek:type=%s,name=VirtualContainer";

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
        String jvc = getJvcName();
        if (log.isDebugEnabled()) log.debug("Setup {}", jvc);

        // Copy the container Jar file if not already in the install directory
        int exists = newScript(INSTALLING)
            .body.append("test -f brooklyn-waratek-container.jar")
            .requireResultCode(Predicates.in(ImmutableSet.of(0, 1)))
            .execute();
        if (exists != 0) {
            getMachine().copyTo(ResourceUtils.create(this).getResourceFromUrl("classpath://brooklyn-waratek-container.jar"),
                    Os.mergePaths(getInstallDir(), "brooklyn-waratek-container.jar"));
        }
    }

    @Override
    public void customize() {
        String jvc = getJvcName();
        if (log.isDebugEnabled()) log.debug("Creating {}", jvc);

        String command = String.format("java -cp %s com.waratek.Brooklyn %s", Os.mergePaths(getInstallDir(), "brooklyn-waratek-container.jar"), jvc);
        try {
            ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(VIRTUAL_MACHINE_MX_BEAN));
            WaratekUtils.sleep(new Random().nextDouble());
            jmxHelper.operation(object.getObjectName(), "defineContainer", jvc, command, getRootDirectory());
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
    }

    @Override
    public void launch() {
        String jvc = getJvcName();
        if (log.isDebugEnabled()) log.debug("Starting {}", jvc);

        try {
            ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(VIRTUAL_MACHINE_MX_BEAN));
            jmxHelper.operation(object.getObjectName(), "startContainer", jvc);
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
    }

    @Override
    public boolean isRunning() {
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
    }

    @Override
    public void stop() {
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
