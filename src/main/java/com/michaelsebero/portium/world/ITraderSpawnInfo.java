package com.michaelsebero.portium.world;

import net.minecraft.world.storage.WorldInfo;

import javax.annotation.Nullable;
import java.util.UUID;

/** 1.14's wandering trader spawner state, kept in level.dat under 1.16's names (Portium's mixin on {@link WorldInfo}). */
public interface ITraderSpawnInfo {

    int portium$getTraderSpawnDelay();

    void portium$setTraderSpawnDelay(int delay);

    int portium$getTraderSpawnChance();

    void portium$setTraderSpawnChance(int chance);

    void portium$setTraderId(@Nullable UUID id);

    static ITraderSpawnInfo of(WorldInfo info) {
        return (ITraderSpawnInfo) info;
    }
}
