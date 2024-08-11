package com.michaelsebero.portium.block.building;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * A slab with a single variant, registered as a half block and a double block (the 1.12 slab model).
 */
public abstract class BlockPortiumSlab extends BlockSlab {

    public static final PropertyEnum<Variant> VARIANT = PropertyEnum.create("variant", Variant.class);

    private Block halfSlab = this;

    protected BlockPortiumSlab(Material material, MapColor mapColor) {
        super(material, mapColor);
        IBlockState state = this.blockState.getBaseState().withProperty(VARIANT, Variant.DEFAULT);
        if (!this.isDouble()) state = state.withProperty(HALF, EnumBlockHalf.BOTTOM);
        this.setDefaultState(state);
        this.setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        this.useNeighborBrightness = !this.isDouble();
    }

    public Block getHalfSlab() {
        return this.halfSlab;
    }

    @Nonnull
    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(this.halfSlab);
    }

    @Nonnull
    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(this.halfSlab);
    }

    @Nonnull
    @Override
    public String getTranslationKey(int meta) {
        return this.halfSlab.getTranslationKey();
    }

    @Nonnull
    @Override
    public IProperty<?> getVariantProperty() {
        return VARIANT;
    }

    @Nonnull
    @Override
    public Comparable<?> getTypeForItem(ItemStack stack) {
        return Variant.DEFAULT;
    }

    @Nonnull
    @Override
    public IBlockState getStateFromMeta(int meta) {
        IBlockState state = this.getDefaultState();
        if (!this.isDouble()) state = state.withProperty(HALF, (meta & 8) == 0 ? EnumBlockHalf.BOTTOM : EnumBlockHalf.TOP);
        return state;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return !this.isDouble() && state.getValue(HALF) == EnumBlockHalf.TOP ? 8 : 0;
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState() {
        return this.isDouble() ? new BlockStateContainer(this, VARIANT) : new BlockStateContainer(this, HALF, VARIANT);
    }

    public static class Half extends BlockPortiumSlab {

        public Half(Material material, MapColor mapColor) {
            super(material, mapColor);
        }

        @Override
        public boolean isDouble() {
            return false;
        }
    }

    public static class Double extends BlockPortiumSlab {

        public Double(Material material, MapColor mapColor, Block halfSlab) {
            super(material, mapColor);
            ((BlockPortiumSlab) this).halfSlab = halfSlab;
            this.setCreativeTab(null);
        }

        @Override
        public boolean isDouble() {
            return true;
        }
    }

    public enum Variant implements IStringSerializable {
        DEFAULT;

        @Nonnull
        @Override
        public String getName() {
            return "default";
        }
    }
}
