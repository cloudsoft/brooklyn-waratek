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

import java.util.concurrent.atomic.AtomicBoolean;

import brooklyn.config.render.RendererHints;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.Sensors;
import brooklyn.util.text.ByteSizeStrings;

public class WaratekAttributes  {

    /*
     * JVC attributes available through the waratek MXBeans.
     */

    public static final AttributeSensor<Long> BYTES_SENT = Sensors.newLongSensor("waratek.jvc.bytesSent", "Total network bytes sent");
    public static final AttributeSensor<Long> BYTES_RECEIVED = Sensors.newLongSensor("waratek.jvc.bytesReceived", "Total network bytes received");
    public static final AttributeSensor<Integer> FILE_DESCRIPTOR_COUNT = Sensors.newIntegerSensor("waratek.jvc.fileDescriptorCount", "Current open file descriptors");
    public static final AttributeSensor<Double> CPU_USAGE = Sensors.newDoubleSensor("waratek.jvc.cpuUsage", "Current CPU usage");
    public static final AttributeSensor<String> STATUS = Sensors.newStringSensor("waratek.jvc.status", "Current JVC status");

    /*
     * Aggregate sensor attributes accumulated from the JVC clusters.
     */

    public static final AttributeSensor<Long> TOTAL_HEAP_MEMORY = Sensors.newLongSensor("waratek.heapMemory.total", "Total aggregated heap memory usage");
    public static final AttributeSensor<Double> HEAP_MEMORY_DELTA_PER_SECOND_LAST = Sensors.newDoubleSensor("waratek.heapMemoryDelta.last", "Change in heap memory usage per second");
    public static final AttributeSensor<Double> HEAP_MEMORY_DELTA_PER_SECOND_IN_WINDOW = Sensors.newDoubleSensor("waratek.heapMemoryDelta.windowed", "Average change in heap memory usage over 30s");
    public static final AttributeSensor<Double> AVERAGE_CPU_USAGE = Sensors.newDoubleSensor("waratek.cpuUsage.average", "Average CPU usage across the cluster");

    /*
     * Counter attributes.
     */

    public static final AttributeSensor<Integer> JVM_COUNT = Sensors.newIntegerSensor("waratek.jvmCount", "Number of JVMs");
    public static final AttributeSensor<Integer> JVC_COUNT = Sensors.newIntegerSensor("waratek.jvcCount", "Number of JVCs");

    private static AtomicBoolean initialized = new AtomicBoolean(false);

    /** Setup renderer hints for the MXBean attributes. */
    @SuppressWarnings("rawtypes")
    public static void init() {
        if (initialized.get()) return;
        synchronized (initialized) {
            if (initialized.get()) return;

            RendererHints.register(TOTAL_HEAP_MEMORY, new RendererHints.DisplayValue(ByteSizeStrings.metric()));
            RendererHints.register(HEAP_MEMORY_DELTA_PER_SECOND_LAST, new RendererHints.DisplayValue(ByteSizeStrings.metric()));
            RendererHints.register(HEAP_MEMORY_DELTA_PER_SECOND_IN_WINDOW, new RendererHints.DisplayValue(ByteSizeStrings.metric()));
            RendererHints.register(BYTES_SENT, new RendererHints.DisplayValue(ByteSizeStrings.metric()));
            RendererHints.register(BYTES_RECEIVED, new RendererHints.DisplayValue(ByteSizeStrings.metric()));

            initialized.set(true);
        }
    }

    static {
        init();
    }
}