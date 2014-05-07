package brooklyn.entity.waratek.cloudvm;

import static brooklyn.entity.waratek.cloudvm.WaratekUtils.VIRTUAL_CONTAINER_MX_BEAN;
import static brooklyn.entity.waratek.cloudvm.WaratekUtils.VIRTUAL_MACHINE_MX_BEAN;

import javax.management.ObjectInstance;
import javax.management.ObjectName;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.java.UsesJmx;
import brooklyn.event.feed.jmx.JmxHelper;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.location.waratek.WaratekContainerLocation;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.exceptions.Exceptions;
import brooklyn.util.internal.ssh.ShellTool;
import brooklyn.util.os.Os;
import brooklyn.util.ssh.BashCommands;
import brooklyn.util.text.ByteSizeStrings;
import brooklyn.util.text.Strings;

/**
 * The SSH implementation of the {@link WaratekJavaAppDriver}.
 */
public class JavaVirtualContainerSshDriver extends AbstractSoftwareProcessSshDriver implements JavaVirtualContainerDriver {

    private volatile JmxHelper jmxHelper;
    private final static String JAF_RULE_FILE_NAME = "rules.jaf";

    public JavaVirtualContainerSshDriver(JavaVirtualContainerImpl entity, SshMachineLocation machine) {
        super(entity, machine);

        // Wait until the JVM has started up
        JavaVirtualMachine jvm = getEntity().getConfig(JavaVirtualContainer.JVM);
        Entities.waitForServiceUp(jvm);

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

    }

    String getJafRulesUrl(){
        return getEntity().getConfig(JavaVirtualContainer.JAF_RULES_FILE_URL);
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

            deployJafRuleFile(getEntity().getConfig(JavaVirtualContainer.JAF_RULES_FILE_URL));
        } catch (InterruptedException ie) {
            throw Exceptions.propagate(ie);
        } finally {
            getMachine().releaseMutex("exec");
        }
    }

    @Override
    public void launch() {
        try {
            getMachine().acquireMutex("exec", "launch");

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
            getMachine().acquireMutex("exec", "isRunning");

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

    private String getJvcLibLocation() {
        JavaVirtualMachine jvm = getEntity().getConfig(JavaVirtualContainer.JVM);
        return Os.mergePaths(jvm.getLibDirectory(), "javad", jvm.getJvmName(), getJvcName());
    }

    @Override
    public void deployJafRuleFile(String fileUrl) {
        try {
            getMachine().acquireMutex("exec", "stop");

            if (log.isDebugEnabled()) {
                log.debug("Updating JAF rules file {} at {}", fileUrl, getJvcName());
            }

            if (Strings.isNonEmpty(fileUrl)) {
                String copyLocation = Os.mergePaths(getJvcLibLocation(), JAF_RULE_FILE_NAME);
                int result = getMachine().installTo(MutableMap.of(ShellTool.PROP_RUN_AS_ROOT.getName(), Boolean.TRUE), getJafRulesUrl(), copyLocation);
                if (result != 0) {
                    throw new IllegalStateException("Failed to deploy JAF rules from " + fileUrl);
                }
            } else {
                // no rules file so call to delete in case there is an existing one.
                newScript(CUSTOMIZING + "-rules")
                        .failOnNonZeroResultCode()
                        .body.append(BashCommands.sudo("rm -f " + Os.mergePaths(getJvcLibLocation(), JAF_RULE_FILE_NAME)))
                        .execute();
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
            getMachine().acquireMutex("exec", "stop");

            String jvc = getJvcName();
            if (log.isDebugEnabled()) log.debug("Stopping {}", jvc);

            try {
                ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(String.format(VIRTUAL_CONTAINER_MX_BEAN, jvc)));
                if (object != null) {
                    getEntity().shutDown();
                    jmxHelper.operation(object.getObjectName(), "undefineContainer");
                    WaratekContainerLocation container = getEntity().getDynamicLocation();
                    if (container != null) container.setEntity(null);
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
        Long size = getEntity().getConfig(JavaVirtualContainer.MAX_HEAP_SIZE);
        String xmx = ByteSizeStrings.java().apply(size);
        return xmx;
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
