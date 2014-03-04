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

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.basic.BrooklynConfigKeys;
import brooklyn.location.Location;
import brooklyn.location.LocationRegistry;
import brooklyn.location.LocationResolver;
import brooklyn.location.LocationSpec;
import brooklyn.location.basic.BasicLocationRegistry;
import brooklyn.management.ManagementContext;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.text.KeyValueParser;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Examples of valid specs:
 *   <ul>
 *     <li>waratek
 *     <li>waratek:()
 *     <li>waratek:(name=abc)
 *     <li>waratek:(name="abc")
 *   </ul>
 * 
 * @author alex, aled
 */
public class WaratekResolver implements LocationResolver {

    private static final Logger log = LoggerFactory.getLogger(WaratekResolver.class);
    
    public static final String waratek = "waratek";
    
    private static final Pattern PATTERN = Pattern.compile("("+waratek+"|"+waratek.toUpperCase()+")" + "(:\\((.*)\\))?$");
    private static final Set<String> ACCEPTABLE_ARGS = ImmutableSet.of("name");
    
    private ManagementContext managementContext;

    @Override
    public void init(ManagementContext managementContext) {
        this.managementContext = checkNotNull(managementContext, "managementContext");
    }
    
    @Override
    public String getPrefix() {
        return waratek;
    }

    @Override
    public Location newLocationFromString(Map locationFlags, String spec, brooklyn.location.LocationRegistry registry) {
        return newLocationFromString(spec, registry, registry.getProperties(), locationFlags);
    }
    
    protected Location newLocationFromString(String spec, brooklyn.location.LocationRegistry registry, Map properties, Map locationFlags) {
        String namedLocation = (String) locationFlags.get("named");
        
        Matcher matcher = PATTERN.matcher(spec);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid location '"+spec+"'; must specify something like waratek or waratek(name=abc)");
        }
        
        String argsPart = matcher.group(3);
        Map<String, String> argsMap = (argsPart != null) ? KeyValueParser.parseMap(argsPart) : Collections.<String,String>emptyMap();
        String namePart = argsMap.get("name");
        
        if (!ACCEPTABLE_ARGS.containsAll(argsMap.keySet())) {
            Set<String> illegalArgs = Sets.difference(argsMap.keySet(), ACCEPTABLE_ARGS);
            throw new IllegalArgumentException("Invalid location '"+spec+"'; illegal args "+illegalArgs+"; acceptable args are "+ACCEPTABLE_ARGS);
        }
        if (argsMap.containsKey("name") && (namePart == null || namePart.isEmpty())) {
            throw new IllegalArgumentException("Invalid location '"+spec+"'; if name supplied then value must be non-empty");
        }

        Map<String, Object> filteredProperties = MutableMap.<String, Object>of(); // new waratekPropertiesFromBrooklynProperties().getLocationProperties("waratek", namedLocation, properties);
        MutableMap<String, Object> flags = MutableMap.<String, Object>builder().putAll(filteredProperties).putAll(locationFlags).build();
        
        if (namePart != null) {
            flags.put("name", namePart);
        } else {
            flags.put("name", "waratek");
        }
        if (registry != null) {
            String brooklynDataDir = (String) registry.getProperties().get(BrooklynConfigKeys.BROOKLYN_DATA_DIR.getName());
            if (brooklynDataDir != null && brooklynDataDir.length() > 0) {
                flags.put("localTempDir", new File(brooklynDataDir));
            }
        }
        
        return managementContext.getLocationManager().createLocation(LocationSpec.create(WaratekLocation.class)
                .configure(flags));
    }

    @Override
    public boolean accepts(String spec, LocationRegistry registry) {
        return BasicLocationRegistry.isResolverPrefixForSpec(this, spec, true);
    }

}
