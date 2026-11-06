package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.GlobalPos;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.memory.WalkTarget;
import net.minecraft.entity.EntityCreature;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

import java.util.Optional;

/** Strolls about while near a remembered position, every nine seconds at most. */
public class WorkTask extends Task<EntityCreature> {

    private final MemoryModuleType<GlobalPos> memory;
    private long nextRun;
    private final int maxDistance;
    private final float speed;

    public WorkTask(MemoryModuleType<GlobalPos> memory, float speed, int maxDistance) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryModuleStatus.REGISTERED, memory, MemoryModuleStatus.VALUE_PRESENT));
        this.memory = memory;
        this.speed = speed;
        this.maxDistance = maxDistance;
    }

    @Override
    protected boolean shouldExecute(WorldServer world, EntityCreature entity) {
        Optional<GlobalPos> pos = BrainUtil.brain(entity).getMemory(this.memory);
        return pos.isPresent() && world.provider.getDimension() == pos.get().getDimension()
                && pos.get().getPos().distanceSqToCenter(entity.posX, entity.posY, entity.posZ) < (double) (this.maxDistance * this.maxDistance);
    }

    @Override
    protected void startExecuting(WorldServer world, EntityCreature entity, long gameTime) {
        if (gameTime > this.nextRun) {
            Optional<Vec3d> pos = Optional.ofNullable(com.michaelsebero.portium.entity.ai.brain.LandPos116.find(entity, 8, 6));
            BrainUtil.brain(entity).setMemory(MemoryModuleType.WALK_TARGET, pos.map(target -> new WalkTarget(target, this.speed, 1)));
            this.nextRun = gameTime + 180L;
        }
    }
}
