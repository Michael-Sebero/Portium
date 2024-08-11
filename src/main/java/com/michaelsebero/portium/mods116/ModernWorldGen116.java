package com.michaelsebero.portium.mods116;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

/**
 * 1.16 mods' world generation in 1.12's: 1.16 mods add their configured features (ores, rocks, plants) to biomes when
 * Forge 1.16 loads them; Portium asks them of 1.12's biomes (see the glue's {@code WorldGenGlue}) and places what
 * they added as 1.12 populates each chunk, in any world type. Like 1.12's own decoration, a chunk's features land in
 * the area 1.12 populates for it (offset half a chunk), so they don't spill into chunks not yet generated.
 */
public final class ModernWorldGen116 implements IWorldGenerator {

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider) {
        Biome biome = world.getBiome(new BlockPos((chunkX << 4) + 16, 0, (chunkZ << 4) + 16));
        if (biome.getRegistryName() == null) return;
        Mods116.generateFeatures(world, biome.getRegistryName().toString(), chunkX, chunkZ, world.getSeed());
    }
}
