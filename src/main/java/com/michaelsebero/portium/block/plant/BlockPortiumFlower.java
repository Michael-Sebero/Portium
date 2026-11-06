package com.michaelsebero.portium.block.plant;

import net.minecraft.block.BlockBush;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.potion.Potion;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * 1.14's flowers (cornflower, lily of the valley; the wither rose builds on it). 1.12's BlockFlower is tied to its
 * fixed flower list, so this is a bush with the flower's shape and offset. The effect is what 1.14's suspicious stew
 * gives when made with it.
 */
public class BlockPortiumFlower extends BlockBush {

    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(0.3125D, 0.0D, 0.3125D, 0.6875D, 0.625D, 0.6875D);

    private final Potion stewEffect;
    /** In ticks. */
    private final int stewDuration;

    public BlockPortiumFlower(Potion stewEffect, int stewSeconds) {
        super(Material.PLANTS);
        this.stewEffect = stewEffect;
        this.stewDuration = stewSeconds * 20;
        this.setHardness(0.0F);
        this.setSoundType(SoundType.PLANT);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
    }

    public Potion getStewEffect() {
        return this.stewEffect;
    }

    public int getStewDuration() {
        return this.stewDuration;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SHAPE.offset(state.getOffset(world, pos));
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.XZ;
    }
}
