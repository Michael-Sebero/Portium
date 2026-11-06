package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;

import java.util.function.Predicate;

/** Forgets a memory when a condition holds. */
public class PredicateTask<E extends EntityLivingBase> extends Task<E> {

    private final Predicate<E> predicate;
    private final MemoryModuleType<?> memory;

    public PredicateTask(Predicate<E> predicate, MemoryModuleType<?> memory) {
        super(ImmutableMap.of(memory, MemoryModuleStatus.VALUE_PRESENT));
        this.predicate = predicate;
        this.memory = memory;
    }

    @Override
    protected boolean shouldExecute(WorldServer world, E entity) {
        return this.predicate.test(entity);
    }

    @Override
    protected void startExecuting(WorldServer world, E entity, long gameTime) {
        BrainUtil.brain(entity).removeMemory(this.memory);
    }
}
