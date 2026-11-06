package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.client.BannerLayers;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.LayeredColorMaskTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 * Banners and shields painted from pattern textures drawn the 1.14+ way as well as the 1.12 way, in any PNG format and
 * size ({@link BannerLayers}). 1.12 painted only 32-bit RGBA layers with the shape in their red channel, so 1.16.5's
 * patterns (grayscale, the shape in their alpha) left banners plain.
 */
@Mixin(LayeredColorMaskTexture.class)
public abstract class MixinLayeredColorMaskTexture extends AbstractTexture {

    @Shadow @Final private ResourceLocation textureLocation;
    @Shadow @Final private List<String> listTextures;
    @Shadow @Final private List<EnumDyeColor> listDyeColors;

    @Inject(method = "loadTexture", at = @At("HEAD"), cancellable = true)
    private void portium$paintLayers(IResourceManager resources, CallbackInfo ci) throws IOException {
        ci.cancel();
        this.deleteGlTexture();
        BufferedImage painted;
        try (IResource cloth = resources.getResource(this.textureLocation)) {
            List<String> textures = this.listTextures;
            List<EnumDyeColor> colours = this.listDyeColors;
            painted = BannerLayers.paint(TextureUtil.readBufferedImage(cloth.getInputStream()), new BannerLayers.LayerSource() {
                @Override
                public BufferedImage layer(int index) throws IOException {
                    String texture = textures.get(index);
                    return texture == null ? null : MinecraftForgeClient.getImageLayer(new ResourceLocation(texture), resources);
                }

                @Override
                public EnumDyeColor colour(int index) {
                    return colours.get(index);
                }
            }, Math.min(textures.size(), colours.size()));
        } catch (IOException e) {
            LogManager.getLogger("Portium").error("Couldn't paint banner texture {}", this.textureLocation, e);
            return;
        }
        TextureUtil.uploadTextureImage(this.getGlTextureId(), painted);
    }
}
