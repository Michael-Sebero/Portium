package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Flowing water treats a waterlogged neighbour as a source: it keeps the water beside it from drying up (1.13). */
@Mixin(BlockDynamicLiquid.class)
public abstract class MixinBlockDynamicLiquid extends BlockLiquid {

    private MixinBlockDynamicLiquid(Material material) {
        super(material);
    }

    /** A waterlogged cell already holds water, so flowing water or lava never washes its block away. */
    @Inject(method = "isBlocked", at = @At("HEAD"), cancellable = true)
    private void portium$waterloggedIsBlocked(World world, BlockPos pos, IBlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (Waterlogging.isWaterlogged(world, pos)) cir.setReturnValue(true);
    }

    @Redirect(method = "checkAdjacentBlock",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockDynamicLiquid;getDepth(Lnet/minecraft/block/state/IBlockState;)I"))
    private int portium$waterloggedNeighborIsSource(BlockDynamicLiquid self, IBlockState state, World world, BlockPos pos, int currentMinLevel) {
        return this.material == Material.WATER && Waterlogging.isWaterlogged(world, pos) ? 0 : this.getDepth(state);
    }
}
