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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.IOException;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import brooklyn.entity.Application;
import brooklyn.entity.BrooklynAppLiveTestSupport;
import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.StartableApplication;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.Startable;
import brooklyn.entity.waratek.cloudvm.WaratekInfrastructure;
import brooklyn.location.Location;
import brooklyn.location.LocationDefinition;
import brooklyn.location.basic.LocationInternal;
import brooklyn.location.dynamic.LocationOwner;
import brooklyn.test.EntityTestUtils;

import com.waratek.cloudvm.SimpleJavaApplication;

import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

/**
 * Waratek integration tests.
 */
public class WaratekInfrastructureIntegrationTest extends BrooklynAppLiveTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(WaratekInfrastructureIntegrationTest.class);

    protected Location testLocation;
    protected WaratekInfrastructure infrastructure;

    @BeforeMethod(alwaysRun = true)
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // TODO configure proper remote locations
        testLocation = app.newLocalhostProvisioningLocation();
    }

    /**
     * Test that a single JVM infrastructure starts and sets SERVICE_UP correctly.
     */
    @Test(groups = "Integration")
    public void canStartupAndShutdown() {
        infrastructure = app.createAndManageChild(EntitySpec.create(WaratekInfrastructure.class)
                .configure(WaratekInfrastructure.JVM_CLUSTER_MIN_SIZE, 1));
        app.start(ImmutableList.of(testLocation));

        // Wait until started
        EntityTestUtils.assertAttributeEqualsEventually(infrastructure, Startable.SERVICE_UP, true);
        Entities.dumpInfo(app);

        // Stop and wait until stopped
        infrastructure.stop();
        EntityTestUtils.assertAttributeEqualsEventually(infrastructure, Startable.SERVICE_UP, false);
    }

    /**
     * Test that a we can deploy a simple java application.
     */
    @Test(groups = "Integration")
    public void canDeploySimpleJavaApplication() {
        infrastructure = app.createAndManageChild(EntitySpec.create(WaratekInfrastructure.class)
                .configure(WaratekInfrastructure.JVM_CLUSTER_MIN_SIZE, 1)
                .configure(WaratekInfrastructure.LOCATION_NAME, "deployment-infrastructure"));
        app.start(ImmutableList.of(testLocation));

        // Wait until started
        EntityTestUtils.assertAttributeEqualsEventually(infrastructure, Startable.SERVICE_UP, true);

        // Check the dynamic location details
        Location location = infrastructure.getAttribute(LocationOwner.DYNAMIC_LOCATION);
        assertNotNull(location);
        assertEquals(location.getConfig(LocationInternal.NAMED_SPEC_NAME), "deployment-infrastructure");

        // Look up the location definition
        LocationDefinition definition = mgmt.getLocationRegistry().getDefinedLocationByName("deployment-infrastructure");
        assertNotNull(definition);

        // Deploy simple Java application
        EntitySpec<StartableApplication> spec = EntitySpec.create(StartableApplication.class, SimpleJavaApplication.class)
                .configure(SimpleJavaApplication.INITIAL_SIZE, 1)
                .configure(SimpleJavaApplication.CLASSPATH, ImmutableList.of("https://s3-eu-west-1.amazonaws.com/brooklyn-waratek/brooklyn-waratek-examples.jar"));
        StartableApplication simple = ApplicationBuilder.newManagedApp(spec, mgmt);
        simple.start(ImmutableList.of(location));

        // Wait until Java application started
        EntityTestUtils.assertAttributeEqualsEventually(simple, Startable.SERVICE_UP, true);

        // Dump info about applications
        dumpInfo(this, ImmutableList.of(app, simple));

        // Stop Java application and wait until stopped
        simple.stop();
        EntityTestUtils.assertAttributeEqualsEventually(simple, Startable.SERVICE_UP, false);
    }

    public static final void dumpInfo(Object context, Iterable<? extends Application> applications) {
        try {
            PrintWriter out = new PrintWriter("enitites-" + CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, context.getClass().getSimpleName()) + ".txt", Charsets.UTF_8.name());
            for (Application application : applications) {
                Entities.dumpInfo(application, out);
            }
        } catch (IOException ioe) {
            LOG.warn("Error writing application data to file", ioe);
        }
    }

}
