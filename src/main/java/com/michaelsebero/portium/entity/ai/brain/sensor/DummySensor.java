package com.michaelsebero.portium.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;

import java.util.Set;

public class DummySensor extends Sensor<EntityLivingBase> {

    @Override
    protected void update(WorldServer world, EntityLivingBase entity) {
    }

    @Override
    public Set<MemoryModuleType<?>> getUsedMemories() {
        return ImmutableSet.of();
    }
}
