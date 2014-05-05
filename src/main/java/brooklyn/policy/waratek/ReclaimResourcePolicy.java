package brooklyn.policy.waratek;

import static brooklyn.util.GroovyJavaMethods.truth;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.catalog.Catalog;
import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.event.AttributeSensor;
import brooklyn.event.SensorEvent;
import brooklyn.event.SensorEventListener;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.policy.PolicySpec;
import brooklyn.policy.basic.AbstractPolicy;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.flags.SetFromFlag;

import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Policy to reclaim idle resources.
 */
@Catalog
public class ReclaimResourcePolicy extends AbstractPolicy {

    private static final Logger LOG = LoggerFactory.getLogger(ReclaimResourcePolicy.class);

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private AttributeSensor<? extends Number> metric;
        private Entity entityWithMetric;
        private Number metricUpperBound;
        private Number metricLowerBound;

        public Builder id(String val) {
            this.id = val; return this;
        }
        public Builder name(String val) {
            this.name = val; return this;
        }
        public Builder metric(AttributeSensor<? extends Number> val) {
            this.metric = val; return this;
        }
        public Builder entityWithMetric(Entity val) {
            this.entityWithMetric = val; return this;
        }
        public Builder metricLowerBound(Number val) {
            this.metricLowerBound = val; return this;
        }
        public Builder metricUpperBound(Number val) {
            this.metricUpperBound = val; return this;
        }
        public Builder metricRange(Number min, Number max) {
            metricLowerBound = checkNotNull(min);
            metricUpperBound = checkNotNull(max);
            return this;
        }
        public ReclaimResourcePolicy build() {
            return new ReclaimResourcePolicy(toFlags());
        }
        public PolicySpec<ReclaimResourcePolicy> buildSpec() {
            return PolicySpec.create(ReclaimResourcePolicy.class)
                    .configure(toFlags());
        }
        private Map<String,?> toFlags() {
            return MutableMap.<String,Object>builder()
                    .putIfNotNull("id", id)
                    .putIfNotNull("name", name)
                    .putIfNotNull("metric", metric)
                    .putIfNotNull("entityWithMetric", entityWithMetric)
                    .putIfNotNull("metricUpperBound", metricUpperBound)
                    .putIfNotNull("metricLowerBound", metricLowerBound)
                    .build();
        }
    }

    public static final String POOL_CURRENT_SIZE_KEY = "pool.current.size";
    public static final String POOL_HIGH_THRESHOLD_KEY = "pool.high.threshold";
    public static final String POOL_LOW_THRESHOLD_KEY = "pool.low.threshold";
    public static final String POOL_CURRENT_WORKRATE_KEY = "pool.current.workrate";

    @SetFromFlag("metric")
    public static final ConfigKey<AttributeSensor<? extends Number>> METRIC = BasicConfigKey.builder(new TypeToken<AttributeSensor<? extends Number>>() {})
            .name("reclaimer.metric")
            .build();

    @SetFromFlag("entityWithMetric")
    public static final ConfigKey<Entity> ENTITY_WITH_METRIC = BasicConfigKey.builder(Entity.class)
            .name("reclaimer.entityWithMetric")
            .build();

    @SetFromFlag("metricLowerBound")
    public static final ConfigKey<Number> METRIC_LOWER_BOUND = BasicConfigKey.builder(Number.class)
            .name("reclaimer.metricLowerBound")
            .reconfigurable(true)
            .build();

    @SetFromFlag("metricUpperBound")
    public static final ConfigKey<Number> METRIC_UPPER_BOUND = BasicConfigKey.builder(Number.class)
            .name("reclaimer.metricUpperBound")
            .reconfigurable(true)
            .build();

    private Entity poolEntity;

    private volatile ScheduledExecutorService executor;

    private final SensorEventListener<Number> metricEventHandler = new SensorEventListener<Number>() {
        public void onEvent(SensorEvent<Number> event) {
            assert event.getSensor().equals(getMetric());
            onMetricChanged(event.getValue());
        }
    };

    public ReclaimResourcePolicy() {
        this(MutableMap.<String,Object>of());
    }

    public ReclaimResourcePolicy(Map<String,?> props) {
        super(props);
    }

    @Override
    public void init() {
        // TODO Should re-use the execution manager's thread pool, somehow
        executor = Executors.newSingleThreadScheduledExecutor(newThreadFactory());
    }

    public void setMetricLowerBound(Number val) {
        if (LOG.isInfoEnabled()) LOG.info("{} changing metricLowerBound from {} to {}", new Object[] {this, getMetricLowerBound(), val});
        setConfig(METRIC_LOWER_BOUND, checkNotNull(val));
    }

    public void setMetricUpperBound(Number val) {
        if (LOG.isInfoEnabled()) LOG.info("{} changing metricUpperBound from {} to {}", new Object[] {this, getMetricUpperBound(), val});
        setConfig(METRIC_UPPER_BOUND, checkNotNull(val));
    }

    private AttributeSensor<? extends Number> getMetric() {
        return getConfig(METRIC);
    }

    private Entity getEntityWithMetric() {
        return getConfig(ENTITY_WITH_METRIC);
    }

    private Number getMetricLowerBound() {
        return getConfig(METRIC_LOWER_BOUND);
    }

    private Number getMetricUpperBound() {
        return getConfig(METRIC_UPPER_BOUND);
    }

    @Override
    protected <T> void doReconfigureConfig(ConfigKey<T> key, T val) {
        if (key.equals(METRIC_LOWER_BOUND)) {
            // TODO If recorded what last metric value was then we could recalculate immediately
            // Rely on next metric-change to trigger recalculation;
            // and same for those below...
        } else if (key.equals(METRIC_UPPER_BOUND)) {

        } else {
            throw new UnsupportedOperationException("reconfiguring "+key+" unsupported for "+this);
        }
    }

    @Override
    public void suspend() {
        super.suspend();
        // TODO unsubscribe from everything? And resubscribe on resume?
        if (executor != null) executor.shutdownNow();
    }

    @Override
    public void resume() {
        super.resume();
        executor = Executors.newSingleThreadScheduledExecutor(newThreadFactory());
    }

    @Override
    public void setEntity(EntityLocal entity) {
        super.setEntity(entity);
        this.poolEntity = entity;

        if (getMetric() != null) {
            Entity entityToSubscribeTo = (getEntityWithMetric() != null) ? getEntityWithMetric() : entity;
            subscribe(entityToSubscribeTo, getMetric(), metricEventHandler);
        }
    }

    private ThreadFactory newThreadFactory() {
        return new ThreadFactoryBuilder()
                .setNameFormat("brooklyn-reclaimerpolicy-%d")
                .build();
    }

    private void onMetricChanged(Number val) {
        if (LOG.isTraceEnabled()) LOG.trace("{} recording pool-metric for {}: {}", new Object[] {this, poolEntity, val});

        if (val==null) {
            // occurs e.g. if using an aggregating enricher who returns null when empty, the sensor has gone away
            if (LOG.isTraceEnabled()) LOG.trace("{} not resizing pool {}, inbound metric is null", new Object[] {this, poolEntity});
            return;
        }

        double currentMetricD = val.doubleValue();
        double metricUpperBoundD = getMetricUpperBound().doubleValue();
        double metricLowerBoundD = getMetricLowerBound().doubleValue();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + (truth(name) ? "("+name+")" : "");
    }
}
