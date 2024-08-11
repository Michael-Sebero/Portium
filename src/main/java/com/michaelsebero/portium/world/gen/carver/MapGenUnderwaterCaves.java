package com.michaelsebero.portium.world.gen.carver;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenCaves;

/** 1.13's underwater caves: 1.12's cave tunnels, started from ocean chunks with 1.13's chance, carved full of water. */
public class MapGenUnderwaterCaves extends MapGenCaves {

    private static final float START_CHANCE = 1.0F / 15.0F;

    @Override
    protected void recursiveGenerate(World world, int chunkX, int chunkZ, int originalX, int originalZ, ChunkPrimer primer) {
        if (this.rand.nextFloat() > START_CHANCE || !UnderwaterCarving.startsIn(world, chunkX, chunkZ)) return;
        int caves = this.rand.nextInt(this.rand.nextInt(this.rand.nextInt(15) + 1) + 1);
        for (int i = 0; i < caves; ++i) {
            double x = chunkX * 16 + this.rand.nextInt(16);
            double y = this.rand.nextInt(this.rand.nextInt(120) + 8);
            double z = chunkZ * 16 + this.rand.nextInt(16);
            int tunnels = 1;
            if (this.rand.nextInt(4) == 0) {
                this.addRoom(this.rand.nextLong(), originalX, originalZ, primer, x, y, z);
                tunnels += this.rand.nextInt(4);
            }
            for (int j = 0; j < tunnels; ++j) {
                float yaw = this.rand.nextFloat() * ((float) Math.PI * 2.0F);
                float pitch = (this.rand.nextFloat() - 0.5F) * 2.0F / 8.0F;
                float width = this.rand.nextFloat() * 2.0F + this.rand.nextFloat();
                if (this.rand.nextInt(10) == 0) width *= this.rand.nextFloat() * this.rand.nextFloat() * 3.0F + 1.0F;
                this.addTunnel(this.rand.nextLong(), originalX, originalZ, primer, x, y, z, width, yaw, pitch, 0, 0, 1.0D);
            }
        }
    }

    /** Unlike land caves, these don't stop at water. */
    @Override
    protected boolean isOceanBlock(ChunkPrimer data, int x, int y, int z, int chunkX, int chunkZ) {
        return false;
    }

    @Override
    protected void digBlock(ChunkPrimer data, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop, IBlockState state, IBlockState up) {
        if (UnderwaterCarving.canCarve(state, this.world.getBiome(new BlockPos(x + chunkX * 16, 0, z + chunkZ * 16)))) {
            UnderwaterCarving.carve(data, x, y, z, this.world.getSeaLevel(), this.rand);
        }
    }
}
