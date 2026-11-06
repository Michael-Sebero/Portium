package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelStrider;
import com.michaelsebero.portium.entity.passive.EntityStrider;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;

/** 1.16's strider renderer: purple and shivering when cold, a saddle layer, half size as a baby. */
public class RenderStrider extends RenderLiving<EntityStrider> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/entity/strider/strider.png");
    private static final ResourceLocation COLD = new ResourceLocation(Tags.MOD_ID, "textures/entity/strider/strider_cold.png");
    private static final ResourceLocation SADDLE = new ResourceLocation(Tags.MOD_ID, "textures/entity/strider/strider_saddle.png");

    public RenderStrider(RenderManager manager) {
        super(manager, new ModelStrider(), 0.5F);
        ModelStrider saddleModel = new ModelStrider();
        this.addLayer(new LayerRenderer<EntityStrider>() {
            @Override
            public void doRenderLayer(EntityStrider strider, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                      float netHeadYaw, float headPitch, float scale) {
                if (!strider.isSaddled()) return;
                RenderStrider.this.bindTexture(SADDLE);
                saddleModel.setModelAttributes(RenderStrider.this.getMainModel());
                saddleModel.render(strider, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            }

            @Override
            public boolean shouldCombineTextures() {
                return false;
            }
        });
    }

    @Override
    protected void preRenderCallback(EntityStrider strider, float partialTicks) {
        if (strider.isChild()) {
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
            this.shadowSize = 0.25F;
        } else {
            this.shadowSize = 0.5F;
        }
    }

    @Override
    protected void applyRotations(EntityStrider strider, float ageInTicks, float rotationYaw, float partialTicks) {
        if (strider.isCold()) rotationYaw += (float) (Math.cos(strider.ticksExisted * 3.25D) * Math.PI * 0.4D);
        super.applyRotations(strider, ageInTicks, rotationYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityStrider strider) {
        return strider.isCold() ? COLD : TEXTURE;
    }
}
