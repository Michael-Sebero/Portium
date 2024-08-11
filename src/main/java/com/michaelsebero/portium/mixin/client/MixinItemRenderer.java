package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.PortiumConfig;
import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** The underwater screen overlay is much fainter in 1.13. */
@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

    @ModifyConstant(method = "renderWaterOverlayTexture", constant = @Constant(floatValue = 0.5F))
    private float portium$fainterUnderwaterOverlay(float alpha) {
        return PortiumConfig.visuals.fainterUnderwaterOverlay ? 0.1F : alpha;
    }
}
