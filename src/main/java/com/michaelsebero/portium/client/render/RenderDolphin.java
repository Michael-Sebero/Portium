package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelDolphin;
import com.michaelsebero.portium.entity.passive.EntityDolphin;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nonnull;

public class RenderDolphin extends RenderLiving<EntityDolphin> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/entity/dolphin.png");

    public RenderDolphin(RenderManager manager) {
        super(manager, new ModelDolphin(), 0.7F);
        this.addLayer(new CarriedItemLayer());
    }

    @Nonnull
    @Override
    protected ResourceLocation getEntityTexture(EntityDolphin dolphin) {
        return TEXTURE;
    }

    /** The item a dolphin is playing with, held at the tip of its nose. */
    private static class CarriedItemLayer implements LayerRenderer<EntityDolphin> {

        @Override
        public void doRenderLayer(EntityDolphin dolphin, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            boolean rightHanded = dolphin.getPrimaryHand() == EnumHandSide.RIGHT;
            ItemStack stack = rightHanded ? dolphin.getHeldItemMainhand() : dolphin.getHeldItemOffhand();
            if (stack.isEmpty()) return;
            boolean translucent = Minecraft.getMinecraft().getRenderItem().shouldRenderItemIn3D(stack)
                    && Block.getBlockFromItem(stack.getItem()).getRenderLayer() == BlockRenderLayer.TRANSLUCENT;
            GlStateManager.pushMatrix();
            if (translucent) GlStateManager.depthMask(false);
            float tilt = MathHelper.abs(dolphin.rotationPitch) / 60.0F;
            if (dolphin.rotationPitch < 0.0F) GlStateManager.translate(0.0F, 1.0F - tilt * 0.5F, -1.0F + tilt * 0.5F);
            else GlStateManager.translate(0.0F, 1.0F + tilt * 0.8F, -1.0F + tilt * 0.2F);
            Minecraft.getMinecraft().getItemRenderer().renderItem(dolphin, stack, ItemCameraTransforms.TransformType.GROUND);
            if (translucent) GlStateManager.depthMask(true);
            GlStateManager.popMatrix();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
