package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.adapter.ModernTileEntity;
import com.michaelsebero.portium.modern.mc.tileentity.TileEntityType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;

/**
 * What a 1.16 mod's tile entities extend in place of 1.16's {@code TileEntity}: Portium's 1.13 one, loading with
 * the state of its block as 1.16 does ({@code read(state, tag)}), and taking Forge 1.16's client update with it too.
 * 1.12 reads a tile entity before its chunk is in the world, so the state comes from the chunk being read
 * ({@code MixinChunkLoaderTileEntity116}), or the one 1.16 code gave ({@link #readWithState}).
 */
public class TileEntity116 extends ModernTileEntity {

    /** The chunk whose tile entities 1.12 is reading, on this thread. */
    private static final ThreadLocal<Chunk> LOADING = new ThreadLocal<>();
    /** The state 1.16 code made a tile entity from a tag with. */
    private static final ThreadLocal<IBlockState> GIVEN = new ThreadLocal<>();

    public TileEntity116(TileEntityType<?> type) {
        super(type);
    }

    /** 1.16's load: the position and Forge's data, given the state its block is in. */
    @Srg("func_230337_a_")
    public void read(IBlockState state, NBTTagCompound tag) {
        super.readFromNBT(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        read(stateFor(tag), tag);
    }

    /** Forge 1.16's: the client's copy of the tile entity as its chunk arrives. */
    public void handleUpdateTag(IBlockState state, NBTTagCompound tag) {
        read(state, tag);
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        handleUpdateTag(stateFor(tag), tag);
    }

    /** Forge 1.16's, with the state. */
    public void deserializeNBT(IBlockState state, NBTTagCompound tag) {
        read(state, tag);
    }

    @Srg("func_226984_a_")
    public void setWorldAndPos(World world, BlockPos pos) {
        setWorld(world);
        setPos(pos);
    }

    private IBlockState stateFor(NBTTagCompound tag) {
        IBlockState given = GIVEN.get();
        if (given != null) return given;
        BlockPos at = new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
        Chunk chunk = LOADING.get();
        if (chunk != null && chunk.x == at.getX() >> 4 && chunk.z == at.getZ() >> 4) return chunk.getBlockState(at);
        if (this.world != null && this.world.isBlockLoaded(at)) return this.world.getBlockState(at);
        return Blocks.AIR.getDefaultState();
    }

    /** Marks the chunk 1.12 is reading tile entities for on this thread, or none. */
    public static void loading(@Nullable Chunk chunk) {
        if (chunk == null) {
            LOADING.remove();
        } else {
            LOADING.set(chunk);
        }
    }

    /** 1.16's {@code TileEntity.readTileEntity}: the tile entity a tag names, loaded with the given state; null if none. */
    @Nullable
    public static TileEntity readWithState(IBlockState state, NBTTagCompound tag) {
        GIVEN.set(state);
        try {
            return TileEntity.create(null, tag);
        } finally {
            GIVEN.remove();
        }
    }
}
