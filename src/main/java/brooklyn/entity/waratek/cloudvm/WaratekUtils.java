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
package brooklyn.entity.waratek.cloudvm;

import java.lang.management.MemoryUsage;
import java.util.concurrent.TimeUnit;

import brooklyn.enricher.RollingTimeWindowMeanEnricher;
import brooklyn.enricher.TimeWeightedDeltaEnricher;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.entity.java.JavaAppUtils;
import brooklyn.entity.java.UsesJavaMXBeans;
import brooklyn.event.feed.http.HttpValueFunctions;
import brooklyn.event.feed.jmx.JmxAttributePollConfig;
import brooklyn.event.feed.jmx.JmxFeed;
import brooklyn.event.feed.jmx.JmxHelper;
import brooklyn.util.time.Duration;

import com.google.common.base.Function;

public class WaratekUtils  {

    public static String waratekMXBeanName(String jvcName, String type) {
        return String.format("com.waratek:type=%s,name=%s", jvcName, type);
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    public static JmxFeed connectMXBeanSensors(JmxHelper helper, EntityLocal entity, Duration jmxPollPeriod) {
        String jvcName = entity.getAttribute(JavaVirtualContainer.JVC_NAME);
        JmxFeed jmxFeed = JmxFeed.builder()
                .helper(helper)
                .entity(entity)
                .period(jmxPollPeriod)

                .pollAttribute(new JmxAttributePollConfig<Long>(UsesJavaMXBeans.USED_HEAP_MEMORY)
                        .objectName(waratekMXBeanName(jvcName, "Memory"))
                        .attributeName("HeapMemoryUsage")
                        .onSuccess((Function) HttpValueFunctions.chain(JavaAppUtils.compositeDataToMemoryUsage(), new Function<MemoryUsage, Long>() {
                            @Override public Long apply(MemoryUsage input) {
                                return (input == null) ? null : input.getUsed();
                            }})))
                .pollAttribute(new JmxAttributePollConfig<Long>(UsesJavaMXBeans.INIT_HEAP_MEMORY)
                        .objectName(waratekMXBeanName(jvcName, "Memory"))
                        .attributeName("HeapMemoryUsage")
                        .onSuccess((Function) HttpValueFunctions.chain(JavaAppUtils.compositeDataToMemoryUsage(), new Function<MemoryUsage, Long>() {
                            @Override public Long apply(MemoryUsage input) {
                                return (input == null) ? null : input.getInit();
                            }})))
                .pollAttribute(new JmxAttributePollConfig<Long>(UsesJavaMXBeans.COMMITTED_HEAP_MEMORY)
                        .objectName(waratekMXBeanName(jvcName, "Memory"))
                        .attributeName("HeapMemoryUsage")
                        .onSuccess((Function) HttpValueFunctions.chain(JavaAppUtils.compositeDataToMemoryUsage(), new Function<MemoryUsage, Long>() {
                            @Override public Long apply(MemoryUsage input) {
                                return (input == null) ? null : input.getCommitted();
                            }})))
                .pollAttribute(new JmxAttributePollConfig<Long>(UsesJavaMXBeans.MAX_HEAP_MEMORY)
                        .objectName(waratekMXBeanName(jvcName, "Memory"))
                        .attributeName("HeapMemoryUsage")
                        .onSuccess((Function) HttpValueFunctions.chain(JavaAppUtils.compositeDataToMemoryUsage(), new Function<MemoryUsage, Long>() {
                            @Override public Long apply(MemoryUsage input) {
                                return (input == null) ? null : input.getMax();
                            }})))
                .pollAttribute(new JmxAttributePollConfig<Long>(UsesJavaMXBeans.NON_HEAP_MEMORY_USAGE)
                        .objectName(waratekMXBeanName(jvcName, "Memory"))
                        .attributeName("NonHeapMemoryUsage")
                        .onSuccess((Function) HttpValueFunctions.chain(JavaAppUtils.compositeDataToMemoryUsage(), new Function<MemoryUsage, Long>() {
                            @Override public Long apply(MemoryUsage input) {
                                return (input == null) ? null : input.getUsed();
                            }})))

                .pollAttribute(new JmxAttributePollConfig<Integer>(UsesJavaMXBeans.CURRENT_THREAD_COUNT)
                        .objectName(waratekMXBeanName(jvcName, "Threading"))
                        .attributeName("ThreadCount"))
                .pollAttribute(new JmxAttributePollConfig<Integer>(UsesJavaMXBeans.PEAK_THREAD_COUNT)
                        .objectName(waratekMXBeanName(jvcName, "Threading"))
                        .attributeName("PeakThreadCount"))

                .pollAttribute(new JmxAttributePollConfig<Long>(UsesJavaMXBeans.START_TIME)
                        .objectName(waratekMXBeanName(jvcName, "Runtime"))
                        .period(60, TimeUnit.SECONDS)
                        .attributeName("StartTime"))
                .pollAttribute(new JmxAttributePollConfig<Long>(UsesJavaMXBeans.UP_TIME)
                        .objectName(waratekMXBeanName(jvcName, "Runtime"))
                        .period(60, TimeUnit.SECONDS)
                        .attributeName("Uptime"))

                .pollAttribute(new JmxAttributePollConfig<Long>(WaratekAttributes.BYTES_SENT)
                        .objectName(waratekMXBeanName(jvcName, "Info"))
                        .attributeName("BytesSent"))
                .pollAttribute(new JmxAttributePollConfig<Long>(WaratekAttributes.BYTES_RECEIVED)
                        .objectName(waratekMXBeanName(jvcName, "Info"))
                        .attributeName("BytesReceived"))
                .pollAttribute(new JmxAttributePollConfig<Integer>(WaratekAttributes.FILE_DESCRIPTOR_COUNT)
                        .objectName(waratekMXBeanName(jvcName, "Info"))
                        .attributeName("FileDescriptorCount"))
                .pollAttribute(new JmxAttributePollConfig<Double>(WaratekAttributes.CPU_USAGE)
                        .objectName(waratekMXBeanName(jvcName, "Info"))
                        .attributeName("CpuUsage"))
                .pollAttribute(new JmxAttributePollConfig<String>(WaratekAttributes.STATUS)
                        .objectName(waratekMXBeanName(jvcName, "Info"))
                        .attributeName("Status"))

                .pollAttribute(new JmxAttributePollConfig<Double>(UsesJavaMXBeans.SYSTEM_LOAD_AVERAGE)
                        .objectName(waratekMXBeanName(jvcName, "OperatingSystem"))
                        .attributeName("SystemLoadAverage")
                        .onSuccess((Function) new Function<Double, Double>() {
                            @Override public Double apply(Double input) {
                                return (input < 0d) ? null : input;
                            }}))
                .pollAttribute(new JmxAttributePollConfig<Integer>(UsesJavaMXBeans.AVAILABLE_PROCESSORS)
                        .objectName(waratekMXBeanName(jvcName, "OperatingSystem"))
                        .period(60, TimeUnit.SECONDS)
                        .attributeName("AvailableProcessors"))

                .build();
        return jmxFeed;
    }

    public static void connectEnrichers(EntityLocal entity) {
        entity.addEnricher(TimeWeightedDeltaEnricher.getPerSecondDeltaEnricher(entity, UsesJavaMXBeans.USED_HEAP_MEMORY, WaratekAttributes.HEAP_MEMORY_DELTA_PER_SECOND_LAST));
        entity.addEnricher(new RollingTimeWindowMeanEnricher<Double>(entity, WaratekAttributes.HEAP_MEMORY_DELTA_PER_SECOND_LAST, WaratekAttributes.HEAP_MEMORY_DELTA_PER_SECOND_IN_WINDOW, Duration.ONE_MINUTE));
    }

}
