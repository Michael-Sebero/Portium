package com.michaelsebero.portium.entity;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/**
 * 1.13's water mob base (fish and dolphins). Unlike 1.12's {@code EntityWaterMob} it is a creature, so it can use
 * pathfinding and the standard AI tasks. It spawns, and counts towards the cap, as a water creature.
 */
public abstract class EntityWaterCreature extends EntityCreature {

    protected EntityWaterCreature(World world) {
        super(world);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean getCanSpawnHere() {
        return true;
    }

    @Override
    public boolean isNotColliding() {
        return this.world.checkNoEntityCollision(this.getEntityBoundingBox(), this);
    }

    @Override
    public int getTalkInterval() {
        return 120;
    }

    @Override
    protected int getExperiencePoints(EntityPlayer player) {
        return 1 + this.world.rand.nextInt(3);
    }

    @Override
    public boolean isPushedByWater() {
        return false;
    }

    @Override
    public boolean canBeLeashedTo(EntityPlayer player) {
        return false;
    }

    @Override
    public boolean isCreatureType(EnumCreatureType type, boolean forSpawnCount) {
        if (forSpawnCount && this.isNoDespawnRequired()) return false;
        return type == EnumCreatureType.WATER_CREATURE;
    }

    @Override
    public void onEntityUpdate() {
        int air = this.getAir();
        super.onEntityUpdate();
        this.updateAir(air);
    }

    /** Water mobs suffocate on land, taking 2 damage every second once out of air. */
    protected void updateAir(int air) {
        if (this.isEntityAlive() && !this.isInWater()) {
            this.setAir(air - 1);
            if (this.getAir() == -20) {
                this.setAir(0);
                this.attackEntityFrom(DamageSource.DROWN, 2.0F);
            }
        } else {
            this.setAir(300);
        }
    }
}
