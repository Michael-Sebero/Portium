package com.michaelsebero.portium.mixin.client;

import net.minecraft.client.renderer.texture.LayeredTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * A layer named {@code <texture>|tint=rrggbb} is the texture multiplied by that colour: 1.14's dyed leather horse
 * armour, drawn into 1.12's layered horse texture.
 */
@Mixin(LayeredTexture.class)
public abstract class MixinLayeredTextureTint {

    @Unique
    private int portium$tint = -1;

    @Redirect(method = "loadTexture", at = @At(value = "NEW", target = "net/minecraft/util/ResourceLocation"))
    private ResourceLocation portium$stripTint(String name) {
        int at = name.indexOf("|tint=");
        this.portium$tint = -1;
        if (at < 0) return new ResourceLocation(name);
        try {
            this.portium$tint = Integer.parseInt(name.substring(at + 6), 16);
        } catch (NumberFormatException ignored) {
        }
        return new ResourceLocation(name.substring(0, at));
    }

    @Redirect(method = "loadTexture", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureUtil;readBufferedImage(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;"))
    private BufferedImage portium$tintLayer(InputStream stream) throws IOException {
        BufferedImage image = TextureUtil.readBufferedImage(stream);
        int tint = this.portium$tint;
        if (tint < 0) return image;
        float r = (tint >> 16 & 255) / 255.0F;
        float g = (tint >> 8 & 255) / 255.0F;
        float b = (tint & 255) / 255.0F;
        BufferedImage tinted = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); ++y) {
            for (int x = 0; x < image.getWidth(); ++x) {
                int argb = image.getRGB(x, y);
                int red = (int) ((argb >> 16 & 255) * r);
                int green = (int) ((argb >> 8 & 255) * g);
                int blue = (int) ((argb & 255) * b);
                tinted.setRGB(x, y, argb & 0xFF000000 | red << 16 | green << 8 | blue);
            }
        }
        return tinted;
    }
}
