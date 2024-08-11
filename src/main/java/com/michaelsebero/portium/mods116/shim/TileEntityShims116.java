package com.michaelsebero.portium.mods116.shim;

import com.michaelsebero.portium.mods116.adapter.TileEntity116;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;

/** 1.16's tile entity statics. */
public final class TileEntityShims116 {

    /** {@code TileEntity.readTileEntity}: made from a tag by its id, and loaded with the given state. */
    @Nullable
    public static TileEntity readTileEntity(IBlockState state, NBTTagCompound tag) {
        return TileEntity116.readWithState(state, tag);
    }

    private TileEntityShims116() {}
}
