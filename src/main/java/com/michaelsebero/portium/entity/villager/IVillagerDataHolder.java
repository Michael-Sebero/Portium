package com.michaelsebero.portium.entity.villager;

/** Villagers and zombie villagers: those with a biome type, profession and level. */
public interface IVillagerDataHolder {

    VillagerData116 getVillagerData();

    void setVillagerData(VillagerData116 data);

    /** Whether it has 1.14 data to show (zombie villagers outside 1.16.5 worlds don't, and keep 1.12's look). */
    default boolean hasVillagerData() {
        return true;
    }
}
