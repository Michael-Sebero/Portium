package com.michaelsebero.portium.client.render;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.client.model.ModelPanda;
import com.michaelsebero.portium.entity.passive.EntityPanda;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import java.util.EnumMap;
import java.util.Map;

/** The panda (1.14), coat by its gene, turned over as it sits, lies back or somersaults, with its bamboo in its paws. */
public class RenderPanda extends RenderLiving<EntityPanda> {

    private static final Map<EntityPanda.Gene, ResourceLocation> TEXTURES = new EnumMap<>(EntityPanda.Gene.class);

    static {
        TEXTURES.put(EntityPanda.Gene.NORMAL, texture("panda"));
        TEXTURES.put(EntityPanda.Gene.LAZY, texture("lazy_panda"));
        TEXTURES.put(EntityPanda.Gene.WORRIED, texture("worried_panda"));
        TEXTURES.put(EntityPanda.Gene.PLAYFUL, texture("playful_panda"));
        TEXTURES.put(EntityPanda.Gene.BROWN, texture("brown_panda"));
        TEXTURES.put(EntityPanda.Gene.WEAK, texture("weak_panda"));
        TEXTURES.put(EntityPanda.Gene.AGGRESSIVE, texture("aggressive_panda"));
    }

    public RenderPanda(RenderManager manager) {
        super(manager, new ModelPanda(), 0.9F);
        this.addLayer(new HeldItemLayer());
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(Tags.MOD_ID, "textures/entity/panda/" + name + ".png");
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityPanda panda) {
        return TEXTURES.getOrDefault(panda.getVisibleGene(), TEXTURES.get(EntityPanda.Gene.NORMAL));
    }

    @Override
    protected void applyRotations(EntityPanda panda, float ageInTicks, float rotationYaw, float partialTicks) {
        super.applyRotations(panda, ageInTicks, rotationYaw, partialTicks);
        if (panda.rollCounter > 0) {
            int tick = panda.rollCounter;
            int next = tick + 1;
            float height = panda.isChild() ? 0.3F : 0.8F;
            if (tick < 8) {
                float angle = rollAngle((float) (90 * tick) / 7.0F, (float) (90 * next) / 7.0F, next, partialTicks, 8.0F);
                GlStateManager.translate(0.0F, (height + 0.2F) * (angle / 90.0F), 0.0F);
                GlStateManager.rotate(-angle, 1.0F, 0.0F, 0.0F);
            } else if (tick < 16) {
                float progress = ((float) tick - 8.0F) / 7.0F;
                float angle = rollAngle(90.0F + 90.0F * progress, 90.0F + 90.0F * ((float) next - 8.0F) / 7.0F, next, partialTicks, 16.0F);
                GlStateManager.translate(0.0F, height + 0.2F + (height - 0.2F) * (angle - 90.0F) / 90.0F, 0.0F);
                GlStateManager.rotate(-angle, 1.0F, 0.0F, 0.0F);
            } else if ((float) tick < 24.0F) {
                float progress = ((float) tick - 16.0F) / 7.0F;
                float angle = rollAngle(180.0F + 90.0F * progress, 180.0F + 90.0F * ((float) next - 16.0F) / 7.0F, next, partialTicks, 24.0F);
                GlStateManager.translate(0.0F, height + height * (270.0F - angle) / 90.0F, 0.0F);
                GlStateManager.rotate(-angle, 1.0F, 0.0F, 0.0F);
            } else if (tick < 32) {
                float progress = ((float) tick - 24.0F) / 7.0F;
                float angle = rollAngle(270.0F + 90.0F * progress, 270.0F + 90.0F * ((float) next - 24.0F) / 7.0F, next, partialTicks, 32.0F);
                GlStateManager.translate(0.0F, height * ((360.0F - angle) / 90.0F), 0.0F);
                GlStateManager.rotate(-angle, 1.0F, 0.0F, 0.0F);
            }
        }
        float sit = panda.getSitAmount(partialTicks);
        if (sit > 0.0F) {
            GlStateManager.translate(0.0F, 0.8F * sit, 0.0F);
            GlStateManager.rotate(lerp(sit, panda.rotationPitch, panda.rotationPitch + 90.0F), 1.0F, 0.0F, 0.0F);
            GlStateManager.translate(0.0F, -1.0F * sit, 0.0F);
            if (panda.isScared()) {
                float shiver = (float) (Math.cos((double) panda.ticksExisted * 1.25D) * Math.PI * 0.05000000074505806D);
                GlStateManager.rotate(shiver, 0.0F, 1.0F, 0.0F);
                if (panda.isChild()) GlStateManager.translate(0.0F, 0.8F, 0.55F);
            }
        }
        float back = panda.getOnBackAmount(partialTicks);
        if (back > 0.0F) {
            float height = panda.isChild() ? 0.5F : 1.3F;
            GlStateManager.translate(0.0F, height * back, 0.0F);
            GlStateManager.rotate(lerp(back, panda.rotationPitch, panda.rotationPitch + 180.0F), 1.0F, 0.0F, 0.0F);
        }
    }

    private static float lerp(float amount, float from, float to) {
        return from + amount * (to - from);
    }

    private static float rollAngle(float from, float to, int next, float partialTicks, float limit) {
        return (float) next < limit ? lerp(partialTicks, from, to) : from;
    }

    /** Bamboo (or cake) held up to its mouth while it sits, bobbing as it eats. */
    private class HeldItemLayer implements LayerRenderer<EntityPanda> {

        @Override
        public void doRenderLayer(EntityPanda panda, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
                                  float headPitch, float scale) {
            ItemStack stack = panda.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
            if (stack.isEmpty() || !panda.isSitting() || panda.isScared()) return;
            float z = -0.6F;
            float y = 1.4F;
            if (panda.isEating()) {
                z -= 0.2F * MathHelper.sin(ageInTicks * 0.6F) + 0.2F;
                y -= 0.09F * MathHelper.sin(ageInTicks * 0.6F);
            }
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.10000000149011612D, y, z);
            Minecraft.getMinecraft().getItemRenderer().renderItemSide(panda, stack, ItemCameraTransforms.TransformType.GROUND, false);
            GlStateManager.popMatrix();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
