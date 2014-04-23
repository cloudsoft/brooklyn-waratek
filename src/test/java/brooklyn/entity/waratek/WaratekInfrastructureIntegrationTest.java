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

import static org.testng.Assert.assertNotNull;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import brooklyn.entity.BrooklynAppLiveTestSupport;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.Startable;
import brooklyn.entity.waratek.cloudvm.WaratekInfrastructure;
import brooklyn.location.Location;
import brooklyn.test.EntityTestUtils;

import com.waratek.cloudvm.SimpleJavaApplication;

import com.google.common.collect.ImmutableList;

/**
 * Waratek integration tests.
 */
public class WaratekInfrastructureIntegrationTest extends BrooklynAppLiveTestSupport {

    protected Location testLocation, waratekLocation;
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
                .configure(WaratekInfrastructure.JVM_CLUSTER_MIN_SIZE, 1)
                .configure(WaratekInfrastructure.LOCATION_NAME, "test-infrastructure"));
        app.start(ImmutableList.of(testLocation));

        EntityTestUtils.assertAttributeEqualsEventually(infrastructure, Startable.SERVICE_UP, true);
        Entities.dumpInfo(app);

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
                .configure(WaratekInfrastructure.LOCATION_NAME, "test-infrastructure"));
        app.start(ImmutableList.of(testLocation));

        EntityTestUtils.assertAttributeEqualsEventually(infrastructure, Startable.SERVICE_UP, true);

        waratekLocation = mgmt.getLocationManager().getLocation("test-infrastructure");
        assertNotNull(waratekLocation);

        SimpleJavaApplication simpleJava = app.createAndManageChild(EntitySpec.create(SimpleJavaApplication.class)
                .configure(SimpleJavaApplication.INITIAL_SIZE, 1)
                .configure(SimpleJavaApplication.CLASSPATH, ImmutableList.of("https://s3-eu-west-1.amazonaws.com/brooklyn-waratek/brooklyn-waratek-examples.jar")));
        simpleJava.start(ImmutableList.of(waratekLocation));

        EntityTestUtils.assertAttributeEqualsEventually(simpleJava, Startable.SERVICE_UP, true);

        simpleJava.stop();
        infrastructure.stop();
    }
}
