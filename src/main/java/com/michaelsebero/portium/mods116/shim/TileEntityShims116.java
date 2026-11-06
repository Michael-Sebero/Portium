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

    /**
     * 1.16's container block entity classes are 1.16's own here (ModernMappings), so 1.12's chests, shulker boxes and
     * Portium's barrels aren't instances of them: these tests count them too (only tests; casts stay casts).
     */
    public static boolean isChest(Object tile) {
        return tile instanceof net.minecraft.tileentity.TileEntityChest || isNative(tile, "net.minecraft.tileentity.ChestTileEntity");
    }

    public static boolean isTrappedChest(Object tile) {
        return tile instanceof net.minecraft.tileentity.TileEntityChest
                && ((net.minecraft.tileentity.TileEntityChest) tile).getChestType() == net.minecraft.block.BlockChest.Type.TRAP
                || isNative(tile, "net.minecraft.tileentity.TrappedChestTileEntity");
    }

    public static boolean isShulkerBox(Object tile) {
        return tile instanceof net.minecraft.tileentity.TileEntityShulkerBox || isNative(tile, "net.minecraft.tileentity.ShulkerBoxTileEntity");
    }

    public static boolean isBarrel(Object tile) {
        return tile instanceof com.michaelsebero.portium.tile.TileEntityBarrel || isNative(tile, "net.minecraft.tileentity.BarrelTileEntity");
    }

    private static boolean isNative(Object object, String name) {
        if (object == null) return false;
        for (Class<?> c = object.getClass(); c != null; c = c.getSuperclass()) {
            if (c.getName().equals(name)) return true;
        }
        return false;
    }

    private TileEntityShims116() {}
}
