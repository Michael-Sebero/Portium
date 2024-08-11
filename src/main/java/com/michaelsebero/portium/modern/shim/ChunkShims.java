package com.michaelsebero.portium.modern.shim;

import com.michaelsebero.portium.modern.mc.fluid.IFluidState;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

/** 1.13's chunk methods: heights by kind of surface, fluids and biomes by position. */
public final class ChunkShims {

    /**
     * 1.13's {@code getTopBlockY}: the highest block of a kind in a column (by 1.13's height map names), or -1. 1.12
     * keeps one height map, for light; the others are found by looking down the column.
     */
    public static int getTopBlockY(Chunk chunk, String type, int x, int z) {
        int localX = x & 15;
        int localZ = z & 15;
        if ("LIGHT_BLOCKING".equals(type)) return chunk.getHeightValue(localX, localZ) - 1;
        int top = chunk.getTopFilledSegment() + 15;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = top; y >= 0; y--) {
            IBlockState state = chunk.getBlockState(pos.setPos(localX, y, localZ));
            Material material = state.getMaterial();
            if (material == Material.AIR) continue;
            switch (type) {
                case "MOTION_BLOCKING":
                    if (material.blocksMovement() || material.isLiquid()) return y;
                    break;
                case "MOTION_BLOCKING_NO_LEAVES":
                    if ((material.blocksMovement() || material.isLiquid()) && material != Material.LEAVES) return y;
                    break;
                case "OCEAN_FLOOR":
                case "OCEAN_FLOOR_WG":
                    if (material.blocksMovement()) return y;
                    break;
                default:
                    return y;
            }
        }
        return -1;
    }

    public static IFluidState getFluidState(Chunk chunk, BlockPos pos) {
        return FluidShims.getFluidState(chunk.getBlockState(pos));
    }

    public static Biome getBiome(Chunk chunk, BlockPos pos) {
        return chunk.getBiome(pos, chunk.getWorld().getBiomeProvider());
    }

    private ChunkShims() {}
}
