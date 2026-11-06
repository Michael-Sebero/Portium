package com.michaelsebero.portium.entity.raid;

import com.michaelsebero.portium.Tags;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * What 1.14's spawn reasons decide for raiders. 1.12 has no spawn reasons, so the spawns that matter say what they
 * are while they run: patrols, raids ("event") and structures; the world's own spawning marks its mobs through Forge.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class RaiderSpawns {

    public enum Reason { OTHER, NATURAL, PATROL, EVENT, STRUCTURE }

    private static final ThreadLocal<Reason> REASON = ThreadLocal.withInitial(() -> Reason.OTHER);

    /** Runs a spawn (its {@code onInitialSpawn}) under a spawn reason. */
    public static void spawning(Reason reason, Runnable spawn) {
        Reason previous = REASON.get();
        REASON.set(reason);
        try {
            spawn.run();
        } finally {
            REASON.set(previous);
        }
    }

    /** The reason the current spawn runs under ({@link Reason#OTHER} outside {@link #spawning}). */
    public static Reason current() {
        return REASON.get();
    }

    /** 1.14's {@code AbstractRaiderEntity} and {@code PatrollerEntity.onInitialSpawn}, ahead of 1.12's mob setup. */
    public static void onInitialSpawn(EntityLiving entity) {
        IRaider raider = (IRaider) entity;
        RaiderState state = raider.portium$raider();
        Reason reason = state.isNaturalSpawn() ? Reason.NATURAL : REASON.get();
        state.setCanJoinRaid(!(entity instanceof EntityWitch) || reason != Reason.NATURAL);
        if (reason != Reason.PATROL && reason != Reason.EVENT && reason != Reason.STRUCTURE && entity.getRNG().nextFloat() < 0.06F
                && raider.portium$canBeLeader()) {
            state.setLeaderOnly(true);
        }
        if (state.isLeader()) {
            entity.setItemStackToSlot(EntityEquipmentSlot.HEAD, OminousBanner.create());
            entity.setDropChance(EntityEquipmentSlot.HEAD, 2.0F);
        }
        if (reason == Reason.PATROL) state.setPatrolling(true);
    }

    /** The world's own spawning (not spawners) is 1.14's NATURAL reason. */
    @SubscribeEvent
    public static void onSpecialSpawn(LivingSpawnEvent.SpecialSpawn event) {
        if (event.getSpawner() == null && event.getEntityLiving() instanceof IRaider) {
            ((IRaider) event.getEntityLiving()).portium$raider().setNaturalSpawn(true);
        }
    }

    private RaiderSpawns() {}
}
