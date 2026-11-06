package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelCat;
import com.michaelsebero.portium.entity.passive.EntityCat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

/** The cat (1.14), four-fifths size, rolled onto its side when lying down, with its collar. */
public class RenderCat extends RenderLiving<EntityCat> {

    private static final ResourceLocation COLLAR = new ResourceLocation(Tags.MOD_ID, "textures/entity/cat/cat_collar.png");

    public RenderCat(RenderManager manager) {
        super(manager, new ModelCat(0.0F), 0.4F);
        this.addLayer(new CollarLayer());
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityCat cat) {
        return cat.getCatTexture();
    }

    @Override
    protected void preRenderCallback(EntityCat cat, float partialTicks) {
        GlStateManager.scale(0.8F, 0.8F, 0.8F);
    }

    @Override
    protected void applyRotations(EntityCat cat, float ageInTicks, float rotationYaw, float partialTicks) {
        super.applyRotations(cat, ageInTicks, rotationYaw, partialTicks);
        float lie = cat.getLieDownAmount(partialTicks);
        if (lie <= 0.0F) return;
        GlStateManager.translate(0.4F * lie, 0.15F * lie, 0.1F * lie);
        GlStateManager.rotate(lie * 90.0F, 0.0F, 0.0F, 1.0F);
        BlockPos pos = new BlockPos(cat);
        for (EntityPlayer player : cat.world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(pos).grow(2.0D, 2.0D, 2.0D))) {
            if (player.isPlayerSleeping()) {
                GlStateManager.translate(0.15F * lie, 0.0F, 0.0F);
                break;
            }
        }
    }

    /** A tame cat's collar in its dye colour. */
    private class CollarLayer implements LayerRenderer<EntityCat> {
        private final ModelCat model = new ModelCat(0.01F);

        @Override
        public void doRenderLayer(EntityCat cat, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scale) {
            if (!cat.isTamed() || cat.isInvisible()) return;
            RenderCat.this.bindTexture(COLLAR);
            float[] colour = cat.getCollarColor().getColorComponentValues();
            GlStateManager.color(colour[0], colour[1], colour[2]);
            this.model.setModelAttributes(RenderCat.this.getMainModel());
            this.model.setLivingAnimations(cat, limbSwing, limbSwingAmount, partialTicks);
            this.model.render(cat, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            GlStateManager.color(1.0F, 1.0F, 1.0F);
        }

        @Override
        public boolean shouldCombineTextures() {
            return true;
        }
    }
}
