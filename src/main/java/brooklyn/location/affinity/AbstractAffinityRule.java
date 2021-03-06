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
package brooklyn.location.affinity;

import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import brooklyn.config.ConfigKey;
import brooklyn.internal.storage.Reference;
import brooklyn.internal.storage.impl.BasicReference;
import brooklyn.location.Location;
import brooklyn.management.ManagementContext;
import brooklyn.management.internal.ManagementContextInternal;
import brooklyn.util.config.ConfigBag;
import brooklyn.util.flags.FlagUtils;
import brooklyn.util.flags.SetFromFlag;
import brooklyn.util.flags.TypeCoercions;
import brooklyn.util.text.Identifiers;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;

public abstract class AbstractAffinityRule implements AffinityRule {

    private ConfigBag configBag = new ConfigBag();
    private volatile ManagementContext managementContext;
    private boolean inConstruction;

    protected Reference<String> name = new BasicReference<String>();

    protected final AtomicBoolean configured = new AtomicBoolean(false);

    @SetFromFlag("id")
    protected String id = Identifiers.makeRandomId(8);

    public AbstractAffinityRule(Map<String, ?> properties) {
        inConstruction = true;

        configure(properties);

        boolean deferConstructionChecks = (properties.containsKey("deferConstructionChecks") && TypeCoercions.coerce(properties.get("deferConstructionChecks"), Boolean.class));
        if (!deferConstructionChecks) {
            FlagUtils.checkRequiredFields(this);
        }

        inConstruction = false;

        init();
    }

    /**
     * Called after configuring.
     */
    public void init() {
        // no-op
    }

    @Override
    public <T> T setConfig(ConfigKey<T> key, T value) {
        return configBag.put(key, value);
    }

    // TODO ensure no callers rely on 'remove' semantics, and don't remove;
    // or perhaps better use a config bag so we know what is used v unused
    private static Object removeIfPossible(Map map, Object key) {
        try {
            return map.remove(key);
        } catch (Exception e) {
            return map.get(key);
        }
    }

    /**
     * Will set fields from flags. The unused configuration can be found via the
     * {@linkplain ConfigBag#getUnusedConfig()}. This can be overridden for custom initialization.
     */
    public void configure(Map<String, ?> properties) {
        configBag.putAll(properties);

        boolean firstTime = !configured.getAndSet(true);

        FlagUtils.setFieldsFromFlagsWithBag(this, properties, configBag, firstTime);
        FlagUtils.setAllConfigKeys(this, configBag, false);

        if (properties.containsKey("name")) {
            name.set((String) removeIfPossible(properties, "name"));
        } else {
            name.set(getClass().getSimpleName()+":"+id.substring(0, Math.min(id.length(),4)));
        }
    }

    public void setManagementContext(ManagementContextInternal managementContext) {
        this.managementContext = managementContext;

        Map<String, Object> oldConfig = configBag.getAllConfig();

        configBag = ConfigBag.newLiveInstance(managementContext.getStorage().<String,Object>getMap(id+"-config"));
        if (oldConfig.size() > 0) {
            configBag.putAll(oldConfig);
        }
    }

    public ManagementContext getManagementContext() {
        return managementContext;
    }

    public boolean isManaged() {
        return managementContext != null;
    }

    @Override
    public abstract int compare(Location o1, Location o2);

    @Override
    public abstract boolean apply(@Nullable Location input);

    @Override
    public SortedSet<Location> checkLocations(Iterable<Location> locs) {
        return ImmutableSortedSet.orderedBy(this).addAll(Iterables.filter(locs, this)).build();
    }

}
