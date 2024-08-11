package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.client.RenderLayers116;
import net.minecraft.block.Block;
import net.minecraft.util.BlockRenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A 1.16 mod's block draws in the layer the mod set for it (see {@link RenderLayers116}). */
@Mixin(Block.class)
public abstract class MixinBlockRenderLayer {

    @Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true)
    private void portium$modLayer(CallbackInfoReturnable<BlockRenderLayer> cir) {
        BlockRenderLayer layer = RenderLayers116.get((Block) (Object) this);
        if (layer != null) cir.setReturnValue(layer);
    }
}
