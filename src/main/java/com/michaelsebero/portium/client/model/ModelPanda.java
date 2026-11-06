package com.michaelsebero.portium.client.model;

import com.michaelsebero.portium.entity.passive.EntityPanda;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;

/** The panda (1.14): walking, sitting to eat, cowering, lying on its back, rolling, sneezing, sulking. */
public class ModelPanda extends ModelBase {

    private final ModelRenderer head;
    private final ModelRenderer body;
    private final ModelRenderer legBackRight;
    private final ModelRenderer legBackLeft;
    private final ModelRenderer legFrontRight;
    private final ModelRenderer legFrontLeft;
    private float sitAmount;
    private float onBackAmount;
    private float rollAmount;

    public ModelPanda() {
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.head = new ModelRenderer(this, 0, 6);
        this.head.addBox(-6.5F, -5.0F, -4.0F, 13, 10, 9);
        this.head.setRotationPoint(0.0F, 11.5F, -17.0F);
        this.head.setTextureOffset(45, 16).addBox(-3.5F, 0.0F, -6.0F, 7, 5, 2);
        this.head.setTextureOffset(52, 25).addBox(-8.5F, -8.0F, -1.0F, 5, 4, 1);
        this.head.setTextureOffset(52, 25).addBox(3.5F, -8.0F, -1.0F, 5, 4, 1);
        this.body = new ModelRenderer(this, 0, 25);
        this.body.addBox(-9.5F, -13.0F, -6.5F, 19, 26, 13);
        this.body.setRotationPoint(0.0F, 10.0F, 0.0F);
        this.legBackRight = leg(-5.5F, 9.0F);
        this.legBackLeft = leg(5.5F, 9.0F);
        this.legFrontRight = leg(-5.5F, -9.0F);
        this.legFrontLeft = leg(5.5F, -9.0F);
    }

    private ModelRenderer leg(float x, float z) {
        ModelRenderer leg = new ModelRenderer(this, 40, 0);
        leg.addBox(-3.0F, 0.0F, -3.0F, 6, 9, 6);
        leg.setRotationPoint(x, 15.0F, z);
        return leg;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        if (this.isChild) {
            GlStateManager.pushMatrix();
            float headScale = 1.5F / 2.7F;
            GlStateManager.scale(headScale, headScale, headScale);
            GlStateManager.translate(0.0F, 23.0F * scale, 4.8F * scale);
            this.head.render(scale);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.scale(1.0F / 3.0F, 1.0F / 3.0F, 1.0F / 3.0F);
            GlStateManager.translate(0.0F, 49.0F * scale, 0.0F);
            this.renderBody(scale);
            GlStateManager.popMatrix();
        } else {
            this.head.render(scale);
            this.renderBody(scale);
        }
    }

    private void renderBody(float scale) {
        this.body.render(scale);
        this.legBackRight.render(scale);
        this.legBackLeft.render(scale);
        this.legFrontRight.render(scale);
        this.legFrontLeft.render(scale);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks) {
        EntityPanda panda = (EntityPanda) entity;
        this.sitAmount = panda.getSitAmount(partialTicks);
        this.onBackAmount = panda.getOnBackAmount(partialTicks);
        this.rollAmount = panda.isChild() ? 0.0F : panda.getRollAmount(partialTicks);
    }

