package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityLivingBase;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

/** Runs one of its tasks, picked at random by weight. */
public class FirstShuffledTask<E extends EntityLivingBase> extends MultiTask<E> {

    public FirstShuffledTask(List<Pair<Task<? super E>, Integer>> tasks) {
        this(ImmutableMap.of(), tasks);
    }

    public FirstShuffledTask(Map<MemoryModuleType<?>, MemoryModuleStatus> required, List<Pair<Task<? super E>, Integer>> tasks) {
        super(required, ImmutableSet.of(), Ordering.SHUFFLED, RunType.RUN_ONE, tasks);
    }
}
