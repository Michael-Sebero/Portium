package com.michaelsebero.portium.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.world.WorldServer;

import java.util.Set;

/** The nearest seen adult of the same kind. */
public class MateSensor extends Sensor<EntityAgeable> {

    @Override
    public Set<MemoryModuleType<?>> getUsedMemories() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryModuleType.VISIBLE_MOBS);
    }

    @Override
    protected void update(WorldServer world, EntityAgeable entity) {
        BrainUtil.brain(entity).getMemory(MemoryModuleType.VISIBLE_MOBS).ifPresent(mobs ->
                BrainUtil.brain(entity).setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT, mobs.stream()
                        .filter(other -> other.getClass() == entity.getClass()).map(other -> (EntityAgeable) other)
                        .filter(other -> !other.isChild()).findFirst()));
    }
}
