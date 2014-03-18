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

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.EntityPredicates;
import brooklyn.location.Location;
import brooklyn.util.flags.SetFromFlag;

import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;

public class EntityTypeAffinityStrategy extends AbstractAffinityStrategy {

    public static final ConfigKey<Class<? extends Entity>> ENTITY_TYPE = ConfigKeys.newConfigKey(
            new TypeToken<Class<? extends Entity>>() { }, "entityType", "The class of entity to have affinity with");

    @SetFromFlag("entityType")
    private Class<? extends Entity> entityType;

    public EntityTypeAffinityStrategy(Map<String, ?> properties) {
        super(properties);
    }

    @Override
    public int compare(Location o1, Location o2) {
        return 0;
    }

    @Override
    public boolean apply(@Nullable Location input) {
        Collection<Entity> all = getManagementContext().getEntityManager().getEntities();
        Iterable<? extends Entity> typed = Iterables.filter(all, entityType);
        for (Entity entity : typed) {
            if (EntityPredicates.withLocation(input).apply(entity)) return true;
        }
        return false;
    }
}
