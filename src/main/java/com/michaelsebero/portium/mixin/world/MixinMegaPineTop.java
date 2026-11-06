package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.world.WorldGeneration;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenMegaPineTree;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * 1.16's giant spruce trunk is level at the top, under the crown. 1.12 runs one of the four columns a block higher,
 * poking through the crown's narrow top; in 1.16.5 worlds that log becomes the crown's leaves.
 */
@Mixin(WorldGenMegaPineTree.class)
public abstract class MixinMegaPineTop {

    @Inject(method = "generate", at = @At("RETURN"))
    private void portium$levelTop(World world, Random random, BlockPos position, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || !WorldGeneration.is116(world)) return;
        BlockPos top = position;
        while (world.getBlockState(top.up()).getBlock() instanceof BlockLog) top = top.up();
        if (top.equals(position) || world.getBlockState(top.east()).getBlock() instanceof BlockLog) return;
        // The lone top log becomes the crown's leaves (those beside it, else spruce leaves as the crown's are).
        IBlockState leaves = SPRUCE_LEAVES;
        for (BlockPos near : new BlockPos[] {top.up(), top.west(), top.north(), top.east(), top.south()}) {
            if (world.getBlockState(near).getBlock() instanceof BlockLeaves) {
                leaves = world.getBlockState(near);
                break;
            }
        }
        world.setBlockState(top, leaves, 2);
    }

    private static final IBlockState SPRUCE_LEAVES = net.minecraft.init.Blocks.LEAVES.getDefaultState()
            .withProperty(net.minecraft.block.BlockOldLeaf.VARIANT, net.minecraft.block.BlockPlanks.EnumType.SPRUCE)
            .withProperty(BlockLeaves.CHECK_DECAY, false);
}
