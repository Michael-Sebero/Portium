package com.michaelsebero.portium.mods116.host;

import com.mojang.serialization.Codec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.structure.StructureManager;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A 1.16 chunk generator standing for a 1.12 world's: what 1.16 code asks a generator about a world (its heights,
 * sea level, biomes; whether it's the debug world's) answered from the world 1.12 generated. It builds nothing:
 * 1.12's own generator makes the terrain.
 */
public final class LegacyChunkGenerator116 extends ChunkGenerator {

    private static final Map<Object, ChunkGenerator> BY_WORLD = Collections.synchronizedMap(new WeakHashMap<>());

    private World world;

    /** The stand-in for a world's generator, made once per world. */
    static ChunkGenerator of(Object world) {
        ChunkGenerator generator = BY_WORLD.get(world);
        if (generator == null) {
            generator = make((World) world);
            BY_WORLD.put(world, generator);
        }
        return generator;
    }

    /** 1.16's {@code serverChunkProvider.getGenerator()}. */
    public static ChunkGenerator ofProvider(Object provider) {
        return of(com.michaelsebero.portium.modern.shim.ChunkShims.worldOf(provider));
    }

    /**
     * Made without running 1.16's constructors, which need 1.16's structure settings and through them every
     * structure and its jigsaw pools, none of which a 1.12 world has; only the biome source is filled in.
     */
    private static ChunkGenerator make(World world) {
        try {
            LegacyChunkGenerator116 generator = WorldGenGlue.allocate(LegacyChunkGenerator116.class);
            generator.world = world;
            Biomes biomes = WorldGenGlue.allocate(Biomes.class);
            biomes.world = world;
            for (String name : new String[] {"field_222542_c", "field_235949_c_"}) {
                java.lang.reflect.Field field = ChunkGenerator.class.getDeclaredField(name);
                field.setAccessible(true);
                field.set(generator, biomes);
            }
            return generator;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Never run (see {@link #make}); 1.16's class needs one. */
    private LegacyChunkGenerator116() {
        super(null, null);
    }

    @Override
    protected Codec<? extends ChunkGenerator> func_230347_a_() {
        return ChunkGenerator.field_235948_a_;
    }

    @Override
    public ChunkGenerator func_230349_a_(long seed) {
        return this;
    }

    @Override
    public void func_225551_a_(WorldGenRegion region, IChunk chunk) {}

    @Override
    public void func_230352_b_(IWorld world, StructureManager structures, IChunk chunk) {}

    @Override
    public int func_222529_a(int x, int z, Heightmap.Type type) {
        return this.world.func_201676_a(type, x, z);
    }

    @Override
    public IBlockReader func_230348_a_(int x, int z) {
        return this.world;
    }

    @Override
    public int func_230356_f_() {
        return this.world.func_181545_F();
    }

    /** The world's biomes, as a 1.16 biome source (by quarter-block cells, as 1.16 asks). */
    private static final class Biomes extends BiomeProvider {

        private World world;

        /** Never run (see {@link #make}). */
        Biomes() {
            super(Collections.<Biome>emptyList());
        }

        @Override
        protected Codec<? extends BiomeProvider> func_230319_a_() {
            return BiomeProvider.field_235202_a_;
        }

        @Override
        public BiomeProvider func_230320_a_(long seed) {
            return this;
        }

        @Override
        public Biome func_225526_b_(int x, int y, int z) {
            return this.world.func_226691_t_(new BlockPos(x << 2, y << 2, z << 2));
        }
    }
}
