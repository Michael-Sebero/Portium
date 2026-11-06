package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.Random;

/** Mills about the spot where a kill fell. */
public class HuntCelebrationTask<E extends EntityLiving> extends Task<E> {

    private final int closeEnough;
    private final float speed;

    public HuntCelebrationTask(int closeEnough, float speed) {
        super(ImmutableMap.of(MemoryModuleType.CELEBRATE_LOCATION, MemoryModuleStatus.VALUE_PRESENT, MemoryModuleType.ATTACK_TARGET, MemoryModuleStatus.VALUE_ABSENT,
                MemoryModuleType.WALK_TARGET, MemoryModuleStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryModuleStatus.REGISTERED));
        this.closeEnough = closeEnough;
        this.speed = speed;
    }

    @Override
    protected void startExecuting(WorldServer world, E entity, long gameTime) {
        BlockPos location = BrainUtil.brain(entity).getMemory(MemoryModuleType.CELEBRATE_LOCATION).get();
        boolean near = location.distanceSqToCenter(entity.posX, entity.posY, entity.posZ) < (double) (this.closeEnough * this.closeEnough);
        if (!near) {
            Random random = entity.world.rand;
            BrainUtil.setTargetPosition(entity, location.add(random.nextInt(3) - 1, 0, random.nextInt(3) - 1), this.speed, this.closeEnough);
        }
    }
}
