package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Water doesn't draw a face against the water of a waterlogged neighbour, as with any water; and a surface's underside
 * follows 1.16's rule.
 */
@Mixin(BlockLiquid.class)
public abstract class MixinBlockLiquidSides extends Block {

    private MixinBlockLiquidSides(Material material) {
        super(material);
    }

    @Inject(method = "shouldSideBeRendered", at = @At("HEAD"), cancellable = true)
    private void portium$noFaceAgainstWaterlogged(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side, CallbackInfoReturnable<Boolean> cir) {
        if (this.material == Material.WATER && Waterlogging.isWaterlogged(world, pos.offset(side))) cir.setReturnValue(false);
    }

    /**
     * 1.16's {@code FluidState.shouldRenderSides}: whether the surface's underside is drawn (seen from below), when a
     * block beside the one above is neither the same fluid nor opaque. A waterlogged neighbour counts as water, so
     * under a hull of planks and waterlogged stairs nothing is drawn; 1.12 counted those stairs as gaps.
     */
    @Inject(method = "shouldRenderSides", at = @At("HEAD"), cancellable = true)
    private void portium$sides116(IBlockAccess world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                BlockPos at = pos.add(dx, 0, dz);
                IBlockState state = world.getBlockState(at);
                boolean sameFluid = state.getMaterial() == this.material || this.material == Material.WATER && Waterlogging.isWaterlogged(world, at);
                if (!sameFluid && !state.isOpaqueCube()) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
        cir.setReturnValue(false);
    }
}
