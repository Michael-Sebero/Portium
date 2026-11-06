package com.michaelsebero.portium.world.spawn;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.world.WorldGeneration;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntityPolarBear;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.init.Biomes;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

/**
 * Spawning in a world with 1.12.2's generation is 1.12's. Biome spawn lists are shared by every world, and Portium
 * gives them 1.13's mobs and tables ({@link PortiumSpawns}); a 1.12.2 world's spawns leave Portium's mobs out (cod,
 * salmon, pufferfish, tropical fish, dolphins, turtles, drowned), give squid back 1.12's weight (10, in groups of 4)
 * and leave out 1.13's polar bears in frozen oceans. Other mods' spawns are untouched. A 1.13.2 world leaves out
 * Portium's 1.14+ mobs ({@link MobVersions}).
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class LegacySpawns {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPotentialSpawns(WorldEvent.PotentialSpawns event) {
        World world = event.getWorld();
        if (world.isRemote) return;
        List<Biome.SpawnListEntry> list = event.getList();
        list.removeIf(entry -> !MobVersions.spawnsIn(world, entry.entityClass));
        if (WorldGeneration.isModern(world)) return;
        Biome biome = world.getBiome(event.getPos());
        if (event.getType() == EnumCreatureType.WATER_CREATURE && biome.getRegistryName() != null
                && "minecraft".equals(biome.getRegistryName().getNamespace())) {
            list.removeIf(entry -> entry.entityClass == EntitySquid.class);
            list.add(SQUID);
        }
        if (event.getType() == EnumCreatureType.CREATURE && (biome == Biomes.FROZEN_OCEAN || biome instanceof com.michaelsebero.portium.world.biome.BiomeFrozenOcean)) {
            list.removeIf(entry -> entry.entityClass == EntityPolarBear.class);
        }
    }

    /** One entry, shared: 1.12 checks its pick is still in a list it asks for again, by identity. */
    private static final Biome.SpawnListEntry SQUID = new Biome.SpawnListEntry(EntitySquid.class, 10, 4, 4);

    private LegacySpawns() {}
}
