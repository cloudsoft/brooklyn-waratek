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

import java.lang.management.MemoryUsage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.enricher.RollingTimeWindowMeanEnricher;
import brooklyn.enricher.TimeWeightedDeltaEnricher;
import brooklyn.entity.java.JavaAppUtils;
import brooklyn.entity.java.UsesJavaMXBeans;
import brooklyn.entity.java.UsesJmx;
import brooklyn.entity.java.VanillaJavaAppImpl;
import brooklyn.event.feed.http.HttpValueFunctions;
import brooklyn.event.feed.jmx.JmxAttributePollConfig;
import brooklyn.event.feed.jmx.JmxFeed;
import brooklyn.event.feed.jmx.JmxHelper;
import brooklyn.util.exceptions.Exceptions;
import brooklyn.util.text.Strings;
import brooklyn.util.time.Duration;

import com.google.common.base.Function;

public class JavaContainerImpl extends VanillaJavaAppImpl implements JavaContainer {

    private static final Logger log = LoggerFactory.getLogger(JavaContainerImpl.class);
    private static final AtomicInteger counter = new AtomicInteger(0);

    private JmxHelper jmxHelper;
    private JmxFeed jmxMxBeanFeed;

    @Override
    public void init() {
        log.info("Starting JVC id {}", getId());

        String jvcName = String.format(getConfig(JavaContainer.JVC_NAME_FORMAT), getId(), counter.incrementAndGet());
        setDisplayName(jvcName);
        setAttribute(JVC_NAME, jvcName);
    }

    @Override
    public String getJvcName() { return getAttribute(JVC_NAME); }

    @Override
    public JavaVM getJavaVM() { return getConfig(JVM); }

