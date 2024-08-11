package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelTrident;
import com.michaelsebero.portium.entity.EntityTrident;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public class RenderTrident extends Render<EntityTrident> {

    public static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/entity/trident.png");
    private final ModelTrident model = new ModelTrident();

    public RenderTrident(RenderManager manager) {
        super(manager);
    }

    @Override
    public void doRender(EntityTrident entity, double x, double y, double z, float entityYaw, float partialTicks) {
        this.bindEntityTexture(entity);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks - 90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks + 90.0F, 0.0F, 0.0F, 1.0F);
        this.model.render();
        if (entity.hasFoil()) GlintRenderer.render(this.model::render);
        GlStateManager.popMatrix();
        GlStateManager.enableLighting();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Nonnull
    @Override
    protected ResourceLocation getEntityTexture(EntityTrident entity) {
        return TEXTURE;
    }
}
