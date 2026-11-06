package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.PortiumConfig;
import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The underwater screen overlay is much fainter in 1.13; the burning overlay draws a modern pack's fire (FireSprites). */
@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

    @ModifyConstant(method = "renderWaterOverlayTexture", constant = @Constant(floatValue = 0.5F))
    private float portium$fainterUnderwaterOverlay(float alpha) {
        return PortiumConfig.visuals.fainterUnderwaterOverlay ? 0.1F : alpha;
    }

    @Redirect(method = "renderFireInFirstPerson", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureMap;getAtlasSprite(Ljava/lang/String;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"))
    private net.minecraft.client.renderer.texture.TextureAtlasSprite portium$fireSprite(net.minecraft.client.renderer.texture.TextureMap map, String name) {
        return com.michaelsebero.portium.client.FireSprites.sprite(map, name);
    }
}
