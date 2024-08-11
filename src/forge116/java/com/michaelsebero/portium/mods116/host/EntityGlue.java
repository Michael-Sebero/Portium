package com.michaelsebero.portium.mods116.host;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

import java.util.function.BiFunction;

/** 1.16's entity type building, onto Portium's entity types. The translator points the calls here. */
public final class EntityGlue {

    /** {@code EntityType.Builder.create(factory, classification)}: 1.16's factory takes the type it makes for. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static EntityType.Builder create(EntityType.IFactory factory, EntityClassification classification) {
        BiFunction<Object, Object, Object> made = (type, world) -> factory.create((EntityType) type, (World) world);
        return (EntityType.Builder) com.michaelsebero.portium.mods116.shim.EntityShims116.builder(made, classification);
    }

    /** {@code entity.getPose()}: 1.16's pose for 1.12's state (1.12's entities have no pose data). */
    public static net.minecraft.entity.Pose pose(Object entity) {
        return net.minecraft.entity.Pose.valueOf(com.michaelsebero.portium.mods116.shim.EntityShims116.poseName(entity));
    }

    private EntityGlue() {}
}
