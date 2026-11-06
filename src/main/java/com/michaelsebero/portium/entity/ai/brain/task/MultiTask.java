package com.michaelsebero.portium.entity.ai.brain.task;

import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.WeightedList;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** A group of weighted tasks, started in order or shuffled, one or all of them. */
public class MultiTask<E extends EntityLivingBase> extends Task<E> {

    private final Set<MemoryModuleType<?>> memoriesToClear;
    private final Ordering ordering;
    private final RunType runType;
    private final WeightedList<Task<? super E>> tasks = new WeightedList<>();

    public MultiTask(Map<MemoryModuleType<?>, MemoryModuleStatus> required, Set<MemoryModuleType<?>> memoriesToClear, Ordering ordering,
                     RunType runType, List<Pair<Task<? super E>, Integer>> tasks) {
        super(required);
        this.memoriesToClear = memoriesToClear;
        this.ordering = ordering;
        this.runType = runType;
        tasks.forEach(pair -> this.tasks.addWeighted(pair.getLeft(), pair.getRight()));
    }

    @Override
    protected boolean shouldContinueExecuting(WorldServer world, E entity, long gameTime) {
        return this.tasks.getValueStream().filter(task -> task.getStatus() == Status.RUNNING)
                .anyMatch(task -> task.shouldContinueExecuting(world, entity, gameTime));
    }

    @Override
    protected boolean isTimedOut(long gameTime) {
        return false;
    }

    @Override
    protected void startExecuting(WorldServer world, E entity, long gameTime) {
        if (this.ordering == Ordering.SHUFFLED) this.tasks.randomizeWithWeight();
        if (this.runType == RunType.RUN_ONE) {
            this.tasks.getValueStream().filter(task -> task.getStatus() == Status.STOPPED)
                    .filter(task -> task.start(world, entity, gameTime)).findFirst();
        } else {
            this.tasks.getValueStream().filter(task -> task.getStatus() == Status.STOPPED)
                    .forEach(task -> task.start(world, entity, gameTime));
        }
    }

    @Override
    protected void updateTask(WorldServer world, E entity, long gameTime) {
        this.tasks.getValueStream().filter(task -> task.getStatus() == Status.RUNNING).forEach(task -> task.tick(world, entity, gameTime));
    }

    @Override
    protected void resetTask(WorldServer world, E entity, long gameTime) {
        this.tasks.getValueStream().filter(task -> task.getStatus() == Status.RUNNING).forEach(task -> task.stop(world, entity, gameTime));
        this.memoriesToClear.forEach(BrainUtil.brain(entity)::removeMemory);
    }

    @Override
    public String toString() {
        return "(" + this.getClass().getSimpleName() + "): " + this.tasks.getValueStream()
                .filter(task -> task.getStatus() == Status.RUNNING).collect(Collectors.toSet());
    }

    public enum RunType {
        RUN_ONE,
        TRY_ALL
    }

    public enum Ordering {
        ORDERED,
        SHUFFLED
    }
}
