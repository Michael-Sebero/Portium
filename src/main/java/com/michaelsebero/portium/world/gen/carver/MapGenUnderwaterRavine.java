package com.michaelsebero.portium.world.gen.carver;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenRavine;

/** 1.13's underwater canyons: 1.12's ravines, started from ocean chunks (2% of them), carved full of water. */
public class MapGenUnderwaterRavine extends MapGenRavine {

    @Override
    protected void recursiveGenerate(World world, int chunkX, int chunkZ, int originalX, int originalZ, ChunkPrimer primer) {
        if (this.rand.nextInt(50) != 0 || !UnderwaterCarving.startsIn(world, chunkX, chunkZ)) return;
        double x = chunkX * 16 + this.rand.nextInt(16);
        double y = this.rand.nextInt(this.rand.nextInt(40) + 8) + 20;
        double z = chunkZ * 16 + this.rand.nextInt(16);
        float yaw = this.rand.nextFloat() * ((float) Math.PI * 2.0F);
        float pitch = (this.rand.nextFloat() - 0.5F) * 2.0F / 8.0F;
        float width = (this.rand.nextFloat() * 2.0F + this.rand.nextFloat()) * 2.0F;
        this.addTunnel(this.rand.nextLong(), originalX, originalZ, primer, x, y, z, width, yaw, pitch, 0, 0, 3.0D);
    }

    @Override
    protected boolean isOceanBlock(ChunkPrimer data, int x, int y, int z, int chunkX, int chunkZ) {
        return false;
    }

    @Override
    protected void digBlock(ChunkPrimer data, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop) {
        if (UnderwaterCarving.canCarve(data.getBlockState(x, y, z), this.world.getBiome(new BlockPos(x + chunkX * 16, 0, z + chunkZ * 16)))) {
            UnderwaterCarving.carve(data, x, y, z, this.world.getSeaLevel(), this.rand);
        }
    }
}
