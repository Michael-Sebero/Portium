package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.block.water.WaterUtil;
import net.minecraft.block.BlockMycelium;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * Mycelium covered by water turns into dirt and doesn't spread under water (1.16). Needed because 1.13 water lets
 * more light through, so 1.12's darkness rule no longer kills it.
 */
@Mixin(BlockMycelium.class)
public abstract class MixinBlockMycelium {

    @Inject(method = "updateTick", at = @At("HEAD"), cancellable = true)
    private void portium$dieUnderWater(World world, BlockPos pos, IBlockState state, Random rand, CallbackInfo ci) {
        if (!world.isRemote && PortiumConfig.mechanics.underwaterGrassToDirt && WaterUtil.isWater(world.getBlockState(pos.up()))) {
            world.setBlockState(pos, Blocks.DIRT.getDefaultState());
            ci.cancel();
        }
    }

    @Redirect(method = "updateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)Z"))
    private boolean portium$dontSpreadUnderWater(World world, BlockPos pos, IBlockState newState) {
        boolean spreading = newState.getBlock() == (Object) this;
        if (spreading && PortiumConfig.mechanics.underwaterGrassToDirt && WaterUtil.isWater(world.getBlockState(pos.up()))) return false;
        return world.setBlockState(pos, newState);
    }
}
