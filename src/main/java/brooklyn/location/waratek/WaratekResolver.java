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
package brooklyn.location.waratek;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.waratek.cloudvm.JavaVirtualMachine;
import brooklyn.entity.waratek.cloudvm.WaratekInfrastructure;
import brooklyn.location.Location;
import brooklyn.location.LocationRegistry;
import brooklyn.location.LocationResolver.EnableableLocationResolver;
import brooklyn.location.LocationSpec;
import brooklyn.location.basic.BasicLocationRegistry;
import brooklyn.location.basic.LocationInternal;
import brooklyn.location.basic.LocationPropertiesFromBrooklynProperties;
import brooklyn.location.dynamic.DynamicLocation;
import brooklyn.management.ManagementContext;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.text.KeyValueParser;
import brooklyn.util.text.Strings;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Examples of valid specs:
 *   <ul>
 *     <li>waratek:infrastructureId
 *     <li>waratek:infrastructureId:(name=waratek-infrastructure)
 *     <li>waratek:infrastructureId:jvmId
 *     <li>waratek:infrastructureId:jvmId:(name=jvm-brooklyn-1234,user=waratek)
 *   </ul>
 */
public class WaratekResolver implements EnableableLocationResolver {

    private static final Logger LOG = LoggerFactory.getLogger(WaratekResolver.class);

    public static final String WARATEK = "waratek";
    public static final Pattern PATTERN = Pattern.compile("("+WARATEK+"|"+WARATEK.toUpperCase()+")" + ":([a-zA-Z0-9]+)" +
            "(:([a-zA-Z0-9]+))?" + "(:\\((.*)\\))?$");
    public static final Set<String> ACCEPTABLE_ARGS = ImmutableSet.of("name", "displayName");

    public static final String WARATEK_INFRASTRUCTURE_SPEC = "waratek:%s";
    public static final String WARATEK_VIRTUAL_MACHINE_SPEC = "waratek:%s:%s";

    private ManagementContext managementContext;

    @Override
    public void init(ManagementContext managementContext) {
        this.managementContext = checkNotNull(managementContext, "managementContext");
    }

    @Override
    public String getPrefix() {
        return WARATEK;
    }

    @Override
    public Location newLocationFromString(Map locationFlags, String spec, LocationRegistry registry) {
        return newLocationFromString(spec, registry, registry.getProperties(), locationFlags);
    }

    protected Location newLocationFromString(String spec, brooklyn.location.LocationRegistry registry, Map properties, Map locationFlags) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Resolving location '" + spec + "' with flags " + Joiner.on(",").withKeyValueSeparator("=").join(locationFlags));
        }
        String namedLocation = (String) locationFlags.get(LocationInternal.NAMED_SPEC_NAME.getName());

        Matcher matcher = PATTERN.matcher(spec);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid location '"+spec+"'; must specify something like waratek:entityId or waratek:entityId:(name=abc)");
        }

        String argsPart = matcher.group(6);
        Map<String, String> argsMap = (argsPart != null) ? KeyValueParser.parseMap(argsPart) : Collections.<String,String>emptyMap();
        String displayNamePart = argsMap.get("displayName");
        String namePart = argsMap.get("name");

        if (!ACCEPTABLE_ARGS.containsAll(argsMap.keySet())) {
            Set<String> illegalArgs = Sets.difference(argsMap.keySet(), ACCEPTABLE_ARGS);
            throw new IllegalArgumentException("Invalid location '"+spec+"'; illegal args "+illegalArgs+"; acceptable args are "+ACCEPTABLE_ARGS);
        }
        if (argsMap.containsKey("displayName") && Strings.isEmpty(displayNamePart)) {
            throw new IllegalArgumentException("Invalid location '"+spec+"'; if displayName supplied then value must be non-empty");
        }
        if (argsMap.containsKey("name") && Strings.isEmpty(namePart)) {
            throw new IllegalArgumentException("Invalid location '"+spec+"'; if name supplied then value must be non-empty");
        }

        Map<String, Object> filteredProperties = new LocationPropertiesFromBrooklynProperties().getLocationProperties(WARATEK, namedLocation, properties);
        MutableMap<String, Object> flags = MutableMap.<String, Object>builder().putAll(filteredProperties).putAll(locationFlags).build();

        String infrastructureId = matcher.group(2);
        if (Strings.isBlank(infrastructureId)) {
            throw new IllegalArgumentException("Invalid location '"+spec+"'; infrastructure entity id must be non-empty");
        }
        String jvmId = matcher.group(4);

        // Build the display name
        StringBuilder name = new StringBuilder();
        if (displayNamePart != null) {
            name.append(displayNamePart);
        } else {
            name.append("Waratek ");
            if (jvmId == null) {
                name.append("Infrastructure ").append(infrastructureId);
            } else {
                name.append("JVM ").append(jvmId);
            }
        }
        final String displayName =  name.toString();

        // Build the location name
        name = new StringBuilder();
        if (namePart != null) {
            name.append(namePart);
        } else {
            name.append("waratek-");
            name.append(infrastructureId);
            if (jvmId != null) {
                name.append("-").append(jvmId);
            }
        }
        final String locationName =  name.toString();
        WaratekInfrastructure infrastructure = (WaratekInfrastructure) managementContext.getEntityManager().getEntity(infrastructureId);

        if (jvmId == null) {
            LocationSpec<WaratekLocation> locationSpec = LocationSpec.create(WaratekLocation.class)
                    .configure(flags)
                    .configure(DynamicLocation.OWNER, infrastructure)
                    .configure(LocationInternal.NAMED_SPEC_NAME, locationName)
                    .displayName(displayName);
            return managementContext.getLocationManager().createLocation(locationSpec);
        } else {
            JavaVirtualMachine jvm = (JavaVirtualMachine) managementContext.getEntityManager().getEntity(jvmId);

            LocationSpec<WaratekMachineLocation> locationSpec = LocationSpec.create(WaratekMachineLocation.class)
                    .parent(infrastructure.getDynamicLocation())
                    .configure(flags)
                    .configure(DynamicLocation.OWNER, jvm)
                    .configure(LocationInternal.NAMED_SPEC_NAME, locationName)
                    .displayName(displayName);
            return managementContext.getLocationManager().createLocation(locationSpec);
        }
    }

    @Override
    public boolean accepts(String spec, LocationRegistry registry) {
        return BasicLocationRegistry.isResolverPrefixForSpec(this, spec, true);
    }

    @Override
    public boolean isEnabled() {
        return true;
//        return Iterables.tryFind(managementContext.getEntityManager().getEntities(), Predicates.instanceOf(WaratekInfrastructure.class)).isPresent();
    }

}
