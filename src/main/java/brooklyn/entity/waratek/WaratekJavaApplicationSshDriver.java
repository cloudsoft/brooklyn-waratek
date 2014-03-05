package brooklyn.entity.waratek;

import brooklyn.entity.java.VanillaJavaAppSshDriver;
import brooklyn.entity.waratek.cloudvm.JavaVirtualContainer;
import brooklyn.entity.waratek.cloudvm.JavaVirtualMachine;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.location.waratek.WaratekContainerLocation;
import brooklyn.util.os.Os;

import com.google.common.base.Optional;

/**
 * The SSH implementation of the {@link WaratekJavaAppDriver}.
 */
public class WaratekJavaApplicationSshDriver extends VanillaJavaAppSshDriver implements WaratekJavaApplicationDriver {

    private JavaVirtualMachine jvm;
    private JavaVirtualContainer jvc;

    public WaratekJavaApplicationSshDriver(WaratekJavaApplicationImpl entity, SshMachineLocation machine) {
        super(entity, machine);
        jvc = ((WaratekContainerLocation) machine).getJavaVirtualContainer();
        jvm = ((WaratekContainerLocation) machine).getJavaVirtualMachine();
    }

    @Override
    protected String getLogFileLocation() {
        return Os.mergePaths(jvm.getRootDirectory(), "var/log/javad", jvm.getJvmName(), jvc.getJvcName(), "console.log");
    }

    @Override
    protected Optional<String> getCurrentJavaVersion() {
        // TODO call JMX to get Waratek JVM version details
        return Optional.of("1.6.0");
    }

    /** Does nothing; we are using the Waratek JVM instead. */
    @Override
    public final boolean installJava() { return true; }

    @Override
    public String getHeapSize() {
        Long heapSize = getEntity().getConfig(WaratekJavaApplication.MAX_HEAP_SIZE, 512 * (1024L * 1024L));
        int megabytes = (int) (heapSize / (1024L * 1024L));
        return megabytes + "m";
    }

    @Override
    public String getRootDirectory() {
        return getRunDir();
    }

}
