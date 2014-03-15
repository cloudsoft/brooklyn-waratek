package brooklyn.location.affinity;

import java.util.Map;

import javax.annotation.Nullable;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.EntityPredicates;
import brooklyn.location.Location;
import brooklyn.util.flags.SetFromFlag;

public class EntityIdAffinityStrategy extends AbstractAffinityStrategy {

    public static final ConfigKey<String> ENTITY_ID = ConfigKeys.newStringConfigKey(
            "entityId", "The id of the entity to have affinity with");

    @SetFromFlag("entityId")
    private String entityId;

    public EntityIdAffinityStrategy(Map<String, ?> properties) {
        super(properties);
    }

    @Override
    public int compare(Location o1, Location o2) {
        return 0;
    }

    @Override
    public boolean apply(@Nullable Location input) {
        Entity entity = getManagementContext().getEntityManager().getEntity(entityId);
        return EntityPredicates.withLocation(input).apply(entity);
    }

}