    @Override
    protected void connectSensors() {
        jmxHelper = new JmxHelper(getJavaVM().getAttribute(UsesJmx.JMX_URL));
        try {
            jmxHelper.connect();
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
        jmxMxBeanFeed = connectMXBeanSensors(Duration.FIVE_SECONDS);
        addEnricher(TimeWeightedDeltaEnricher.getPerSecondDeltaEnricher(this, UsesJavaMXBeans.USED_HEAP_MEMORY, WaratekJavaApp.HEAP_MEMORY_DELTA_PER_SECOND_LAST));
        addEnricher(new RollingTimeWindowMeanEnricher<Double>(this, WaratekJavaApp.HEAP_MEMORY_DELTA_PER_SECOND_LAST, WaratekJavaApp.HEAP_MEMORY_DELTA_PER_SECOND_IN_WINDOW, Duration.ONE_MINUTE));
        connectServiceUpIsRunning();
    }

    public String waratekMXBeanName(String type) {
        return String.format("com.waratek:type=%s,name=%s", getJvcName(), type);
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    public JmxFeed connectMXBeanSensors(Duration jmxPollPeriod) {
        JmxFeed jmxFeed = JmxFeed.builder()
                .helper(jmxHelper)
                .entity(this)
                .period(jmxPollPeriod)

                .pollAttribute(new JmxAttributePollConfig<Long>(UsesJavaMXBeans.USED_HEAP_MEMORY)
                        .objectName(waratekMXBeanName("Memory"))
                        .attributeName("HeapMemoryUsage")
                        .onSuccess((Function) HttpValueFunctions.chain(JavaAppUtils.compositeDataToMemoryUsage(), new Function<MemoryUsage, Long>() {
                            @Override public Long apply(MemoryUsage input) {
                                return (input == null) ? null : input.getUsed();
                            }})))
                .pollAttribute(new JmxAttributePollConfig<Long>(UsesJavaMXBeans.INIT_HEAP_MEMORY)
                        .objectName(waratekMXBeanName("Memory"))
                        .attributeName("HeapMemoryUsage")
                        .onSuccess((Function) HttpValueFunctions.chain(JavaAppUtils.compositeDataToMemoryUsage(), new Function<MemoryUsage, Long>() {
                            @Override public Long apply(MemoryUsage input) {
                                return (input == null) ? null : input.getInit();
                            }})))
                .pollAttribute(new JmxAttributePollConfig<Long>(UsesJavaMXBeans.COMMITTED_HEAP_MEMORY)
                        .objectName(waratekMXBeanName("Memory"))
                        .attributeName("HeapMemoryUsage")
                        .onSuccess((Function) HttpValueFunctions.chain(JavaAppUtils.compositeDataToMemoryUsage(), new Function<MemoryUsage, Long>() {
                            @Override public Long apply(MemoryUsage input) {
                                return (input == null) ? null : input.getCommitted();
                            }})))
                .pollAttribute(new JmxAttributePollConfig<Long>(UsesJavaMXBeans.MAX_HEAP_MEMORY)
                        .objectName(waratekMXBeanName("Memory"))
                        .attributeName("HeapMemoryUsage")
                        .onSuccess((Function) HttpValueFunctions.chain(JavaAppUtils.compositeDataToMemoryUsage(), new Function<MemoryUsage, Long>() {
                            @Override public Long apply(MemoryUsage input) {
                                return (input == null) ? null : input.getMax();
                            }})))
                .pollAttribute(new JmxAttributePollConfig<Long>(UsesJavaMXBeans.NON_HEAP_MEMORY_USAGE)
                        .objectName(waratekMXBeanName("Memory"))
                        .attributeName("NonHeapMemoryUsage")
                        .onSuccess((Function) HttpValueFunctions.chain(JavaAppUtils.compositeDataToMemoryUsage(), new Function<MemoryUsage, Long>() {
                            @Override public Long apply(MemoryUsage input) {
                                return (input == null) ? null : input.getUsed();
                            }})))

                .pollAttribute(new JmxAttributePollConfig<Integer>(UsesJavaMXBeans.CURRENT_THREAD_COUNT)
                        .objectName(waratekMXBeanName("Threading"))
                        .attributeName("ThreadCount"))
                .pollAttribute(new JmxAttributePollConfig<Integer>(UsesJavaMXBeans.PEAK_THREAD_COUNT)
                        .objectName(waratekMXBeanName("Threading"))
                        .attributeName("PeakThreadCount"))

                .pollAttribute(new JmxAttributePollConfig<Long>(UsesJavaMXBeans.START_TIME)
                        .objectName(waratekMXBeanName("Runtime"))
                        .period(60, TimeUnit.SECONDS)
                        .attributeName("StartTime"))
                .pollAttribute(new JmxAttributePollConfig<Long>(UsesJavaMXBeans.UP_TIME)
                        .objectName(waratekMXBeanName("Runtime"))
                        .period(60, TimeUnit.SECONDS)
                        .attributeName("Uptime"))

                .pollAttribute(new JmxAttributePollConfig<Long>(JavaContainer.BYTES_SENT)
                        .objectName(waratekMXBeanName("Info"))
                        .attributeName("BytesSent"))
                .pollAttribute(new JmxAttributePollConfig<Long>(JavaContainer.BYTES_RECEIVED)
                        .objectName(waratekMXBeanName("Info"))
                        .attributeName("BytesReceived"))
                .pollAttribute(new JmxAttributePollConfig<Integer>(JavaContainer.FILE_DESCRIPTOR_COUNT)
                        .objectName(waratekMXBeanName("Info"))
                        .attributeName("FileDescriptorCount"))
                .pollAttribute(new JmxAttributePollConfig<Double>(JavaContainer.CPU_USAGE)
                        .objectName(waratekMXBeanName("Info"))
                        .attributeName("CpuUsage"))
                .pollAttribute(new JmxAttributePollConfig<String>(JavaContainer.STATUS)
                        .objectName(waratekMXBeanName("Info"))
                        .attributeName("Status"))

                .pollAttribute(new JmxAttributePollConfig<Double>(UsesJavaMXBeans.SYSTEM_LOAD_AVERAGE)
                        .objectName(waratekMXBeanName("OperatingSystem"))
                        .attributeName("SystemLoadAverage")
                        .onSuccess((Function) new Function<Double, Double>() {
                            @Override public Double apply(Double input) {
                                return (input < 0d) ? null : input;
                            }}))
                .pollAttribute(new JmxAttributePollConfig<Integer>(UsesJavaMXBeans.AVAILABLE_PROCESSORS)
                        .objectName(waratekMXBeanName("OperatingSystem"))
                        .period(60, TimeUnit.SECONDS)
                        .attributeName("AvailableProcessors"))

                .build();
        return jmxFeed;
    }

    @Override
    public void postStart() {
        Long heapSize = getConfig(MAX_HEAP_SIZE);
        allocateHeap(heapSize);
    }

    @Override
    public void disconnectSensors() {
        disconnectServiceUpIsRunning();
        if (jmxMxBeanFeed != null) jmxMxBeanFeed.stop();
        if (jmxHelper != null) jmxHelper.disconnect();
    }

    @Override
    public void pause() {
        String jvc = getAttribute(JavaContainer.JVC_NAME);
        log.info("Pausing {}", jvc);

        try {
            ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(waratekMXBeanName("VirtualContainer")));
            jmxHelper.operation(object.getObjectName(), "suspendContainer");
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
    }

    @Override
    public void resume() {
        String jvc = getAttribute(JavaContainer.JVC_NAME);
        log.info("Resume {}", jvc);

        try {
            ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(waratekMXBeanName("VirtualContainer")));
            jmxHelper.operation(object.getObjectName(), "resumeContainer");
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
    }

    @Override
    public Long allocateHeap(Long size) {
        String jvc = getAttribute(JavaContainer.JVC_NAME);
        log.info("Allocate {} to {}", Strings.makeSizeString(size), jvc);

        try {
            ObjectInstance object = jmxHelper.findMBean(ObjectName.getInstance(waratekMXBeanName("Memory")));
            Long oldSize = (Long) jmxHelper.getAttribute(object.getObjectName(), "MaximumHeapMemorySize");
            jmxHelper.setAttribute(object.getObjectName(), "MaximumHeapMemorySize", size);
            Long newSize = (Long) jmxHelper.getAttribute(object.getObjectName(), "MaximumHeapMemorySize");
            if (log.isDebugEnabled()) {
                log.debug("Changed max heap from {} to {}", Strings.makeSizeString(oldSize), Strings.makeSizeString(newSize));
            }
            setAttribute(MAX_HEAP_SIZE, newSize);
            return oldSize;
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
    }

    @Override
    public Class<? extends JavaContainerDriver> getDriverInterface() {
        return JavaContainerDriver.class;
    }

    @Override
    public String getShortName() { return "JVC"; }

}
