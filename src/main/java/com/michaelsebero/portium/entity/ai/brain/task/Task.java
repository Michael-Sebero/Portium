package com.michaelsebero.portium.entity.ai.brain.task;

import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;

import java.util.Map;

/**
 * 1.14's brain task: starts when its required memories are as listed and {@link #shouldExecute} agrees, runs until
 * {@link #shouldContinueExecuting} says stop or its random duration is up.
 */
public abstract class Task<E extends EntityLivingBase> {

    protected final Map<MemoryModuleType<?>, MemoryModuleStatus> requiredMemoryState;
    private Status status = Status.STOPPED;
    private long stopTime;
    private final int durationMin;
    private final int durationMax;

    public Task(Map<MemoryModuleType<?>, MemoryModuleStatus> required) {
        this(required, 60);
    }

    public Task(Map<MemoryModuleType<?>, MemoryModuleStatus> required, int duration) {
        this(required, duration, duration);
    }

    public Task(Map<MemoryModuleType<?>, MemoryModuleStatus> required, int durationMin, int durationMax) {
        this.durationMin = durationMin;
        this.durationMax = durationMax;
        this.requiredMemoryState = required;
    }

    public Status getStatus() {
        return this.status;
    }

    public final boolean start(WorldServer world, E entity, long gameTime) {
        if (this.hasRequiredMemories(entity) && this.shouldExecute(world, entity)) {
            this.status = Status.RUNNING;
            int duration = this.durationMin + world.rand.nextInt(this.durationMax + 1 - this.durationMin);
            this.stopTime = gameTime + duration;
            this.startExecuting(world, entity, gameTime);
            return true;
        }
        return false;
    }

    protected void startExecuting(WorldServer world, E entity, long gameTime) {
    }

    public final void tick(WorldServer world, E entity, long gameTime) {
        if (!this.isTimedOut(gameTime) && this.shouldContinueExecuting(world, entity, gameTime)) {
            this.updateTask(world, entity, gameTime);
        } else {
            this.stop(world, entity, gameTime);
        }
    }

    protected void updateTask(WorldServer world, E entity, long gameTime) {
    }

    public final void stop(WorldServer world, E entity, long gameTime) {
        this.status = Status.STOPPED;
        this.resetTask(world, entity, gameTime);
    }

    protected void resetTask(WorldServer world, E entity, long gameTime) {
    }

    protected boolean shouldContinueExecuting(WorldServer world, E entity, long gameTime) {
        return false;
    }

    protected boolean isTimedOut(long gameTime) {
        return gameTime > this.stopTime;
    }

    protected boolean shouldExecute(WorldServer world, E entity) {
        return true;
    }

    private boolean hasRequiredMemories(E entity) {
        for (Map.Entry<MemoryModuleType<?>, MemoryModuleStatus> entry : this.requiredMemoryState.entrySet()) {
            if (!BrainUtil.brain(entity).hasMemory(entry.getKey(), entry.getValue())) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    public enum Status {
        STOPPED,
        RUNNING
    }
}
