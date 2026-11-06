package com.michaelsebero.portium.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Living entities within 16 blocks, nearest first, and those of them this entity can see. */
public class NearestLivingEntitiesSensor extends Sensor<EntityLivingBase> {

    @Override
    protected void update(WorldServer world, EntityLivingBase entity) {
        List<EntityLivingBase> mobs = world.getEntitiesWithinAABB(EntityLivingBase.class, entity.getEntityBoundingBox().grow(16.0D),
                other -> other != entity && other.isEntityAlive());
        mobs.sort(Comparator.comparingDouble(entity::getDistanceSq));
        Brain<?> brain = BrainUtil.brain(entity);
        brain.setMemory(MemoryModuleType.MOBS, mobs);
        brain.setMemory(MemoryModuleType.VISIBLE_MOBS, mobs.stream().filter(other -> canAttackTarget(entity, other)).collect(Collectors.toList()));
    }

    @Override
    public Set<MemoryModuleType<?>> getUsedMemories() {
        return ImmutableSet.of(MemoryModuleType.MOBS, MemoryModuleType.VISIBLE_MOBS);
    }
}
