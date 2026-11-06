package com.michaelsebero.portium.entity;

import net.minecraft.entity.EntityLivingBase;

/** A firework rocket shot from a crossbow (1.14): it flies straight at its aim and bursts on what it hits. */
public interface ICrossbowRocket {

    void portium$shootFrom(EntityLivingBase shooter);

    boolean portium$isShotAtAngle();

    void portium$shoot(double x, double y, double z, float velocity, float inaccuracy);
}
