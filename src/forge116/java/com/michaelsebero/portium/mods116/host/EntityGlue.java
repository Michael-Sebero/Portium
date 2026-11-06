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

    /** {@code entity.areEyesInFluid(tag)}: water or lava (by the tag's name) at the entity's eyes. */
    public static boolean eyesInFluid(net.minecraft.entity.Entity entity, net.minecraft.tags.ITag<?> tag) {
        String name = tag instanceof net.minecraft.tags.ITag.INamedTag ? ((net.minecraft.tags.ITag.INamedTag<?>) tag).func_230234_a_().func_110623_a() : "water";
        return com.michaelsebero.portium.mods116.shim.EntityShims116.eyesIn(entity, name.contains("lava"));
    }

    private EntityGlue() {}

    /** 1.16's {@code AttributeModifier.getOperation()}: the enum for 1.12's number. */
    public static net.minecraft.entity.ai.attributes.AttributeModifier.Operation modifierOperation(net.minecraft.entity.ai.attributes.AttributeModifier modifier) {
        return net.minecraft.entity.ai.attributes.AttributeModifier.Operation.func_220372_a(
                com.michaelsebero.portium.mods116.shim.MiscShims116.operationOf(modifier));
    }

    /** 1.16's {@code Effect.getCategory()}: a 1.16 mod's effect's own, else from 1.12's bad and beneficial flags. */
    public static net.minecraft.potion.EffectType effectType(net.minecraft.potion.Effect effect) {
        Object potion = effect;
        if (potion instanceof com.michaelsebero.portium.mods116.adapter.Effect116) {
            return (net.minecraft.potion.EffectType) ((com.michaelsebero.portium.mods116.adapter.Effect116) potion).getEffectType();
        }
        if (com.michaelsebero.portium.mods116.shim.MiscShims116.isBadEffect(potion)) return net.minecraft.potion.EffectType.valueOf("HARMFUL");
        if (com.michaelsebero.portium.mods116.shim.MiscShims116.isBeneficialEffect(potion)) return net.minecraft.potion.EffectType.valueOf("BENEFICIAL");
        return net.minecraft.potion.EffectType.valueOf("NEUTRAL");
    }
}
