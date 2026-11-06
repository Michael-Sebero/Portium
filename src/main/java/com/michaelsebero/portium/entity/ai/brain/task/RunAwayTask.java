package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.memory.WalkTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

import java.util.Optional;
import java.util.function.Function;

/** Runs away from a remembered entity or block when within a distance of it. */
public class RunAwayTask<T> extends Task<EntityCreature> {

    private final MemoryModuleType<T> memory;
    private final float speed;
    private final int maxDistance;
    private final Function<T, Vec3d> position;

    public RunAwayTask(MemoryModuleType<T> memory, float speed, int maxDistance, boolean overrideWalkTarget, Function<T, Vec3d> position) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, overrideWalkTarget ? MemoryModuleStatus.REGISTERED : MemoryModuleStatus.VALUE_ABSENT,
                memory, MemoryModuleStatus.VALUE_PRESENT));
        this.memory = memory;
        this.speed = speed;
        this.maxDistance = maxDistance;
        this.position = position;
    }

    public static RunAwayTask<BlockPos> fromBlock(MemoryModuleType<BlockPos> memory, float speed, int maxDistance, boolean overrideWalkTarget) {
        return new RunAwayTask<>(memory, speed, maxDistance, overrideWalkTarget, pos -> new Vec3d(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
    }

    public static <T extends Entity> RunAwayTask<T> fromEntity(MemoryModuleType<T> memory, float speed, int maxDistance, boolean overrideWalkTarget) {
        return new RunAwayTask<>(memory, speed, maxDistance, overrideWalkTarget, Entity::getPositionVector);
    }

    @Override
    protected boolean shouldExecute(WorldServer world, EntityCreature entity) {
        return !this.alreadyRunningAway(entity) && entity.getPositionVector().squareDistanceTo(this.threat(entity)) < (double) (this.maxDistance * this.maxDistance);
    }

    private Vec3d threat(EntityCreature entity) {
        return this.position.apply(BrainUtil.brain(entity).getMemory(this.memory).get());
    }

    private boolean alreadyRunningAway(EntityCreature entity) {
        Optional<WalkTarget> walk = BrainUtil.brain(entity).getMemory(MemoryModuleType.WALK_TARGET);
        if (!walk.isPresent() || walk.get().getSpeed() != this.speed) return false;
        Vec3d heading = walk.get().getTarget().getPos().subtract(entity.getPositionVector());
        Vec3d towardsThreat = this.threat(entity).subtract(entity.getPositionVector());
        return heading.dotProduct(towardsThreat) < 0.0D;
    }

    @Override
    protected void startExecuting(WorldServer world, EntityCreature entity, long gameTime) {
        Vec3d threat = this.threat(entity);
        for (int i = 0; i < 10; ++i) {
            Vec3d away = RandomPositionGenerator.findRandomTargetBlockAwayFrom(entity, 16, 7, threat);
            if (away != null) {
                BrainUtil.brain(entity).setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(away, this.speed, 0));
                return;
            }
        }
    }
}
