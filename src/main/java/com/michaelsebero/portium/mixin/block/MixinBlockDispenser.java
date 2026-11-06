package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.PortiumConfig;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dispensers and droppers keep their facing when set (1.13): 1.12 turns one placed facing a solid block around when
 * the block behind is open, which turned {@code /setblock}'s and structures' dispensers away from what they face.
 */
@Mixin(BlockDispenser.class)
public abstract class MixinBlockDispenser {

    @Inject(method = "setDefaultDirection", at = @At("HEAD"), cancellable = true)
    private void portium$keepFacing(World world, BlockPos pos, IBlockState state, CallbackInfo ci) {
        if (PortiumConfig.mechanics.dispenserFacing113) ci.cancel();
    }
}
