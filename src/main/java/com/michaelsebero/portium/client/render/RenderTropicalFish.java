package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelTropicalFish;
import com.michaelsebero.portium.entity.fish.EntityTropicalFish;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nonnull;

/** Tropical fish (1.13): the body tinted with the body colour and a pattern layer tinted with the pattern colour. */
public class RenderTropicalFish extends RenderLiving<EntityTropicalFish> {

    private static final ResourceLocation[] BODY = {texture("tropical_a"), texture("tropical_b")};
    private static final ResourceLocation[][] PATTERNS = new ResourceLocation[2][6];

    static {
        for (int i = 0; i < 6; i++) {
            PATTERNS[0][i] = texture("tropical_a_pattern_" + (i + 1));
            PATTERNS[1][i] = texture("tropical_b_pattern_" + (i + 1));
        }
    }

    private final ModelTropicalFish[] shapes = {ModelTropicalFish.shapeA(0.0F), ModelTropicalFish.shapeB(0.0F)};

    public RenderTropicalFish(RenderManager manager) {
        super(manager, ModelTropicalFish.shapeA(0.0F), 0.15F);
        this.addLayer(new PatternLayer());
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(Tags.MOD_ID, "textures/entity/fish/" + name + ".png");
    }

    @Override
    public void doRender(EntityTropicalFish fish, double x, double y, double z, float entityYaw, float partialTicks) {
        int variant = fish.getVariant();
        this.mainModel = this.shapes[EntityTropicalFish.shape(variant)];
        float[] color = EntityTropicalFish.bodyColor(variant).getColorComponentValues();
        GlStateManager.color(color[0], color[1], color[2]);
        super.doRender(fish, x, y, z, entityYaw, partialTicks);
        GlStateManager.color(1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void applyRotations(EntityTropicalFish fish, float ageInTicks, float rotationYaw, float partialTicks) {
        super.applyRotations(fish, ageInTicks, rotationYaw, partialTicks);
        GlStateManager.rotate(4.3F * MathHelper.sin(0.6F * ageInTicks), 0.0F, 1.0F, 0.0F);
        if (!fish.isInWater()) {
            GlStateManager.translate(0.2F, 0.1F, 0.0F);
            GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
        }
    }

    @Nonnull
    @Override
    protected ResourceLocation getEntityTexture(EntityTropicalFish fish) {
        return BODY[EntityTropicalFish.shape(fish.getVariant())];
    }

    private class PatternLayer implements LayerRenderer<EntityTropicalFish> {

        private final ModelTropicalFish[] shapes = {ModelTropicalFish.shapeA(0.008F), ModelTropicalFish.shapeB(0.008F)};

        @Override
        public void doRenderLayer(EntityTropicalFish fish, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            if (fish.isInvisible()) return;
            int variant = fish.getVariant();
            int shape = EntityTropicalFish.shape(variant);
            ModelTropicalFish model = this.shapes[shape];
            RenderTropicalFish.this.bindTexture(PATTERNS[shape][EntityTropicalFish.patternIndex(variant)]);
            float[] color = EntityTropicalFish.patternColor(variant).getColorComponentValues();
            GlStateManager.color(color[0], color[1], color[2]);
            model.setModelAttributes(RenderTropicalFish.this.getMainModel());
            model.setLivingAnimations(fish, limbSwing, limbSwingAmount, partialTicks);
            model.render(fish, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            GlStateManager.color(1.0F, 1.0F, 1.0F);
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
