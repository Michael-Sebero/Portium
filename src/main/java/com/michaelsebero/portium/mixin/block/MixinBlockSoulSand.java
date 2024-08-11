package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.block.BlockBubbleColumn;
import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSoulSand;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Random;

/** Soul sand pushes an upward bubble column into the water above it (1.13). */
@Mixin(BlockSoulSand.class)
public abstract class MixinBlockSoulSand extends Block {

    private MixinBlockSoulSand(Material material) {
        super(material);
    }

    @Override
    public int tickRate(World world) {
        return PortiumBlocks.BUBBLE_COLUMN == null ? super.tickRate(world) : 20;
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        this.portium$scheduleIfWaterAbove(world, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        this.portium$scheduleIfWaterAbove(world, pos);
    }

    /** Only still water can hold a column, so skip the tick otherwise (soul sand and magma are common in the Nether). */
    private void portium$scheduleIfWaterAbove(World world, BlockPos pos) {
        if (PortiumBlocks.BUBBLE_COLUMN != null && BlockBubbleColumn.canHoldBubbleColumn(world, pos.up())) world.scheduleUpdate(pos, this, this.tickRate(world));
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        BlockBubbleColumn.placeBubbleColumn(world, pos.up(), false);
    }
}
