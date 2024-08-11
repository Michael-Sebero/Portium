package com.michaelsebero.portium.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/** Draws the enchantment shimmer over a model, as vanilla does for armour (1.12 has no item-entity glint helper). */
public final class GlintRenderer {

    private static final ResourceLocation GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");

    public static void render(Runnable model) {
        Minecraft mc = Minecraft.getMinecraft();
        float ticks = (Minecraft.getSystemTime() % 1000000L) / 50.0F;
        mc.getTextureManager().bindTexture(GLINT);
        GlStateManager.enableBlend();
        GlStateManager.depthFunc(514);
        GlStateManager.depthMask(false);
        GlStateManager.disableLighting();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE);
        GlStateManager.color(0.38F, 0.19F, 0.608F, 1.0F);
        for (int pass = 0; pass < 2; ++pass) {
            GlStateManager.matrixMode(5890);
            GlStateManager.loadIdentity();
            GlStateManager.scale(0.33333334F, 0.33333334F, 0.33333334F);
            GlStateManager.rotate(30.0F - pass * 60.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.translate(0.0F, ticks * (0.001F + pass * 0.003F) * 20.0F, 0.0F);
            GlStateManager.matrixMode(5888);
            model.run();
        }
        GlStateManager.matrixMode(5890);
        GlStateManager.loadIdentity();
        GlStateManager.matrixMode(5888);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(515);
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private GlintRenderer() {}
}
