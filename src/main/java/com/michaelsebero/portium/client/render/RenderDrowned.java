package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelDrowned;
import com.michaelsebero.portium.entity.monster.EntityDrowned;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

/** The drowned (1.13), with its seaweed-draped outer layer, tilting forward while it swims. */
public class RenderDrowned extends RenderBiped<EntityDrowned> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/entity/zombie/drowned.png");
    private static final ResourceLocation OUTER_LAYER = new ResourceLocation(Tags.MOD_ID, "textures/entity/zombie/drowned_outer_layer.png");

    public RenderDrowned(RenderManager manager) {
        super(manager, new ModelDrowned(0.0F), 0.5F);
        this.addLayer(new LayerBipedArmor(this) {
            @Override
            protected void initArmor() {
                this.modelLeggings = new ModelZombie(0.5F, true);
                this.modelArmor = new ModelZombie(1.0F, true);
            }
        });
        this.addLayer(new OuterLayer());
    }

    @Override
    protected void applyRotations(EntityDrowned drowned, float ageInTicks, float rotationYaw, float partialTicks) {
        float swim = drowned.getSwimAnimation(partialTicks);
        super.applyRotations(drowned, ageInTicks, rotationYaw, partialTicks);
        if (swim > 0.0F) {
            GlStateManager.rotate(drowned.rotationPitch + swim * (-10.0F - drowned.rotationPitch - drowned.rotationPitch), 1.0F, 0.0F, 0.0F);
        }
    }

    @Nonnull
    @Override
    protected ResourceLocation getEntityTexture(EntityDrowned drowned) {
        return TEXTURE;
    }

    private class OuterLayer implements LayerRenderer<EntityDrowned> {

        private final ModelDrowned model = new ModelDrowned(0.25F);

        @Override
        public void doRenderLayer(EntityDrowned drowned, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            if (drowned.isInvisible()) return;
            this.model.setModelAttributes(RenderDrowned.this.getMainModel());
            this.model.setLivingAnimations(drowned, limbSwing, limbSwingAmount, partialTicks);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            RenderDrowned.this.bindTexture(OUTER_LAYER);
            this.model.render(drowned, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }

        @Override
        public boolean shouldCombineTextures() {
            return true;
        }
    }
}
