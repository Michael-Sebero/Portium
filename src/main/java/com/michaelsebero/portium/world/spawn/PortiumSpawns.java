package com.michaelsebero.portium.world.spawn;

import com.michaelsebero.portium.entity.fish.EntityCod;
import com.michaelsebero.portium.entity.fish.EntityPufferfish;
import com.michaelsebero.portium.entity.fish.EntitySalmon;
import com.michaelsebero.portium.entity.fish.EntityTropicalFish;
import com.michaelsebero.portium.entity.monster.EntityDrowned;
import com.michaelsebero.portium.entity.passive.EntityDolphin;
import com.michaelsebero.portium.entity.passive.EntityTurtle;
import com.michaelsebero.portium.init.PortiumBiomes;
import com.michaelsebero.portium.init.PortiumEntities;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;

import java.util.Arrays;
import java.util.List;

/** Where 1.13's new mobs spawn, with 1.13's per-biome spawn tables for the oceans, rivers and beaches. */
public final class PortiumSpawns {

    public static void registerPlacements() {
        List<Class<? extends EntityLiving>> swimmers = Arrays.asList(EntityCod.class, EntitySalmon.class, EntityPufferfish.class,
                EntityTropicalFish.class, EntityDolphin.class, EntityDrowned.class);
        for (Class<? extends EntityLiving> type : swimmers) {
            EntitySpawnPlacementRegistry.setPlacementType(type, EntityLiving.SpawnPlacementType.IN_WATER);
        }
    }

    public static void addBiomeSpawns() {
        water(Biomes.OCEAN, squid(1, 1, 4), entry(EntityCod.class, 10, 3, 6), entry(EntityDolphin.class, 1, 1, 2));
        water(Biomes.DEEP_OCEAN, squid(1, 1, 4), entry(EntityCod.class, 10, 3, 6), entry(EntityDolphin.class, 1, 1, 2));
        water(PortiumBiomes.COLD_OCEAN, squid(3, 1, 4), entry(EntityCod.class, 15, 3, 6), entry(EntitySalmon.class, 15, 1, 5));
        water(PortiumBiomes.DEEP_COLD_OCEAN, squid(3, 1, 4), entry(EntityCod.class, 15, 3, 6), entry(EntitySalmon.class, 15, 1, 5));
        water(PortiumBiomes.FROZEN_OCEAN, squid(1, 1, 4), entry(EntitySalmon.class, 15, 1, 5));
        water(PortiumBiomes.DEEP_FROZEN_OCEAN, squid(1, 1, 4), entry(EntitySalmon.class, 15, 1, 5));
        water(PortiumBiomes.LUKEWARM_OCEAN, squid(10, 1, 2), entry(EntityCod.class, 15, 3, 6), entry(EntityPufferfish.class, 5, 1, 3),
                entry(EntityTropicalFish.class, 25, 8, 8), entry(EntityDolphin.class, 2, 1, 2));
        water(PortiumBiomes.DEEP_LUKEWARM_OCEAN, squid(8, 1, 4), entry(EntityCod.class, 8, 3, 6), entry(EntityPufferfish.class, 5, 1, 3),
                entry(EntityTropicalFish.class, 25, 8, 8), entry(EntityDolphin.class, 2, 1, 2));
        water(PortiumBiomes.WARM_OCEAN, squid(10, 4, 4), entry(EntityPufferfish.class, 15, 1, 3), entry(EntityTropicalFish.class, 25, 8, 8));
        water(PortiumBiomes.DEEP_WARM_OCEAN, squid(10, 4, 4), entry(EntityPufferfish.class, 15, 1, 3), entry(EntityTropicalFish.class, 25, 8, 8));
        water(Biomes.RIVER, squid(2, 1, 4), entry(EntitySalmon.class, 5, 1, 5));
        water(Biomes.FROZEN_RIVER, squid(2, 1, 4), entry(EntitySalmon.class, 5, 1, 5));

        for (Biome ocean : new Biome[] {Biomes.OCEAN, Biomes.DEEP_OCEAN, PortiumBiomes.COLD_OCEAN, PortiumBiomes.DEEP_COLD_OCEAN, PortiumBiomes.FROZEN_OCEAN,
                PortiumBiomes.DEEP_FROZEN_OCEAN, PortiumBiomes.LUKEWARM_OCEAN, PortiumBiomes.DEEP_LUKEWARM_OCEAN, PortiumBiomes.WARM_OCEAN, PortiumBiomes.DEEP_WARM_OCEAN}) {
            drowned(ocean, 5);
        }
        drowned(Biomes.RIVER, 100);
        drowned(Biomes.FROZEN_RIVER, 1);

        if (PortiumEntities.isRegistered(EntityTurtle.class)) {
            Biomes.BEACH.getSpawnableList(EnumCreatureType.CREATURE).add(entry(EntityTurtle.class, 5, 2, 5));
        }
    }

    /** Replaces a biome's water creatures with 1.13's list, leaving out mobs that are switched off. */
    private static void water(Biome biome, Biome.SpawnListEntry... entries) {
        List<Biome.SpawnListEntry> list = biome.getSpawnableList(EnumCreatureType.WATER_CREATURE);
        list.clear();
        for (Biome.SpawnListEntry entry : entries) {
            if (entry.entityClass == EntitySquid.class || PortiumEntities.isRegistered(entry.entityClass)) list.add(entry);
        }
    }

    private static void drowned(Biome biome, int weight) {
        if (!PortiumEntities.isRegistered(EntityDrowned.class)) return;
        biome.getSpawnableList(EnumCreatureType.MONSTER).add(entry(EntityDrowned.class, weight, 1, 1));
    }

    private static Biome.SpawnListEntry squid(int weight, int min, int max) {
        return entry(EntitySquid.class, weight, min, max);
    }

    private static Biome.SpawnListEntry entry(Class<? extends EntityLiving> type, int weight, int min, int max) {
        return new Biome.SpawnListEntry(type, weight, min, max);
    }

    private PortiumSpawns() {}
}
