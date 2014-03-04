package com.waratek.cloudvm;

import io.airlift.command.Command;
import io.airlift.command.Option;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.catalog.BrooklynCatalog;

import com.google.common.base.Objects.ToStringHelper;

/**
 * This class provides a static main entry point for launching a custom Brooklyn-based app.
 * <p>
 * It inherits the standard Brooklyn CLI options from {@link Main},
 * plus adds a few more shortcuts for favourite blueprints to the {@link LaunchCommand}.
 */
public class Main extends brooklyn.cli.Main {
    
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    
    public static final String DEFAULT_LOCATION = "waratek";

    public static void main(String... args) {
        log.debug("CLI invoked with args "+Arrays.asList(args));
        new Main().execCli(args);
    }

    @Override
    protected String cliScriptName() {
        return "start.sh";
    }
    
    @Override
    protected Class<? extends BrooklynCommand> cliLaunchCommand() {
        return LaunchCommand.class;
    }

    @Command(name = "launch", description = "Starts a server, and optionally an application. "
        + "Use --infrastructure or --application to launch the Waratek infrastructure or a simple application.")
    public static class LaunchCommand extends brooklyn.cli.Main.LaunchCommand {

        @Option(name = { "--infrastructure" }, description = "Launch a basic Waratek infrastructure")
        public boolean infrastructure;

        @Option(name = { "--application" }, description = "Launch a simple waratek application")
        public boolean application;

        @Override
        public Void call() throws Exception {
            // process our CLI arguments
            if (infrastructure) setAppToLaunch(BasicInfrastructure.class.getCanonicalName());
            if (application) setAppToLaunch(SimpleJavaApplication.class.getCanonicalName());
            
            // now process the standard launch arguments
            return super.call();
        }

        @Override
        protected void populateCatalog(BrooklynCatalog catalog) {
            super.populateCatalog(catalog);
            catalog.addItem(BasicInfrastructure.class);
            catalog.addItem(SimpleJavaApplication.class);
        }

        @Override
        public ToStringHelper string() {
            return super.string()
                    .add("infrastructure", infrastructure)
                    .add("application", application);
        }
    }
}
