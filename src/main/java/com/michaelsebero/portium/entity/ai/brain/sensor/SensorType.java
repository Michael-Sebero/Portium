package com.michaelsebero.portium.entity.ai.brain.sensor;

import java.util.function.Supplier;

/** A kind of sensor; each brain gets its own instance. */
public final class SensorType<U extends Sensor<?>> {

    public static final SensorType<DummySensor> DUMMY = new SensorType<>("dummy", DummySensor::new);
    public static final SensorType<WantedItemsSensor> NEAREST_ITEMS = new SensorType<>("nearest_items", WantedItemsSensor::new);
    public static final SensorType<NearestLivingEntitiesSensor> NEAREST_LIVING_ENTITIES = new SensorType<>("nearest_living_entities", NearestLivingEntitiesSensor::new);
    public static final SensorType<NearestPlayersSensor> NEAREST_PLAYERS = new SensorType<>("nearest_players", NearestPlayersSensor::new);
    public static final SensorType<HurtBySensor> HURT_BY = new SensorType<>("hurt_by", HurtBySensor::new);
    public static final SensorType<MateSensor> NEAREST_ADULT = new SensorType<>("nearest_adult", MateSensor::new);

    private final String name;
    private final Supplier<U> supplier;

    public SensorType(String name, Supplier<U> supplier) {
        this.name = name;
        this.supplier = supplier;
    }

    public U create() {
        return this.supplier.get();
    }

    @Override
    public String toString() {
        return "minecraft:" + this.name;
    }
}
