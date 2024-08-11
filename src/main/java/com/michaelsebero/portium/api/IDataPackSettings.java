package com.michaelsebero.portium.api;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.WorldInfo;

/**
 * Implemented on {@link WorldInfo} by mixin: the {@code level.dat} entries 1.13 added, stored exactly where 1.13 keeps
 * them: {@code DataPacks} (the enabled and disabled packs) and {@code CustomBossEvents} (boss bars from {@code /bossbar}).
 */
public interface IDataPackSettings {

    NBTTagCompound portium$getDataPacks();

    void portium$setDataPacks(NBTTagCompound dataPacks);

    NBTTagCompound portium$getCustomBossEvents();

    void portium$setCustomBossEvents(NBTTagCompound bossEvents);

    static IDataPackSettings of(WorldInfo info) {
        return (IDataPackSettings) info;
    }
}
