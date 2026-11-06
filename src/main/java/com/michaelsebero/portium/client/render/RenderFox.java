package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelFox;
import com.michaelsebero.portium.entity.passive.EntityFox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** The fox (1.14), red or snow, awake or asleep, tipped along its leap, with whatever it carries in its mouth. */
public class RenderFox extends RenderLiving<EntityFox> {

    private static final ResourceLocation FOX = texture("fox");
    private static final ResourceLocation SLEEPING_FOX = texture("fox_sleep");
    private static final ResourceLocation SNOW_FOX = texture("snow_fox");
    private static final ResourceLocation SLEEPING_SNOW_FOX = texture("snow_fox_sleep");

    public RenderFox(RenderManager manager) {
        super(manager, new ModelFox(), 0.4F);
        this.addLayer(new HeldItemLayer());
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(Tags.MOD_ID, "textures/entity/fox/" + name + ".png");
    }

    @Override
    protected void applyRotations(EntityFox fox, float ageInTicks, float rotationYaw, float partialTicks) {
        super.applyRotations(fox, ageInTicks, rotationYaw, partialTicks);
        if (fox.isPouncing() || fox.isStuck()) {
            float pitch = -(fox.prevRotationPitch + (fox.rotationPitch - fox.prevRotationPitch) * partialTicks);
            GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityFox fox) {
        if (fox.getVariantType() == EntityFox.Type.RED) return fox.isSleeping() ? SLEEPING_FOX : FOX;
        return fox.isSleeping() ? SLEEPING_SNOW_FOX : SNOW_FOX;
    }

    /** What the fox holds, in its mouth. */
    private class HeldItemLayer implements LayerRenderer<EntityFox> {

        @Override
        public void doRenderLayer(EntityFox fox, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scale) {
            ItemStack stack = fox.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
            if (stack.isEmpty()) return;
            ModelFox model = (ModelFox) RenderFox.this.getMainModel();
            boolean sleeping = fox.isSleeping();
            boolean child = fox.isChild();
            GlStateManager.pushMatrix();
            if (child) {
                GlStateManager.scale(0.75F, 0.75F, 0.75F);
                GlStateManager.translate(0.0D, 0.5D, 0.20937499403953552D);
            }
            GlStateManager.translate(model.head.rotationPointX / 16.0F, model.head.rotationPointY / 16.0F, model.head.rotationPointZ / 16.0F);
            GlStateManager.rotate((float) Math.toDegrees(fox.getInterestedAngle(partialTicks)), 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(netHeadYaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(headPitch, 1.0F, 0.0F, 0.0F);
            if (child) {
                if (sleeping) GlStateManager.translate(0.4000000059604645D, 0.25999999046325684D, 0.15000000596046448D);
                else GlStateManager.translate(0.05999999865889549D, 0.25999999046325684D, -0.5D);
            } else if (sleeping) {
                GlStateManager.translate(0.46000000834465027D, 0.25999999046325684D, 0.2199999988079071D);
            } else {
                GlStateManager.translate(0.05999999865889549D, 0.27000001072883606D, -0.5D);
            }
            GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
            if (sleeping) GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
            Minecraft.getMinecraft().getItemRenderer().renderItemSide(fox, stack, ItemCameraTransforms.TransformType.GROUND, false);
            GlStateManager.popMatrix();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
