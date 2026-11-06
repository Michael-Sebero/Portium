package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.world.WorldServer;

import java.util.Optional;
import java.util.function.BiConsumer;

/** Two animals in love walk to each other and, after 275-325 ticks together, make a baby. */
public class AnimalBreedTask extends Task<EntityAnimal> {

    private final Class<? extends EntityAnimal> type;
    private final float speed;
    private final BiConsumer<EntityAnimal, EntityAnimal> spawnBaby;
    private long breedTime;

    public AnimalBreedTask(Class<? extends EntityAnimal> type, float speed, BiConsumer<EntityAnimal, EntityAnimal> spawnBaby) {
        super(ImmutableMap.of(MemoryModuleType.VISIBLE_MOBS, MemoryModuleStatus.VALUE_PRESENT, MemoryModuleType.BREED_TARGET, MemoryModuleStatus.VALUE_ABSENT,
                MemoryModuleType.WALK_TARGET, MemoryModuleStatus.REGISTERED, MemoryModuleType.LOOK_TARGET, MemoryModuleStatus.REGISTERED), 325);
        this.type = type;
        this.speed = speed;
        this.spawnBaby = spawnBaby;
    }

    @Override
    protected boolean shouldExecute(WorldServer world, EntityAnimal entity) {
        return entity.isInLove() && this.nearestMate(entity).isPresent();
    }

    @Override
    protected void startExecuting(WorldServer world, EntityAnimal entity, long gameTime) {
        EntityAnimal mate = this.nearestMate(entity).get();
        BrainUtil.brain(entity).setMemory(MemoryModuleType.BREED_TARGET, mate);
        BrainUtil.brain(mate).setMemory(MemoryModuleType.BREED_TARGET, entity);
        BrainUtil.lookApproachEachOther(entity, mate, this.speed);
        this.breedTime = gameTime + 275 + entity.getRNG().nextInt(50);
    }

    @Override
    protected boolean shouldContinueExecuting(WorldServer world, EntityAnimal entity, long gameTime) {
        if (!this.hasBreedTarget(entity)) return false;
        EntityAnimal mate = breedTarget(entity);
        return mate.isEntityAlive() && entity.canMateWith(mate) && BrainUtil.canSee(BrainUtil.brain(entity), mate) && gameTime <= this.breedTime;
    }

    @Override
    protected void updateTask(WorldServer world, EntityAnimal entity, long gameTime) {
        EntityAnimal mate = breedTarget(entity);
        BrainUtil.lookApproachEachOther(entity, mate, this.speed);
        if (entity.getDistanceSq(mate) < 9.0D && gameTime >= this.breedTime) {
            this.spawnBaby.accept(entity, mate);
            BrainUtil.brain(entity).removeMemory(MemoryModuleType.BREED_TARGET);
            BrainUtil.brain(mate).removeMemory(MemoryModuleType.BREED_TARGET);
        }
    }

    @Override
    protected void resetTask(WorldServer world, EntityAnimal entity, long gameTime) {
        Brain<?> brain = BrainUtil.brain(entity);
        brain.removeMemory(MemoryModuleType.BREED_TARGET);
        brain.removeMemory(MemoryModuleType.WALK_TARGET);
        brain.removeMemory(MemoryModuleType.LOOK_TARGET);
        this.breedTime = 0L;
    }

    private static EntityAnimal breedTarget(EntityAnimal entity) {
        return (EntityAnimal) BrainUtil.brain(entity).getMemory(MemoryModuleType.BREED_TARGET).get();
    }

    private boolean hasBreedTarget(EntityAnimal entity) {
        Optional<EntityAgeable> target = BrainUtil.brain(entity).getMemory(MemoryModuleType.BREED_TARGET);
        return target.isPresent() && target.get().getClass() == this.type;
    }

    private Optional<EntityAnimal> nearestMate(EntityAnimal entity) {
        return BrainUtil.brain(entity).getMemory(MemoryModuleType.VISIBLE_MOBS).get().stream().filter(other -> other.getClass() == this.type)
                .map(other -> (EntityAnimal) other).filter(entity::canMateWith).findFirst();
    }
}
