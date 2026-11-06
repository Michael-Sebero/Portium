package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.PortiumConfig;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockWall;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Glass panes and iron bars join walls in 1.16, as walls join them (MixinBlockWall116). */
@Mixin(BlockPane.class)
public abstract class MixinBlockPaneWalls {

    @Inject(method = "attachesTo", at = @At("RETURN"), cancellable = true)
    private void portium$toWalls(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing facing, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && PortiumConfig.mechanics.walls116 && state.getBlock() instanceof BlockWall) cir.setReturnValue(true);
    }
}
