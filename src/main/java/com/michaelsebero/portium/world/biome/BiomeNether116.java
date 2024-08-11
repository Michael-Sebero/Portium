package com.michaelsebero.portium.world.biome;

import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.Random;

/**
 * The Nether biomes 1.16 added: soul sand valleys, crimson and warped forests, and basalt deltas. Their terrain,
 * surfaces and features come from the 1.16 Nether generator, which only places them in worlds of 1.16.5 generation.
 * Mobs that 1.12 doesn't have yet (piglins, hoglins, striders) aren't in their spawn lists.
 */
public class BiomeNether116 extends Biome {

    public enum Kind {
        SOUL_SAND_VALLEY(1787717),
        CRIMSON_FOREST(3343107),
        WARPED_FOREST(1705242),
        BASALT_DELTAS(6840176);

        /** 1.16's fog colour for the biome. */
        public final int fogColor;

        Kind(int fogColor) {
            this.fogColor = fogColor;
        }
    }

    private final Kind kind;

    public BiomeNether116(String name, Kind kind) {
        super(new BiomeProperties(name).setTemperature(2.0F).setRainfall(0.0F).setRainDisabled());
        this.kind = kind;
        this.topBlock = Blocks.NETHERRACK.getDefaultState();
        this.fillerBlock = Blocks.NETHERRACK.getDefaultState();
        this.spawnableMonsterList.clear();
        this.spawnableCreatureList.clear();
        this.spawnableWaterCreatureList.clear();
        this.spawnableCaveCreatureList.clear();
        switch (kind) {
            case SOUL_SAND_VALLEY:
                this.spawnableMonsterList.add(new SpawnListEntry(EntitySkeleton.class, 20, 5, 5));
                this.spawnableMonsterList.add(new SpawnListEntry(EntityGhast.class, 50, 4, 4));
                this.spawnableMonsterList.add(new SpawnListEntry(EntityEnderman.class, 1, 4, 4));
                break;
            case CRIMSON_FOREST:
                this.spawnableMonsterList.add(new SpawnListEntry(EntityPigZombie.class, 1, 2, 4));
                break;
            case WARPED_FOREST:
                this.spawnableMonsterList.add(new SpawnListEntry(EntityEnderman.class, 1, 4, 4));
                break;
            case BASALT_DELTAS:
                this.spawnableMonsterList.add(new SpawnListEntry(EntityGhast.class, 40, 1, 1));
                this.spawnableMonsterList.add(new SpawnListEntry(EntityMagmaCube.class, 100, 2, 5));
                break;
        }
    }

    public Kind getKind() {
        return this.kind;
    }

    /** The 1.16 generator decorates these itself. */
    @Override
    public void decorate(World world, Random random, BlockPos pos) {
    }
}
