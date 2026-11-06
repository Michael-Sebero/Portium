package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.GlobalPos;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.memory.WalkTarget;
import net.minecraft.entity.EntityCreature;
import net.minecraft.world.WorldServer;

import java.util.Optional;

/** Heads back to a remembered position when farther than a distance from it, at most every four seconds. */
public class WalkTowardsPosTask extends Task<EntityCreature> {

    private final MemoryModuleType<GlobalPos> memory;
    private final int completionRange;
    private final int maxDistance;
    private final float speed;
    private long nextRun;

    public WalkTowardsPosTask(MemoryModuleType<GlobalPos> memory, float speed, int completionRange, int maxDistance) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryModuleStatus.REGISTERED, memory, MemoryModuleStatus.VALUE_PRESENT));
        this.memory = memory;
        this.speed = speed;
        this.completionRange = completionRange;
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
            Brain<?> brain = BrainUtil.brain(entity);
            brain.getMemory(this.memory).ifPresent(pos ->
                    brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos.getPos(), this.speed, this.completionRange)));
            this.nextRun = gameTime + 80L;
        }
    }
}
