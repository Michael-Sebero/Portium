package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;

/** Does nothing for a while: idling between other tasks of a shuffled group. */
public class DummyTask extends Task<EntityLivingBase> {

    public DummyTask(int durationMin, int durationMax) {
        super(ImmutableMap.of(), durationMin, durationMax);
    }

    @Override
    protected boolean shouldContinueExecuting(WorldServer world, EntityLivingBase entity, long gameTime) {
        return true;
    }
}
