package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/** Runs another task only while a condition holds ("RunIf"). */
public class SupplementedTask<E extends EntityLivingBase> extends Task<E> {

    private final Predicate<E> predicate;
    private final Task<? super E> task;
    private final boolean checkWhileRunning;

    public SupplementedTask(Map<MemoryModuleType<?>, MemoryModuleStatus> required, Predicate<E> predicate, Task<? super E> task, boolean checkWhileRunning) {
        super(merge(required, task.requiredMemoryState));
        this.predicate = predicate;
        this.task = task;
        this.checkWhileRunning = checkWhileRunning;
    }

    public SupplementedTask(Predicate<E> predicate, Task<? super E> task) {
        this(ImmutableMap.of(), predicate, task, false);
    }

    private static Map<MemoryModuleType<?>, MemoryModuleStatus> merge(Map<MemoryModuleType<?>, MemoryModuleStatus> first, Map<MemoryModuleType<?>, MemoryModuleStatus> second) {
        Map<MemoryModuleType<?>, MemoryModuleStatus> merged = new HashMap<>(first);
        merged.putAll(second);
        return merged;
    }

    @Override
    protected boolean shouldExecute(WorldServer world, E entity) {
        return this.predicate.test(entity) && this.task.shouldExecute(world, entity);
    }

    @Override
    protected boolean shouldContinueExecuting(WorldServer world, E entity, long gameTime) {
        return this.checkWhileRunning && this.predicate.test(entity) && this.task.shouldContinueExecuting(world, entity, gameTime);
    }

    @Override
    protected boolean isTimedOut(long gameTime) {
        return false;
    }

    @Override
    protected void startExecuting(WorldServer world, E entity, long gameTime) {
        this.task.startExecuting(world, entity, gameTime);
    }

    @Override
    protected void updateTask(WorldServer world, E entity, long gameTime) {
        this.task.updateTask(world, entity, gameTime);
    }

    @Override
    protected void resetTask(WorldServer world, E entity, long gameTime) {
        this.task.resetTask(world, entity, gameTime);
    }

    @Override
    public String toString() {
        return "RunIf: " + this.task;
    }
}
