package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.EntityPosWrapper;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.WorldServer;

import java.util.function.Predicate;

/** Picks a seen entity matching a predicate, within a distance, as the look target. */
public class LookAtEntityTask extends Task<EntityLivingBase> {

    private final Predicate<EntityLivingBase> targetPredicate;
    private final float maxDistanceSq;

    public LookAtEntityTask(EnumCreatureType classification, float maxDistance) {
        this(entity -> entity.isCreatureType(classification, false), maxDistance);
    }

    public LookAtEntityTask(Class<? extends Entity> type, float maxDistance) {
        this(entity -> entity.getClass() == type, maxDistance);
    }

    public LookAtEntityTask(float maxDistance) {
        this(entity -> true, maxDistance);
    }

    public LookAtEntityTask(Predicate<EntityLivingBase> predicate, float maxDistance) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryModuleStatus.VALUE_ABSENT, MemoryModuleType.VISIBLE_MOBS, MemoryModuleStatus.VALUE_PRESENT));
        this.targetPredicate = predicate;
        this.maxDistanceSq = maxDistance * maxDistance;
    }

    @Override
    protected boolean shouldExecute(WorldServer world, EntityLivingBase entity) {
        return BrainUtil.brain(entity).getMemory(MemoryModuleType.VISIBLE_MOBS).get().stream().anyMatch(this.targetPredicate);
    }

    @Override
    protected void startExecuting(WorldServer world, EntityLivingBase entity, long gameTime) {
        Brain<?> brain = BrainUtil.brain(entity);
        brain.getMemory(MemoryModuleType.VISIBLE_MOBS).ifPresent(mobs -> mobs.stream().filter(this.targetPredicate)
                .filter(other -> other.getDistanceSq(entity) <= (double) this.maxDistanceSq).findFirst()
                .ifPresent(other -> brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityPosWrapper(other, true))));
    }
}
