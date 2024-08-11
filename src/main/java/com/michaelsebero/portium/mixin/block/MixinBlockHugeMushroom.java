package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHugeMushroom;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * Vanilla's generated huge mushrooms give Portium's six-faced mushroom blocks when silk-touched or picked,
 * and stems drop nothing (1.13).
 */
@Mixin(BlockHugeMushroom.class)
public abstract class MixinBlockHugeMushroom extends Block {

    private MixinBlockHugeMushroom(Material material) {
        super(material);
    }

    private Block portium$replacement(IBlockState state) {
        BlockHugeMushroom.EnumType variant = state.getValue(BlockHugeMushroom.VARIANT);
        if (variant == BlockHugeMushroom.EnumType.STEM || variant == BlockHugeMushroom.EnumType.ALL_STEM) return PortiumBlocks.MUSHROOM_STEM;
        return (Object) this == Blocks.RED_MUSHROOM_BLOCK ? PortiumBlocks.RED_MUSHROOM_BLOCK : PortiumBlocks.BROWN_MUSHROOM_BLOCK;
    }

    // Without Portium's mushroom blocks, vanilla's drop and pick as in 1.12.

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return PortiumBlocks.MUSHROOM_STEM != null || super.canSilkHarvest(world, pos, state, player);
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return PortiumBlocks.MUSHROOM_STEM == null ? super.getSilkTouchDrop(state) : new ItemStack(this.portium$replacement(state));
    }

    @Inject(method = "getItem", at = @At("HEAD"), cancellable = true)
    private void portium$pickNewBlock(World world, BlockPos pos, IBlockState state, CallbackInfoReturnable<ItemStack> cir) {
        if (PortiumBlocks.MUSHROOM_STEM != null) cir.setReturnValue(new ItemStack(this.portium$replacement(state)));
    }

    @Inject(method = "getItemDropped", at = @At("HEAD"), cancellable = true)
    private void portium$stemsDropNothing(IBlockState state, Random rand, int fortune, CallbackInfoReturnable<Item> cir) {
        if (PortiumBlocks.MUSHROOM_STEM != null && this.portium$replacement(state) == PortiumBlocks.MUSHROOM_STEM) cir.setReturnValue(Items.AIR);
    }
}
