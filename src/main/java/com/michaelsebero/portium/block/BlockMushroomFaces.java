package com.michaelsebero.portium.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

/**
 * The 1.13 mushroom blocks ({@code red_mushroom_block}, {@code brown_mushroom_block}, {@code mushroom_stem}),
 * whose six faces each show the outer skin or the pores.
 * <p>
 * 1.13 stores the six faces in the block state, which needs 64 states. Here the faces are computed from the
 * neighbours instead: a face shows pores when it touches the same kind of mushroom block. Vanilla's 1.12 mushroom
 * blocks still generate huge mushrooms; silk-touching them gives these blocks.
 */
public class BlockMushroomFaces extends Block {

    public static final PropertyBool UP = PropertyBool.create("up");
    public static final PropertyBool DOWN = PropertyBool.create("down");
    public static final PropertyBool NORTH = PropertyBool.create("north");
    public static final PropertyBool SOUTH = PropertyBool.create("south");
    public static final PropertyBool WEST = PropertyBool.create("west");
    public static final PropertyBool EAST = PropertyBool.create("east");
    private static final PropertyBool[] BY_FACING = {DOWN, UP, NORTH, SOUTH, WEST, EAST};

    @Nullable
    private final Block smallMushroom;

    /**
     * @param smallMushroom the mushroom this block drops 0-2 of, or {@code null} for the stem, which drops nothing.
     */
    public BlockMushroomFaces(MapColor mapColor, @Nullable Block smallMushroom) {
        super(Material.WOOD, mapColor);
        this.smallMushroom = smallMushroom;
        IBlockState state = this.blockState.getBaseState();
        for (PropertyBool face : BY_FACING) state = state.withProperty(face, true);
        this.setDefaultState(state);
        this.setHardness(0.2F);
        this.setSoundType(SoundType.WOOD);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
    }

    @Nonnull
    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        for (EnumFacing facing : EnumFacing.VALUES) {
            state = state.withProperty(BY_FACING[facing.getIndex()], world.getBlockState(pos.offset(facing)).getBlock() != this);
        }
        return state;
    }

    @Override
    public int quantityDropped(Random random) {
        return this.smallMushroom == null ? 0 : Math.max(0, random.nextInt(10) - 7);
    }

    @Nonnull
    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return this.smallMushroom == null ? Items.AIR : Item.getItemFromBlock(this.smallMushroom);
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, UP, DOWN, NORTH, SOUTH, WEST, EAST);
    }
}
