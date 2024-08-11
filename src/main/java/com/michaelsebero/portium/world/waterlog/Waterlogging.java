package com.michaelsebero.portium.world.waterlog;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.api.IWaterloggableBlock;
import com.michaelsebero.portium.block.water.IWaterloggable;
import com.michaelsebero.portium.block.water.WaterUtil;
import com.michaelsebero.portium.mixin.accessor.ChunkCacheAccessor;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;

/**
 * 1.13 waterlogging for vanilla and other mods' blocks ({@link WaterlogRules} says which): they can hold a water
 * source in their cell. The block's state stays the 1.12 state; which cells hold water is kept per chunk beside it,
 * so the world stays readable by 1.12. Portium's own aquatic blocks keep their water in their state instead
 * ({@link IWaterloggable}).
 */
public final class Waterlogging {

    /** The water surface in a waterlogged cell, as in a still source: 8/9 of a block up. */
    public static final double SURFACE = 1.0D - 1.0D / 9.0D;

    public static boolean isEnabled() {
        return PortiumConfig.mechanics.waterlogging;
    }

    /**
     * Whether a block can hold water in its cell: see {@link WaterlogRules}. Portium's aquatic blocks hold it in their
     * state instead, and blocks that implement {@link IWaterloggableBlock} decide for themselves.
     */
    public static boolean canWaterlog(IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof IWaterloggable) return false;
        if (block instanceof IWaterloggableBlock) return ((IWaterloggableBlock) block).canBeWaterlogged(state);
        return WaterlogRules.canWaterlog(state);
    }

    public static boolean isWaterlogged(IBlockAccess world, BlockPos pos) {
        if (!isEnabled()) return false;
        Chunk chunk = loadedChunk(world, pos);
        return chunk != null && ((IWaterlogChunk) chunk).portium$getWaterlog().get(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * The block a 1.12 water check should see at a cell: a still water source where the cell is waterlogged. Used
     * where 1.12 code asks "is the block next door water?" and 1.13 asks "is there water fluid?".
     */
    public static IBlockState withWater(IBlockAccess world, BlockPos pos) {
        return isWaterlogged(world, pos) ? Blocks.WATER.getDefaultState() : world.getBlockState(pos);
    }

    /** Whether a cell holds water in 1.13's sense: water, a waterlogged Portium block, or a waterlogged vanilla block. */
    public static boolean hasWater(IBlockAccess world, BlockPos pos) {
        return world.getBlockState(pos).getMaterial() == Material.WATER || isWaterlogged(world, pos);
    }

    /**
     * Fills or drains a cell. Returns false if nothing changed: the block can't hold water, it already does (or
     * doesn't), or water evaporates in this dimension.
     */
    public static boolean setWaterlogged(World world, BlockPos pos, boolean waterlogged) {
        if (!isEnabled()) return false;
        IBlockState state = world.getBlockState(pos);
        if (waterlogged && (!canWaterlog(state) || world.provider.doesWaterVaporize())) return false;
        Chunk chunk = world.getChunk(pos);
        if (!((IWaterlogChunk) chunk).portium$getWaterlog().set(pos.getX(), pos.getY(), pos.getZ(), waterlogged)) return false;
        chunk.markDirty();
        onChanged(world, pos, waterlogged);
        world.notifyNeighborsOfStateChange(pos, state.getBlock(), false);
        if (waterlogged && !world.isRemote) WaterlogFlow.schedule(world, pos);
        return true;
    }

    /**
     * Fills a cell during world generation: no neighbour updates and no flow, which could reach into chunks that
     * aren't generated yet. Structures placed underwater are surrounded by water anyway.
     */
    public static void setWaterloggedQuietly(World world, BlockPos pos) {
        if (!isEnabled() || world.provider.doesWaterVaporize() || !canWaterlog(world.getBlockState(pos))) return;
        Chunk chunk = world.getChunk(pos);
        if (((IWaterlogChunk) chunk).portium$getWaterlog().set(pos.getX(), pos.getY(), pos.getZ(), true)) {
            chunk.markDirty();
            onChanged(world, pos, true);
        }
    }

    /**
     * Whether a cell is open to still water: a water source above it or beside it. Generated blocks there hold water
     * even where the sea floor stood before, so nothing under the sea is left with a dry gap. Only asks loaded chunks.
     */
    public static boolean isOpenToWater(World world, BlockPos pos) {
        for (EnumFacing side : new EnumFacing[] {EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST}) {
            BlockPos next = pos.offset(side);
            if (world.isBlockLoaded(next) && WaterUtil.isPlainWaterSource(world.getBlockState(next))) return true;
        }
        return false;
    }

    /** What a waterlogged cell leaves when its block is broken: its water. */
    public static IBlockState leftBehind(World world, BlockPos pos, IBlockState otherwise) {
        return isWaterlogged(world, pos) && !world.provider.doesWaterVaporize() ? Blocks.WATER.getDefaultState() : otherwise;
    }

    /** After a cell's water changed: tell the players who can see it, or redraw it on a client. */
    public static void onChanged(World world, BlockPos pos, boolean waterlogged) {
        if (world instanceof WorldServer) WaterlogSync.sendUpdate((WorldServer) world, pos, waterlogged);
        else if (world != null) world.markBlockRangeForRenderUpdate(pos.add(-1, -1, -1), pos.add(1, 1, 1));
    }

    /** The chunk holding a position, without loading it; render threads see the copy their region cache took. */
    @Nullable
    private static Chunk loadedChunk(IBlockAccess access, BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        if (access instanceof World) return ((World) access).getChunkProvider().getLoadedChunk(chunkX, chunkZ);
        if (access instanceof ChunkCache) {
            ChunkCacheAccessor cache = (ChunkCacheAccessor) access;
            Chunk[][] chunks = cache.portium$getChunks();
            int i = chunkX - cache.portium$getChunkX();
            int j = chunkZ - cache.portium$getChunkZ();
            if (i >= 0 && i < chunks.length && j >= 0 && j < chunks[i].length) return chunks[i][j];
        }
        return null;
    }

    private Waterlogging() {}
}
