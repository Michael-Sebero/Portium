package com.michaelsebero.portium.entity.ai;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.stats.StatList;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** 1.14's {@code BreedGoal}: as 1.12's mating task, but a mob can change how the baby comes ({@link #spawnBaby}). */
public class EntityAIBreed116 extends net.minecraft.entity.ai.EntityAIBase {

    protected final EntityAnimal animal;
    protected final World world;
    private final Class<? extends EntityAnimal> mateClass;
    protected EntityAnimal targetMate;
    private int spawnBabyDelay;
    private final double moveSpeed;

    public EntityAIBreed116(EntityAnimal animal, double moveSpeed) {
        this(animal, moveSpeed, animal.getClass());
    }

    public EntityAIBreed116(EntityAnimal animal, double moveSpeed, Class<? extends EntityAnimal> mateClass) {
        this.animal = animal;
        this.world = animal.world;
        this.mateClass = mateClass;
        this.moveSpeed = moveSpeed;
        this.setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        if (!this.animal.isInLove()) return false;
        this.targetMate = this.getNearbyMate();
        return this.targetMate != null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.targetMate.isEntityAlive() && this.targetMate.isInLove() && this.spawnBabyDelay < 60;
    }

    @Override
    public void resetTask() {
        this.targetMate = null;
        this.spawnBabyDelay = 0;
    }

    @Override
    public void updateTask() {
        this.animal.getLookHelper().setLookPositionWithEntity(this.targetMate, 10.0F, (float) this.animal.getVerticalFaceSpeed());
        this.animal.getNavigator().tryMoveToEntityLiving(this.targetMate, this.moveSpeed);
        ++this.spawnBabyDelay;
        if (this.spawnBabyDelay >= 60 && this.animal.getDistanceSq(this.targetMate) < 9.0D) this.spawnBaby();
    }

    @Nullable
    private EntityAnimal getNearbyMate() {
        List<? extends EntityAnimal> candidates = this.world.getEntitiesWithinAABB(this.mateClass, this.animal.getEntityBoundingBox().grow(8.0D));
        double nearest = Double.MAX_VALUE;
        EntityAnimal mate = null;
        for (EntityAnimal other : candidates) {
            if (other == this.animal || !other.isEntityAlive() || this.animal.getDistanceSq(other) > 64.0D) continue;
            if (this.animal.canMateWith(other) && this.animal.getDistanceSq(other) < nearest) {
                mate = other;
                nearest = this.animal.getDistanceSq(other);
            }
        }
        return mate;
    }

    /** 1.14's {@code AnimalEntity.spawnBabyAnimal}. */
    protected void spawnBaby() {
        EntityAgeable baby = this.animal.createChild(this.targetMate);
        if (baby == null) return;
        EntityPlayerMP player = this.animal.getLoveCause();
        if (player == null && this.targetMate.getLoveCause() != null) player = this.targetMate.getLoveCause();
        if (player != null) {
            player.addStat(StatList.ANIMALS_BRED);
            CriteriaTriggers.BRED_ANIMALS.trigger(player, this.animal, this.targetMate, baby);
        }
        this.finishBreeding(baby);
    }

    /** The parents' cooldown and the baby's arrival, with its hearts and experience. */
    protected void finishBreeding(EntityAgeable baby) {
        this.animal.setGrowingAge(6000);
        this.targetMate.setGrowingAge(6000);
        this.animal.resetInLove();
        this.targetMate.resetInLove();
        baby.setGrowingAge(-24000);
        baby.setLocationAndAngles(this.animal.posX, this.animal.posY, this.animal.posZ, 0.0F, 0.0F);
        this.world.spawnEntity(baby);
        this.world.setEntityState(this.animal, (byte) 18);
        if (this.world.getGameRules().getBoolean("doMobLoot")) {
            this.world.spawnEntity(new EntityXPOrb(this.world, this.animal.posX, this.animal.posY, this.animal.posZ, this.animal.getRNG().nextInt(7) + 1));
        }
    }
}
