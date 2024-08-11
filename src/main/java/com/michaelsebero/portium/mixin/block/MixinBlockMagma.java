package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.block.BlockBubbleColumn;
import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockMagma;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * Magma pulls a downward bubble column into the water above it instead of evaporating that water (1.13).
 * It no longer needs random ticks, which in 1.12 were only used for the evaporation.
 */
@Mixin(BlockMagma.class)
public abstract class MixinBlockMagma extends Block {

    private MixinBlockMagma(Material material) {
        super(material);
    }

    /** Asked as chunks load, after registration; without bubble columns magma ticks randomly as in 1.12. */
    @Override
    public boolean getTickRandomly() {
        return PortiumBlocks.BUBBLE_COLUMN == null;
    }

    @Inject(method = "updateTick", at = @At("HEAD"), cancellable = true)
    private void portium$placeBubbleColumn(World world, BlockPos pos, IBlockState state, Random rand, CallbackInfo ci) {
        if (PortiumBlocks.BUBBLE_COLUMN == null) return;
        BlockBubbleColumn.placeBubbleColumn(world, pos.up(), true);
        ci.cancel();
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
}
