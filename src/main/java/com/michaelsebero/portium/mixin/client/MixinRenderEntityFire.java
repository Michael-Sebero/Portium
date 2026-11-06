package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.client.FireSprites;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Burning entities' fire draws a modern pack's fire sprites when it has them (see FireSprites). */
@Mixin(Render.class)
public abstract class MixinRenderEntityFire {

    @Redirect(method = "renderEntityOnFire", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureMap;getAtlasSprite(Ljava/lang/String;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"))
    private TextureAtlasSprite portium$fireSprite(TextureMap map, String name) {
        return FireSprites.sprite(map, name);
    }
}
