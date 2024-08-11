package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWall;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Walls don't attach to the uncarved pumpkin, just like they don't attach to vanilla pumpkins. */
@Mixin(BlockWall.class)
public abstract class MixinBlockWallAttach {

    @Inject(method = "isExcepBlockForAttachWithPiston", at = @At("HEAD"), cancellable = true)
    private static void portium$excludeUncarvedPumpkin(Block block, CallbackInfoReturnable<Boolean> cir) {
        if (block == PortiumBlocks.PUMPKIN) cir.setReturnValue(true);
    }
}
