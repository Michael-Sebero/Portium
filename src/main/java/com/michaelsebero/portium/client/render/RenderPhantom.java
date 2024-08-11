package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelPhantom;
import com.michaelsebero.portium.entity.monster.EntityPhantom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

/** The phantom (1.13): scaled by its size, pitched with its flight, with glowing eyes. */
public class RenderPhantom extends RenderLiving<EntityPhantom> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/entity/phantom.png");
    private static final ResourceLocation EYES = new ResourceLocation(Tags.MOD_ID, "textures/entity/phantom_eyes.png");

    public RenderPhantom(RenderManager manager) {
        super(manager, new ModelPhantom(), 0.75F);
        this.addLayer(new EyesLayer());
    }

    @Override
    protected void preRenderCallback(EntityPhantom phantom, float partialTicks) {
        float scale = 1.0F + 0.15F * phantom.getPhantomSize();
        GlStateManager.scale(scale, scale, scale);
        GlStateManager.translate(0.0F, 1.3125F, 0.1875F);
    }

    @Override
    protected void applyRotations(EntityPhantom phantom, float ageInTicks, float rotationYaw, float partialTicks) {
        super.applyRotations(phantom, ageInTicks, rotationYaw, partialTicks);
        GlStateManager.rotate(phantom.rotationPitch, 1.0F, 0.0F, 0.0F);
    }

    @Nonnull
    @Override
    protected ResourceLocation getEntityTexture(EntityPhantom phantom) {
        return TEXTURE;
    }

    private class EyesLayer implements LayerRenderer<EntityPhantom> {

        @Override
        public void doRenderLayer(EntityPhantom phantom, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            RenderPhantom.this.bindTexture(EYES);
            GlStateManager.enableBlend();
            GlStateManager.disableAlpha();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            GlStateManager.depthMask(!phantom.isInvisible());
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 61680 % 65536, 61680 / 65536);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Minecraft.getMinecraft().entityRenderer.setupFogColor(true);
            RenderPhantom.this.getMainModel().render(phantom, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            Minecraft.getMinecraft().entityRenderer.setupFogColor(false);
            int light = phantom.getBrightnessForRender();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
            RenderPhantom.this.setLightmap(phantom);
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
