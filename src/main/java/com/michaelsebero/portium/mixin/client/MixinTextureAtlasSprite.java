package com.michaelsebero.portium.mixin.client;

import net.minecraft.client.renderer.texture.PngSizeInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A texture that isn't square and isn't animated is one frame of its own size, as from 1.13 on (newer mods' scrolling
 * belt textures are long strips); 1.12 refused it as a broken animation.
 */
@Mixin(TextureAtlasSprite.class)
public abstract class MixinTextureAtlasSprite {

    @Shadow protected int width;
    @Shadow protected int height;

    @Shadow
    protected abstract void resetSprite();

    @Inject(method = "loadSprite", at = @At("HEAD"), cancellable = true)
    private void portium$anyShape(PngSizeInfo size, boolean animated, CallbackInfo ci) {
        if (animated || size.pngWidth == size.pngHeight) return;
        resetSprite();
        this.width = size.pngWidth;
        this.height = size.pngHeight;
        ci.cancel();
    }
}
