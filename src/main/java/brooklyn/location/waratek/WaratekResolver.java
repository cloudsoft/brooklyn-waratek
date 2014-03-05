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

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.location.Location;
import brooklyn.location.LocationRegistry;
import brooklyn.location.LocationResolver;
import brooklyn.location.basic.BasicLocationRegistry;
import brooklyn.location.basic.LocationInternal;
import brooklyn.location.basic.LocationPropertiesFromBrooklynProperties;
import brooklyn.management.ManagementContext;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.text.KeyValueParser;
import brooklyn.util.text.Strings;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * Examples of valid specs:
 *   <ul>
 *     <li>waratek:infrastructureId
 *     <li>waratek:infrastructureId:(name="waratek-infrastructure")
 *     <li>waratek:infrastructureId:jvmId
 *     <li>waratek:infrastructureId:jvmid:jvcId
 *     <li>waratek:infrastructureId:*:jvcid
 *   </ul>
 */
public class WaratekResolver implements LocationResolver {

    private static final Logger LOG = LoggerFactory.getLogger(WaratekResolver.class);

    public static final String WARATEK = "waratek";
    public static final Pattern PATTERN = Pattern.compile("("+WARATEK+"|"+WARATEK.toUpperCase()+")" + ":([a-zA-Z0-9]+)" +
            "(:([a-zA-Z0-9]+)(:([a-zA-Z0-9]+))?)?" + "(:\\((.*)\\))?$");
    public static final Set<String> ACCEPTABLE_ARGS = ImmutableSet.of("name");

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
        LOG.info("Resolving location '" + spec + "' with flags " + Joiner.on(",").withKeyValueSeparator("=").join(locationFlags));
        String namedLocation = (String) locationFlags.get(LocationInternal.NAMED_SPEC_NAME.getName());

        Matcher matcher = PATTERN.matcher(spec);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid location '"+spec+"'; must specify something like waratek:entityId or waratek:entityId:(name=abc)");
        }

        String argsPart = matcher.group(8);
        Map<String, String> argsMap = (argsPart != null) ? KeyValueParser.parseMap(argsPart) : Collections.<String,String>emptyMap();
        String namePart = argsMap.get("name");

        if (!ACCEPTABLE_ARGS.containsAll(argsMap.keySet())) {
            Set<String> illegalArgs = Sets.difference(argsMap.keySet(), ACCEPTABLE_ARGS);
            throw new IllegalArgumentException("Invalid location '"+spec+"'; illegal args "+illegalArgs+"; acceptable args are "+ACCEPTABLE_ARGS);
        }
        if (argsMap.containsKey("name") && (namePart == null || namePart.isEmpty())) {
            throw new IllegalArgumentException("Invalid location '"+spec+"'; if name supplied then value must be non-empty");
        }

        Map<String, Object> filteredProperties = new LocationPropertiesFromBrooklynProperties().getLocationProperties(WARATEK, namedLocation, properties);
        MutableMap<String, Object> flags = MutableMap.<String, Object>builder().putAll(filteredProperties).putAll(locationFlags).build();
        // LocationConfigUtils.finalAndOriginalSpecs(spec, locationFlags, properties, namedLocation);
        // If there are a lot of flags here, something is pronbably wrong...

        String infrastructureId = matcher.group(2);
        if (Strings.isBlank(infrastructureId)) {
            throw new IllegalArgumentException("Invalid location '"+spec+"'; infrastructure entity id must be non-empty");
        }
        String jvmId = matcher.group(4);
        String jvcId = matcher.group(6);

        // Build the location name from parts
        StringBuilder name = new StringBuilder();
        if (namePart != null) {
            name.append(namePart);
        } else {
            name.append("waratek-");
            name.append(infrastructureId);
            if (jvmId != null) {
                name.append("-").append(jvmId);
                if (jvcId != null) {
                    name.append("-").append(jvcId);
                }
            }
        }
        final String locationName =  name.toString();
        flags.put("name", locationName);
        LOG.info("Location name will be: '" + locationName + "'");

        // Look up location by ID and return
        Location check = managementContext.getLocationManager().getLocation(locationName);
        LOG.info("Check location: {}", check);
        Optional<Location> found = Iterables.tryFind(managementContext.getLocationManager().getLocations(),
                new Predicate<Location>() {
                    @Override
                    public boolean apply(Location input) {
                        return input.getId().equals(locationName);
                    }
                });
        if (found.isPresent()) {
            Location location = found.get();
            LOG.info("Obtained location: " + location);
            return location;
        } else {
            throw new IllegalArgumentException("Invalid location '"+spec+"'; cannot find location '" + locationName + "'");
        }
    }

    @Override
    public boolean accepts(String spec, LocationRegistry registry) {
        return BasicLocationRegistry.isResolverPrefixForSpec(this, spec, true);
    }

}
