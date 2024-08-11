package com.michaelsebero.portium.api;

import net.minecraft.entity.EntityLivingBase;

/**
 * Implemented on {@link EntityLivingBase} by mixin: the riptide spin attack (1.13). While spinning, the entity
 * damages the first living entity it touches and bounces off it. The spinning state is synced to clients.
 */
public interface IRiptideEntity {

    void portium$startSpinAttack(int ticks);

    boolean portium$isSpinAttacking();

    static IRiptideEntity of(EntityLivingBase entity) {
        return (IRiptideEntity) entity;
    }
}
