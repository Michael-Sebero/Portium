package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.GlobalPos;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Opens wooden doors on the path as it reaches them, and closes those it opened once through (unless another of its kind is using them). */
public class InteractWithDoorTask extends Task<EntityLivingBase> {

    @Nullable
    private PathPoint lastPoint;
    private int stuckTicks;

    public InteractWithDoorTask() {
        super(ImmutableMap.of(MemoryModuleType.PATH, MemoryModuleStatus.VALUE_PRESENT, MemoryModuleType.OPENED_DOORS, MemoryModuleStatus.REGISTERED));
    }

    @Override
    protected boolean shouldExecute(WorldServer world, EntityLivingBase entity) {
        Path path = BrainUtil.brain(entity).getMemory(MemoryModuleType.PATH).get();
        if (path.getCurrentPathLength() == 0 || path.isFinished()) return false;
        if (!Objects.equals(this.lastPoint, current(path))) {
            this.stuckTicks = 20;
            return true;
        }
        if (this.stuckTicks > 0) --this.stuckTicks;
        return this.stuckTicks == 0;
    }

    @Override
    protected void startExecuting(WorldServer world, EntityLivingBase entity, long gameTime) {
        Path path = BrainUtil.brain(entity).getMemory(MemoryModuleType.PATH).get();
        this.lastPoint = current(path);
        PathPoint previous = previous(path);
        PathPoint next = current(path);
        if (previous != null) {
            BlockPos pos = pos(previous);
            if (isWoodenDoor(world.getBlockState(pos))) {
                if (!BlockDoor.isOpen(world, pos)) ((BlockDoor) world.getBlockState(pos).getBlock()).toggleDoor(world, pos, true);
                this.rememberOpened(world, entity, pos);
            }
        }
        BlockPos nextPos = pos(next);
        if (isWoodenDoor(world.getBlockState(nextPos)) && !BlockDoor.isOpen(world, nextPos)) {
            ((BlockDoor) world.getBlockState(nextPos).getBlock()).toggleDoor(world, nextPos, true);
            this.rememberOpened(world, entity, nextPos);
        }
        closeDoorsBehind(world, entity, previous, next);
    }

    public static void closeDoorsBehind(WorldServer world, EntityLivingBase entity, @Nullable PathPoint previous, @Nullable PathPoint next) {
        Brain<?> brain = BrainUtil.brain(entity);
        if (!brain.hasMemory(MemoryModuleType.OPENED_DOORS)) return;
        for (Iterator<GlobalPos> it = brain.getMemory(MemoryModuleType.OPENED_DOORS).get().iterator(); it.hasNext(); ) {
            GlobalPos door = it.next();
            BlockPos pos = door.getPos();
            if (previous != null && pos(previous).equals(pos) || next != null && pos(next).equals(pos)) continue;
            if (isFarFrom(world, entity, door)) {
                it.remove();
                continue;
            }
            IBlockState state = world.getBlockState(pos);
            if (!isWoodenDoor(state) || !BlockDoor.isOpen(world, pos)) {
                it.remove();
            } else if (anotherIsPassing(entity, pos)) {
                it.remove();
            } else {
                ((BlockDoor) state.getBlock()).toggleDoor(world, pos, false);
                it.remove();
            }
        }
    }

    private static boolean anotherIsPassing(EntityLivingBase entity, BlockPos door) {
        Optional<List<EntityLivingBase>> mobs = BrainUtil.brain(entity).getMemory(MemoryModuleType.MOBS);
        return mobs.isPresent() && mobs.get().stream().filter(other -> other.getClass() == entity.getClass())
                .filter(other -> door.distanceSqToCenter(other.posX, other.posY, other.posZ) < 4.0D)
                .anyMatch(other -> isPathingThrough(other, door));
    }

    private static boolean isPathingThrough(EntityLivingBase other, BlockPos door) {
        if (!(other instanceof com.michaelsebero.portium.entity.ai.brain.IBrainHolder)) return false;
        Optional<Path> path = BrainUtil.brain(other).getMemory(MemoryModuleType.PATH);
        if (!path.isPresent() || path.get().isFinished()) return false;
        PathPoint previous = previous(path.get());
        return previous != null && (door.equals(pos(previous)) || door.equals(pos(current(path.get()))));
    }

    private static boolean isFarFrom(WorldServer world, EntityLivingBase entity, GlobalPos door) {
        return door.getDimension() != world.provider.getDimension() || door.getPos().distanceSqToCenter(entity.posX, entity.posY, entity.posZ) >= 4.0D;
    }

    private void rememberOpened(WorldServer world, EntityLivingBase entity, BlockPos pos) {
        Brain<?> brain = BrainUtil.brain(entity);
        GlobalPos door = GlobalPos.of(world.provider.getDimension(), pos);
        Optional<Set<GlobalPos>> opened = brain.getMemory(MemoryModuleType.OPENED_DOORS);
        if (opened.isPresent()) {
            opened.get().add(door);
        } else {
            brain.setMemory(MemoryModuleType.OPENED_DOORS, Sets.newHashSet(door));
        }
    }

    private static boolean isWoodenDoor(IBlockState state) {
        return state.getBlock() instanceof BlockDoor && state.getMaterial() == Material.WOOD;
    }

    private static PathPoint current(Path path) {
        return path.getPathPointFromIndex(Math.min(path.getCurrentPathIndex(), path.getCurrentPathLength() - 1));
    }

    @Nullable
    private static PathPoint previous(Path path) {
        int index = path.getCurrentPathIndex();
        return index > 0 && index - 1 < path.getCurrentPathLength() ? path.getPathPointFromIndex(index - 1) : null;
    }

    private static BlockPos pos(PathPoint point) {
        return new BlockPos(point.x, point.y, point.z);
    }
}
