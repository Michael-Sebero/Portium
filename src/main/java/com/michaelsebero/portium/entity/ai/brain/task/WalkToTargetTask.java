package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.memory.WalkTarget;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Follows the walk target: paths to it, re-paths when it moves over two blocks, and remembers since when it couldn't be
 * reached. A target out of reach gets a path towards it instead, as 1.16 does.
 */
public class WalkToTargetTask extends Task<EntityLiving> {

    private int cooldown;
    @Nullable
    private Path path;
    @Nullable
    private BlockPos targetPos;
    private float speed;

    public WalkToTargetTask() {
        this(150, 250);
    }

    public WalkToTargetTask(int durationMin, int durationMax) {
        super(ImmutableMap.of(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleStatus.REGISTERED, MemoryModuleType.PATH,
                MemoryModuleStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryModuleStatus.VALUE_PRESENT), durationMin, durationMax);
    }

    @Override
    protected boolean shouldExecute(WorldServer world, EntityLiving entity) {
        if (this.cooldown > 0) {
            --this.cooldown;
            return false;
        }
        Brain<?> brain = BrainUtil.brain(entity);
        WalkTarget target = brain.getMemory(MemoryModuleType.WALK_TARGET).get();
        boolean reached = this.hasReachedTarget(entity, target);
        if (!reached && this.findPath(entity, target, world.getTotalWorldTime())) {
            this.targetPos = target.getTarget().getBlockPos();
            return true;
        }
        brain.removeMemory(MemoryModuleType.WALK_TARGET);
        if (reached) brain.removeMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        return false;
    }

    @Override
    protected boolean shouldContinueExecuting(WorldServer world, EntityLiving entity, long gameTime) {
        if (this.path == null || this.targetPos == null) return false;
        Optional<WalkTarget> target = BrainUtil.brain(entity).getMemory(MemoryModuleType.WALK_TARGET);
        PathNavigate navigator = entity.getNavigator();
        return !navigator.noPath() && target.isPresent() && !this.hasReachedTarget(entity, target.get());
    }

    @Override
    protected void resetTask(WorldServer world, EntityLiving entity, long gameTime) {
        Brain<?> brain = BrainUtil.brain(entity);
        if (brain.hasMemory(MemoryModuleType.WALK_TARGET) && !this.hasReachedTarget(entity, brain.getMemory(MemoryModuleType.WALK_TARGET).get())
                && entity.getNavigator().noPath()) {
            this.cooldown = world.rand.nextInt(40);
        }
        entity.getNavigator().clearPath();
        brain.removeMemory(MemoryModuleType.WALK_TARGET);
        brain.removeMemory(MemoryModuleType.PATH);
        this.path = null;
    }

    @Override
    protected void startExecuting(WorldServer world, EntityLiving entity, long gameTime) {
        BrainUtil.brain(entity).setMemory(MemoryModuleType.PATH, this.path);
        entity.getNavigator().setPath(this.path, this.speed);
    }

    @Override
    protected void updateTask(WorldServer world, EntityLiving entity, long gameTime) {
        Path current = entity.getNavigator().getPath();
        Brain<?> brain = BrainUtil.brain(entity);
        if (this.path != current) {
            this.path = current;
            brain.setMemory(MemoryModuleType.PATH, current);
        }
        if (current != null && this.targetPos != null) {
            WalkTarget target = brain.getMemory(MemoryModuleType.WALK_TARGET).get();
            if (target.getTarget().getBlockPos().distanceSq(this.targetPos) > 4.0D && this.findPath(entity, target, world.getTotalWorldTime())) {
                this.targetPos = target.getTarget().getBlockPos();
                this.startExecuting(world, entity, gameTime);
            }
        }
    }

    private boolean findPath(EntityLiving entity, WalkTarget target, long gameTime) {
        BlockPos pos = target.getTarget().getBlockPos();
        this.path = entity.getNavigator().getPathToPos(pos);
        this.speed = target.getSpeed();
        Brain<?> brain = BrainUtil.brain(entity);
        if (this.hasReachedTarget(entity, target)) {
            brain.removeMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        } else {
            if (this.path != null && reachesTarget(this.path, pos)) {
                brain.removeMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            } else if (!brain.hasMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)) {
                brain.setMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, gameTime);
            }
            if (this.path != null) return true;
            if (entity instanceof EntityCreature) {
                Vec3d towards = RandomPositionGenerator.findRandomTargetBlockTowards((EntityCreature) entity, 10, 7,
                        new Vec3d(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
                if (towards != null) {
                    this.path = entity.getNavigator().getPathToXYZ(towards.x, towards.y, towards.z);
                    return this.path != null;
                }
            }
        }
        return false;
    }

    /** 1.16's {@code Path.reachesTarget}: the path ends at the target (a block of slack for 1.12's node heights). */
    public static boolean reachesTarget(Path path, BlockPos target) {
        PathPoint end = path.getFinalPathPoint();
        if (end == null) return false;
        return Math.abs(end.x - target.getX()) + Math.abs(end.z - target.getZ()) == 0 && Math.abs(end.y - target.getY()) <= 1;
    }

    private boolean hasReachedTarget(EntityLiving entity, WalkTarget target) {
        BlockPos pos = target.getTarget().getBlockPos();
        BlockPos at = new BlockPos(entity);
        return Math.abs(pos.getX() - at.getX()) + Math.abs(pos.getY() - at.getY()) + Math.abs(pos.getZ() - at.getZ()) <= target.getDistance();
    }
}
