package com.michaelsebero.portium.entity.ai.brain.task;

import com.michaelsebero.portium.entity.ai.brain.RangedInteger;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;

/** Runs another task, then waits a random cooldown before it may run again. */
public class RunSometimesTask<E extends EntityLivingBase> extends Task<E> {

    private boolean resetTicks;
    private boolean wasRunning;
    private final RangedInteger interval;
    private final Task<? super E> task;
    private int ticksUntilNextStart;

    public RunSometimesTask(Task<? super E> task, RangedInteger interval) {
        this(task, false, interval);
    }

    public RunSometimesTask(Task<? super E> task, boolean runFirst, RangedInteger interval) {
        super(task.requiredMemoryState);
        this.task = task;
        this.resetTicks = !runFirst;
        this.interval = interval;
    }

    @Override
    protected boolean shouldExecute(WorldServer world, E entity) {
        if (!this.task.shouldExecute(world, entity)) return false;
        if (this.resetTicks) {
            this.resetTicksUntilNextStart(world);
            this.resetTicks = false;
        }
        if (this.ticksUntilNextStart > 0) --this.ticksUntilNextStart;
        return !this.wasRunning && this.ticksUntilNextStart == 0;
    }

    @Override
    protected void startExecuting(WorldServer world, E entity, long gameTime) {
        this.task.startExecuting(world, entity, gameTime);
    }

    @Override
    protected boolean shouldContinueExecuting(WorldServer world, E entity, long gameTime) {
        return this.task.shouldContinueExecuting(world, entity, gameTime);
    }

    @Override
    protected void updateTask(WorldServer world, E entity, long gameTime) {
        this.task.updateTask(world, entity, gameTime);
        this.wasRunning = this.task.getStatus() == Status.RUNNING;
    }

    @Override
    protected void resetTask(WorldServer world, E entity, long gameTime) {
        this.resetTicksUntilNextStart(world);
        this.task.resetTask(world, entity, gameTime);
    }

    private void resetTicksUntilNextStart(WorldServer world) {
        this.ticksUntilNextStart = this.interval.getRandomWithinRange(world.rand);
    }

    @Override
    protected boolean isTimedOut(long gameTime) {
        return false;
    }

    @Override
    public String toString() {
        return "RunSometimes: " + this.task;
    }
}
