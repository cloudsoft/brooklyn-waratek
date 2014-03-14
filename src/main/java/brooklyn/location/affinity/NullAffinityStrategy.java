package brooklyn.location.affinity;

import java.util.Map;

import javax.annotation.Nullable;

import brooklyn.location.Location;

public class NullAffinityStrategy extends AbstractAffinityStrategy {

    public NullAffinityStrategy(Map<String, ?> properties) {
        super(properties);
    }

    @Override
    public int compare(Location o1, Location o2) {
        return 0;
    }

    @Override
    public boolean apply(@Nullable Location input) {
        return true;
    }

}
