package com.michaelsebero.portium.api;

import net.minecraft.entity.monster.EntityZombie;

/** Implemented on {@link EntityZombie} by mixin: whether it is converting after drowning, synced so it can shake. */
public interface IDrowningZombie {

    boolean portium$isDrowning();

    void portium$setDrowning(boolean drowning);

    static IDrowningZombie of(EntityZombie zombie) {
        return (IDrowningZombie) zombie;
    }
}
