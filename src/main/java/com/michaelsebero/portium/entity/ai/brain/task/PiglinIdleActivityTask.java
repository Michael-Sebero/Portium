package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.RangedInteger;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityLiving;
import net.minecraft.world.WorldServer;

import java.util.function.Predicate;

/** Copies one memory into another for a random time (piglins and hoglins: what to avoid, and for how long). */
public class PiglinIdleActivityTask<E extends EntityLiving, T> extends Task<E> {

    private final Predicate<E> predicate;
    private final MemoryModuleType<? extends T> from;
    private final MemoryModuleType<T> to;
    private final RangedInteger duration;

    public PiglinIdleActivityTask(Predicate<E> predicate, MemoryModuleType<? extends T> from, MemoryModuleType<T> to, RangedInteger duration) {
        super(ImmutableMap.of(from, MemoryModuleStatus.VALUE_PRESENT, to, MemoryModuleStatus.VALUE_ABSENT));
        this.predicate = predicate;
        this.from = from;
        this.to = to;
        this.duration = duration;
    }

    @Override
    protected boolean shouldExecute(WorldServer world, E entity) {
        return this.predicate.test(entity);
    }

    @Override
    protected void startExecuting(WorldServer world, E entity, long gameTime) {
        Brain<?> brain = BrainUtil.brain(entity);
        brain.replaceMemory(this.to, brain.getMemory(this.from).get(), this.duration.getRandomWithinRange(world.rand));
    }
}
