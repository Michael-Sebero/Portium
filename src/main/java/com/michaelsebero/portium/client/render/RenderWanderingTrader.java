package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelVillager116;
import com.michaelsebero.portium.entity.merchant.EntityWanderingTrader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** 1.14's wandering trader renderer: the 1.14 villager model, a head item, and what it holds in its crossed arms. */
public class RenderWanderingTrader extends RenderLiving<EntityWanderingTrader> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/entity/wandering_trader.png");

    public RenderWanderingTrader(RenderManager manager) {
        super(manager, new ModelVillager116(0.0F), 0.5F);
        this.addLayer(new LayerCustomHead(((ModelVillager116) this.mainModel).villagerHead));
        this.addLayer(new LayerCrossedArmsItem());
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityWanderingTrader trader) {
        return TEXTURE;
    }

    @Override
    protected void preRenderCallback(EntityWanderingTrader trader, float partialTicks) {
        GlStateManager.scale(0.9375F, 0.9375F, 0.9375F);
    }

    /** 1.14's {@code CrossedArmsItemLayer}: the main hand item, held in the crossed arms. */
    public static class LayerCrossedArmsItem implements LayerRenderer<EntityLivingBase> {

        @Override
        public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scale) {
            ItemStack stack = entity.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
            if (stack.isEmpty()) return;
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, 0.4F, -0.4F);
            GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
            Minecraft.getMinecraft().getItemRenderer().renderItem(entity, stack, ItemCameraTransforms.TransformType.GROUND);
            GlStateManager.popMatrix();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
