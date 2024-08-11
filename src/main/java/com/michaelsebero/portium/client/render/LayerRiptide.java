package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.api.IRiptideEntity;
import com.michaelsebero.portium.client.model.ModelRiptide;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

/** The spinning water vortex drawn around players using Riptide (1.13). */
public class LayerRiptide implements LayerRenderer<EntityLivingBase> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/entity/trident_riptide.png");
    private final RenderLivingBase<?> renderer;
    private final ModelRiptide model = new ModelRiptide();

    public LayerRiptide(RenderLivingBase<?> renderer) {
        this.renderer = renderer;
    }

    @Override
    public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        if (!IRiptideEntity.of(entity).portium$isSpinAttacking()) return;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.renderer.bindTexture(TEXTURE);
        for (int i = 0; i < 3; ++i) {
            GlStateManager.pushMatrix();
            GlStateManager.rotate(ageInTicks * -(45 + i * 5), 0.0F, 1.0F, 0.0F);
            float size = 0.75F * i;
            GlStateManager.scale(size, size, size);
            GlStateManager.translate(0.0F, -0.2F + 0.6F * i, 0.0F);
            this.model.render(scale);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
