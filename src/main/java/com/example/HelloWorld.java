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
package com.example;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class HelloWorld {

    private static final AtomicLong counter = new AtomicLong(0L);
    private static final List<Object> data = new LinkedList<Object>();

    /**
     * Example 'Hello, World!' application.
     * <p>
     * Called with no arguments will print 'Hello, World!' on standard output
     * with an incrementing counter, every second. If an integer argument is given,
     * then this number of bytes will be allocated and stored in a collection on 
     * the heap each iteration as well.
     */
    public static void main(String...argv) {
        int allocate = 0;
        if (argv.length > 0) {
            allocate = Integer.valueOf(argv[0]);
        }
        while (true) {
            System.out.printf("Hello, World! %d\n", counter.incrementAndGet());
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                Thread.interrupted();
                System.exit(1);
            }
            if (allocate > 0) {
                data.add(new byte[allocate]);
            }
        }
    }

}
