/*
 * Copyright 2014 by Cloudsoft Corp.
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
package com.waratek.cloudvm;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.StartableApplication;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.util.CommandLineUtil;
import brooklyn.util.exceptions.Exceptions;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static final String DEFAULT_LOCATION = "localhost";

    public static void main(String...argv) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Brooklyn launcher starting. Arguments: " + Arrays.toString(argv));
        }

        List<String> args = Lists.newArrayList(argv);

        if (args.remove("--help") || args.remove("-h") || args.remove("help") || args.isEmpty()) {
            if (!args.isEmpty()) {
                log.warn("Invalid options used with 'help': "+Joiner.on(" ").join(args));
            }
            showHelp();
            System.exit(0);
        }

        String command = args.remove(0);

        String port = CommandLineUtil.getCommandLineOption(args, "--port", "8081+");
        String location = CommandLineUtil.getCommandLineOption(args, "--location");
        String applicationClassName = CommandLineUtil.getCommandLineOption(args, "--class");

        if (!args.isEmpty()) {
            warnAndExit("Unknown options: " + Joiner.on(" ").join(args));
        }

        // load the right application class, if needed
        Class<? extends StartableApplication> applicationClass = null;
        if ("application".equals(command)) {
            if (applicationClassName == null) {
                warnAndExit("Missing option: `--class <Class>` must be supplied for command `application`");
            } else {
                applicationClass = loadAppClass(applicationClassName);
            }
        } else {
            if (applicationClassName != null) {
                warnAndExit("Invalid option: `--class <Class>` is only valid for command `application`");
            }

            if ("launch".equals(command)) {
                applicationClass = CloudVM.class;
            } else if ("server".equals(command)) {
                // no-op
            } else {
                warnAndExit("Invalid command: `"+command+"` is not supported as a <command>");
            }
        }

        BrooklynLauncher launcher = BrooklynLauncher.newInstance();
        launcher.webconsolePort(port);

        if (applicationClass != null) {
            launcher.application(EntitySpec.create(StartableApplication.class, applicationClass).displayName("CloudVM"));
            launcher.location(location != null ? location : DEFAULT_LOCATION);
        } else {
            if (location != null) {
                warnAndExit("Invalid option: `--location <Location>` is only valid when starting an app");
            }
        }

        launcher.start();

        if (applicationClass != null) {
            Entities.dumpInfo(launcher.getApplications());
            log.info("Brooklyn start of "+command+" "+applicationClass.getSimpleName()+" completed; " +
            		"console at "+launcher.getServerDetails().getWebServerUrl());
        }
    }

    @SuppressWarnings("unchecked")
    protected static Class<? extends StartableApplication> loadAppClass(String applicationClassName) {
        try {
            return (Class<? extends StartableApplication>) Class.forName(applicationClassName);
        } catch (ClassNotFoundException e) {
            log.error("Could not load '" + applicationClassName + " (rethrowing): " + e);
            throw Exceptions.propagate(e);
        }
    }

    protected static void showHelp() {
        System.out.println();
        System.out.println("Usage: ./start.sh <command> [ --port port ] [ --location location ] [ --class class ]");
        System.out.println();
        System.out.println("where <command> is any of:");
        System.out.println("        server       start the Brooklyn server (no applications)");
        System.out.println("        application  start the applicaton specified by the --class option");
        System.out.println("        help         display this help list");
        System.out.println("        launch       start the CloudVM appplication");
        System.out.println();
    }

    protected static void warnAndExit(String message) {
        log.warn(message);
        showHelp();
        System.err.println("ERROR (exiting): "+message);
        System.err.println();
        System.exit(1);
    }

}
