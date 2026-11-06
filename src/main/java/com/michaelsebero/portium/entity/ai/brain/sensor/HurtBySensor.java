package com.michaelsebero.portium.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldServer;

import java.util.Set;

/** The last damage taken (1.12 forgets it after 40 ticks, as 1.16 does) and who dealt it. */
public class HurtBySensor extends Sensor<EntityLivingBase> {

    @Override
    public Set<MemoryModuleType<?>> getUsedMemories() {
        return ImmutableSet.of(MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY);
    }

    @Override
    protected void update(WorldServer world, EntityLivingBase entity) {
        Brain<?> brain = BrainUtil.brain(entity);
        DamageSource source = entity.getLastDamageSource();
        if (source != null) {
            brain.setMemory(MemoryModuleType.HURT_BY, source);
            Entity attacker = source.getTrueSource();
            if (attacker instanceof EntityLivingBase) brain.setMemory(MemoryModuleType.HURT_BY_ENTITY, (EntityLivingBase) attacker);
        } else {
            brain.removeMemory(MemoryModuleType.HURT_BY);
        }
        brain.getMemory(MemoryModuleType.HURT_BY_ENTITY).ifPresent(attacker -> {
            if (!attacker.isEntityAlive() || attacker.world != world) brain.removeMemory(MemoryModuleType.HURT_BY_ENTITY);
        });
    }
}