    /** 1.15's {@code ModelUtils}: the shortest turn from one angle toward another. */
    private static float turn(float from, float to, float amount) {
        float difference = to - from;
        while (difference < -(float) Math.PI) difference += ((float) Math.PI * 2F);
        while (difference >= (float) Math.PI) difference -= ((float) Math.PI * 2F);
        return from + amount * difference;
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        EntityPanda panda = (EntityPanda) entity;
        // 1.14's QuadrupedModel first.
        this.head.rotateAngleX = headPitch * 0.017453292F;
        this.head.rotateAngleY = netHeadYaw * 0.017453292F;
        this.body.rotateAngleX = ((float) Math.PI / 2F);
        this.legBackRight.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.legBackLeft.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
        this.legFrontRight.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
        this.legFrontLeft.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        boolean unhappy = panda.getUnhappyCounter() > 0;
        boolean sneezing = panda.isSneezing();
        int sneeze = panda.getSneezeCounter();
        boolean eating = panda.isEating();
        boolean scared = panda.isScared();
        if (unhappy) {
            this.head.rotateAngleY = 0.35F * MathHelper.sin(0.6F * ageInTicks);
            this.head.rotateAngleZ = 0.35F * MathHelper.sin(0.6F * ageInTicks);
            this.legFrontRight.rotateAngleX = -0.75F * MathHelper.sin(0.3F * ageInTicks);
            this.legFrontLeft.rotateAngleX = 0.75F * MathHelper.sin(0.3F * ageInTicks);
        } else {
            this.head.rotateAngleZ = 0.0F;
        }
        if (sneezing) {
            if (sneeze < 15) {
                this.head.rotateAngleX = -((float) Math.PI / 4F) * (float) sneeze / 14.0F;
            } else if (sneeze < 20) {
                float progress = (float) ((sneeze - 15) / 5);
                this.head.rotateAngleX = -((float) Math.PI / 4F) + ((float) Math.PI / 4F) * progress;
            }
        }
        if (this.sitAmount > 0.0F) {
            this.body.rotateAngleX = turn(this.body.rotateAngleX, 1.7407963F, this.sitAmount);
            this.head.rotateAngleX = turn(this.head.rotateAngleX, ((float) Math.PI / 2F), this.sitAmount);
            this.legFrontRight.rotateAngleZ = -0.27079642F;
            this.legFrontLeft.rotateAngleZ = 0.27079642F;
            this.legBackRight.rotateAngleZ = 0.5707964F;
            this.legBackLeft.rotateAngleZ = -0.5707964F;
            if (eating) {
                this.head.rotateAngleX = ((float) Math.PI / 2F) + 0.2F * MathHelper.sin(ageInTicks * 0.6F);
                this.legFrontRight.rotateAngleX = -0.4F - 0.2F * MathHelper.sin(ageInTicks * 0.6F);
                this.legFrontLeft.rotateAngleX = -0.4F - 0.2F * MathHelper.sin(ageInTicks * 0.6F);
            }
            if (scared) {
                this.head.rotateAngleX = 2.1707964F;
                this.legFrontRight.rotateAngleX = -0.9F;
                this.legFrontLeft.rotateAngleX = -0.9F;
            }
        } else {
            this.legBackRight.rotateAngleZ = 0.0F;
            this.legBackLeft.rotateAngleZ = 0.0F;
            this.legFrontRight.rotateAngleZ = 0.0F;
            this.legFrontLeft.rotateAngleZ = 0.0F;
        }
        if (this.onBackAmount > 0.0F) {
            this.legBackRight.rotateAngleX = -0.6F * MathHelper.sin(ageInTicks * 0.15F);
            this.legBackLeft.rotateAngleX = 0.6F * MathHelper.sin(ageInTicks * 0.15F);
            this.legFrontRight.rotateAngleX = 0.3F * MathHelper.sin(ageInTicks * 0.25F);
            this.legFrontLeft.rotateAngleX = -0.3F * MathHelper.sin(ageInTicks * 0.25F);
            this.head.rotateAngleX = turn(this.head.rotateAngleX, ((float) Math.PI / 2F), this.onBackAmount);
        }
        if (this.rollAmount > 0.0F) {
            this.head.rotateAngleX = turn(this.head.rotateAngleX, 2.0561945F, this.rollAmount);
            this.legBackRight.rotateAngleX = -0.5F * MathHelper.sin(ageInTicks * 0.5F);
            this.legBackLeft.rotateAngleX = 0.5F * MathHelper.sin(ageInTicks * 0.5F);
            this.legFrontRight.rotateAngleX = 0.5F * MathHelper.sin(ageInTicks * 0.5F);
            this.legFrontLeft.rotateAngleX = -0.5F * MathHelper.sin(ageInTicks * 0.5F);
        }
    }
}
