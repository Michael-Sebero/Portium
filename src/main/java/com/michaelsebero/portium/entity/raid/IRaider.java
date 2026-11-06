package com.michaelsebero.portium.entity.raid;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.SoundEvent;

import javax.annotation.Nullable;

/**
 * 1.14's raiders ({@code AbstractRaiderEntity} and the {@code PatrollerEntity} under it): pillagers and ravagers, and
 * 1.12's own illagers and witches through mixins. Patrol and raid state lives in {@link RaiderState}.
 */
public interface IRaider {

    RaiderState portium$raider();

    /** Synced: raising arms after a raid the village lost. */
    boolean portium$isCelebrating();

    void portium$setCelebrating(boolean celebrating);

    /** 1.14's {@code MobEntity} aggressive flag (synced for illagers, whose arm pose shows it). */
    boolean portium$isAggressive();

    void portium$setAggressive(boolean aggressive);

    /** Played while celebrating; null for none. */
    @Nullable
    SoundEvent portium$getCelebrationSound();

    /** Whether it may lead a patrol (ravagers can't). */
    default boolean portium$canBeLeader() {
        return true;
    }

    /** Gear for a raid wave (pillagers get enchanted crossbows); called as it joins. */
    default void portium$applyWaveBonus(int wave, boolean leader) {}

    static RaiderState state(EntityLiving entity) {
        return ((IRaider) entity).portium$raider();
    }
}
