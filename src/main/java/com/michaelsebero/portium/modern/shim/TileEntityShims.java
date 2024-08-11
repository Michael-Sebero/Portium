package com.michaelsebero.portium.modern.shim;

import com.michaelsebero.portium.modern.adapter.ModernTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import java.util.function.Function;

/**
 * 1.13's tile entity methods, for 1.13 tile entities built on 1.12's vanilla ones (a sign, a chest) rather than on
 * Portium's {@link ModernTileEntity}.
 */
public final class TileEntityShims {

    public static IBlockState getBlockState(TileEntity tileEntity) {
        if (tileEntity instanceof ModernTileEntity) return ((ModernTileEntity) tileEntity).getBlockState();
        World world = tileEntity.getWorld();
        return world != null ? world.getBlockState(tileEntity.getPos()) : Blocks.AIR.getDefaultState();
    }

    /** 1.13's {@code getLidAngle}: the lid's opening, between ticks. */
    public static float getLidAngle(TileEntityChest chest, float partialTicks) {
        return chest.prevLidAngle + (chest.lidAngle - chest.prevLidAngle) * partialTicks;
    }

    /** {@code IChestLid.getLidAngle}, for 1.13 lids and 1.12 chests (which don't have the interface) alike. */
    public static float getLidAngle(com.michaelsebero.portium.modern.mc.tileentity.IChestLid lid, float partialTicks) {
        Object object = lid;
        if (!(object instanceof com.michaelsebero.portium.modern.mc.tileentity.IChestLid) && object instanceof TileEntityChest) {
            return getLidAngle((TileEntityChest) object, partialTicks);
        }
        if (!(object instanceof com.michaelsebero.portium.modern.mc.tileentity.IChestLid) && object instanceof net.minecraft.tileentity.TileEntityEnderChest) {
            net.minecraft.tileentity.TileEntityEnderChest chest = (net.minecraft.tileentity.TileEntityEnderChest) object;
            return chest.prevLidAngle + (chest.lidAngle - chest.prevLidAngle) * partialTicks;
        }
        return lid.getLidAngle(partialTicks);
    }

    public static ITextComponent getText(TileEntitySign sign, int line) {
        return sign.signText[line];
    }

    public static void setText(TileEntitySign sign, int line, ITextComponent text) {
        sign.signText[line] = text;
    }

    public static String getRenderText(TileEntitySign sign, int line, Function<ITextComponent, String> format) {
        return format.apply(sign.signText[line]);
    }

    private TileEntityShims() {}
}
