package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

/** Turns the head towards the look target while it stays in sight. */
public class LookTask extends Task<EntityLiving> {

    public LookTask(int durationMin, int durationMax) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryModuleStatus.VALUE_PRESENT), durationMin, durationMax);
    }

    @Override
    protected boolean shouldContinueExecuting(WorldServer world, EntityLiving entity, long gameTime) {
        return BrainUtil.brain(entity).getMemory(MemoryModuleType.LOOK_TARGET).filter(target -> target.isVisibleTo(entity)).isPresent();
    }

    @Override
    protected void resetTask(WorldServer world, EntityLiving entity, long gameTime) {
        BrainUtil.brain(entity).removeMemory(MemoryModuleType.LOOK_TARGET);
    }

    @Override
    protected void updateTask(WorldServer world, EntityLiving entity, long gameTime) {
        BrainUtil.brain(entity).getMemory(MemoryModuleType.LOOK_TARGET).ifPresent(target -> {
            Vec3d pos = target.getPos();
            entity.getLookHelper().setLookPosition(pos.x, pos.y, pos.z, entity.getHorizontalFaceSpeed(), entity.getVerticalFaceSpeed());
        });
    }
}
