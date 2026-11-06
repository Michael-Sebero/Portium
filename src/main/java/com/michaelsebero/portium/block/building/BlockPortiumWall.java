package com.michaelsebero.portium.block.building;

import net.minecraft.block.Block;
import net.minecraft.block.BlockWall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.translation.I18n;

/**
 * A wall of a newer stone (1.14's and 1.16's walls). 1.12's wall block carries a cobblestone/mossy variant it can't
 * drop, so each wall is one of those with its variant fixed, ignored by its model (ClientRegistration) and its item.
 */
public class BlockPortiumWall extends BlockWall {

    /** 1.12's walls take their material's colour on maps (stone grey); 1.16's take their stone's. */
    private final net.minecraft.block.material.MapColor mapColor;

    public BlockPortiumWall(Block modelBlock) {
        this(modelBlock, modelBlock.getDefaultState().getMaterial().getMaterialMapColor());
    }

    public BlockPortiumWall(Block modelBlock, net.minecraft.block.material.MapColor mapColor) {
        super(modelBlock);
        this.mapColor = mapColor;
        this.setCreativeTab(CreativeTabs.DECORATIONS);
    }

    @Override
    @SuppressWarnings("deprecation")
    public net.minecraft.block.material.MapColor getMapColor(IBlockState state, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        return this.mapColor;
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        items.add(new ItemStack(this));
    }

    @Override
    public int damageDropped(IBlockState state) {
        return 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getLocalizedName() {
        return I18n.translateToLocal(getTranslationKey() + ".name");
    }
}
