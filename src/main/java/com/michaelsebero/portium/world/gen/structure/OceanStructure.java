package com.michaelsebero.portium.world.gen.structure;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * A structure placed on 1.13's scattered-feature grid: the world is split into square regions of {@code spacing}
 * chunks and each region gets at most one start, at a random chunk at least {@code separation} chunks from the next
 * region's. The random choice is seeded from the world seed, region and a per-structure salt, as in 1.13.
 */
public abstract class OceanStructure {

    private final String name;
    private final int spacing;
    private final int separation;
    private final int salt;

    protected OceanStructure(String name, int spacing, int separation, int salt) {
        this.name = name;
        this.spacing = spacing;
        this.separation = separation;
        this.salt = salt;
    }

    /** The name used by {@code /locate} and explorer maps. */
    public String getName() {
        return this.name;
    }

    protected abstract boolean canSpawnIn(Biome biome);

    /** Lays out the structure's pieces for a start chunk. {@code random} is seeded for that chunk. */
    protected abstract List<StructurePiece> createPieces(World world, Random random, int chunkX, int chunkZ, Biome biome);

    /** The chunk in a region where this structure may start. */
    public ChunkPos getStartInRegion(long seed, int regionX, int regionZ) {
        Random random = new Random(regionX * 341873128712L + regionZ * 132897987541L + seed + this.salt);
        int x = regionX * this.spacing + random.nextInt(this.spacing - this.separation);
        int z = regionZ * this.spacing + random.nextInt(this.spacing - this.separation);
        return new ChunkPos(x, z);
    }

    public int regionOf(int chunkCoord) {
        return Math.floorDiv(chunkCoord, this.spacing);
    }

    /** Whether a structure starts in this chunk: it's the region's chosen chunk, in a suitable biome. */
    public boolean hasStartAt(World world, int chunkX, int chunkZ) {
        ChunkPos start = this.getStartInRegion(world.getSeed(), this.regionOf(chunkX), this.regionOf(chunkZ));
        return start.x == chunkX && start.z == chunkZ && this.canSpawnIn(startBiome(world, chunkX, chunkZ));
    }

    static Biome startBiome(World world, int chunkX, int chunkZ) {
        return world.getBiomeProvider().getBiome(new BlockPos((chunkX << 4) + 9, 0, (chunkZ << 4) + 9));
    }

    /** 1.13 seeds each structure start's layout from the world seed and its chunk. */
    static Random startRandom(long seed, int chunkX, int chunkZ) {
        Random random = new Random(seed);
        long a = random.nextLong();
        long b = random.nextLong();
        random.setSeed(chunkX * a ^ chunkZ * b ^ seed);
        return random;
    }

    public List<StructurePiece> getPieces(World world, int chunkX, int chunkZ) {
        return this.createPieces(world, startRandom(world.getSeed(), chunkX, chunkZ), chunkX, chunkZ, startBiome(world, chunkX, chunkZ));
    }

    /** The block a locator points to for a start. */
    public BlockPos getLocatorPos(int chunkX, int chunkZ) {
        return new BlockPos((chunkX << 4) + 8, 0, (chunkZ << 4) + 8);
    }

    /** The nearest start, searching outwards ring by ring up to {@code radius} regions away (1.13's locate). */
    @Nullable
    public BlockPos findNearest(World world, BlockPos pos, int radius) {
        int originX = this.regionOf(pos.getX() >> 4);
        int originZ = this.regionOf(pos.getZ() >> 4);
        for (int ring = 0; ring <= radius; ++ring) {
            for (int dx = -ring; dx <= ring; ++dx) {
                for (int dz = -ring; dz <= ring; ++dz) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) continue;
                    ChunkPos start = this.getStartInRegion(world.getSeed(), originX + dx, originZ + dz);
                    if (this.hasStartAt(world, start.x, start.z)) return this.getLocatorPos(start.x, start.z);
                }
            }
        }
        return null;
    }
}
