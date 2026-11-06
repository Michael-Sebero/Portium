package com.michaelsebero.portium.client.model;

import com.michaelsebero.portium.entity.monster.IFlinging;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** 1.16's {@code BoarModel} for hoglins and zoglins: a heavy body with a mane, a tusked head that tosses when attacking. */
public class ModelBoar extends ModelBase {

    private final ModelRenderer head;
    private final ModelRenderer rightEar;
    private final ModelRenderer leftEar;
    private final ModelRenderer body;
    private final ModelRenderer frontRightLeg;
    private final ModelRenderer frontLeftLeg;
    private final ModelRenderer backRightLeg;
    private final ModelRenderer backLeftLeg;
    private final ModelRenderer mane;

    public ModelBoar() {
        this.textureWidth = 128;
        this.textureHeight = 64;
        this.body = new ModelRenderer(this);
        this.body.setRotationPoint(0.0F, 7.0F, 0.0F);
        this.body.setTextureOffset(1, 1).addBox(-8.0F, -7.0F, -13.0F, 16, 14, 26);
        this.mane = new ModelRenderer(this);
        this.mane.setRotationPoint(0.0F, -14.0F, -5.0F);
        this.mane.setTextureOffset(90, 33).addBox(0.0F, 0.0F, -9.0F, 0, 10, 19, 0.001F);
        this.body.addChild(this.mane);
        this.head = new ModelRenderer(this);
        this.head.setRotationPoint(0.0F, 2.0F, -12.0F);
        this.head.setTextureOffset(61, 1).addBox(-7.0F, -3.0F, -19.0F, 14, 6, 19);
        this.rightEar = new ModelRenderer(this);
        this.rightEar.setRotationPoint(-6.0F, -2.0F, -3.0F);
        this.rightEar.setTextureOffset(1, 1).addBox(-6.0F, -1.0F, -2.0F, 6, 1, 4);
        this.rightEar.rotateAngleZ = -0.6981317F;
        this.head.addChild(this.rightEar);
        this.leftEar = new ModelRenderer(this);
        this.leftEar.setRotationPoint(6.0F, -2.0F, -3.0F);
        this.leftEar.setTextureOffset(1, 6).addBox(0.0F, -1.0F, -2.0F, 6, 1, 4);
        this.leftEar.rotateAngleZ = 0.6981317F;
        this.head.addChild(this.leftEar);
        ModelRenderer rightTusk = new ModelRenderer(this);
        rightTusk.setRotationPoint(-7.0F, 2.0F, -12.0F);
        rightTusk.setTextureOffset(10, 13).addBox(-1.0F, -11.0F, -1.0F, 2, 11, 2);
        this.head.addChild(rightTusk);
        ModelRenderer leftTusk = new ModelRenderer(this);
        leftTusk.setRotationPoint(7.0F, 2.0F, -12.0F);
        leftTusk.setTextureOffset(1, 13).addBox(-1.0F, -11.0F, -1.0F, 2, 11, 2);
        this.head.addChild(leftTusk);
        this.head.rotateAngleX = 0.87266463F;
        this.frontRightLeg = new ModelRenderer(this);
        this.frontRightLeg.setRotationPoint(-4.0F, 10.0F, -8.5F);
        this.frontRightLeg.setTextureOffset(66, 42).addBox(-3.0F, 0.0F, -3.0F, 6, 14, 6);
        this.frontLeftLeg = new ModelRenderer(this);
        this.frontLeftLeg.setRotationPoint(4.0F, 10.0F, -8.5F);
        this.frontLeftLeg.setTextureOffset(41, 42).addBox(-3.0F, 0.0F, -3.0F, 6, 14, 6);
        this.backRightLeg = new ModelRenderer(this);
        this.backRightLeg.setRotationPoint(-5.0F, 13.0F, 10.0F);
        this.backRightLeg.setTextureOffset(21, 45).addBox(-2.5F, 0.0F, -2.5F, 5, 11, 5);
        this.backLeftLeg = new ModelRenderer(this);
        this.backLeftLeg.setRotationPoint(5.0F, 13.0F, 10.0F);
        this.backLeftLeg.setTextureOffset(0, 45).addBox(-2.5F, 0.0F, -2.5F, 5, 11, 5);
    }

    /** 1.16's {@code AgeableModel(true, 8, 6, 1.9, 2, 24)}: a baby's head is scaled less than its body. */
    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        if (this.isChild) {
            GlStateManager.pushMatrix();
            float headScale = 1.5F / 1.9F;
            GlStateManager.scale(headScale, headScale, headScale);
            GlStateManager.translate(0.0F, 8.0F * scale, 6.0F * scale);
            this.head.render(scale);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
            GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
            this.renderBody(scale);
            GlStateManager.popMatrix();
        } else {
            this.head.render(scale);
            this.renderBody(scale);
        }
    }

    private void renderBody(float scale) {
        this.body.render(scale);
        this.frontRightLeg.render(scale);
        this.frontLeftLeg.render(scale);
        this.backRightLeg.render(scale);
        this.backLeftLeg.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        this.rightEar.rotateAngleZ = -0.6981317F - limbSwingAmount * MathHelper.sin(limbSwing);
        this.leftEar.rotateAngleZ = 0.6981317F + limbSwingAmount * MathHelper.sin(limbSwing);
        this.head.rotateAngleY = netHeadYaw * 0.017453292F;
        int attackTick = entity instanceof IFlinging ? ((IFlinging) entity).getAttackAnimationTick() : 0;
        float toss = 1.0F - (float) MathHelper.abs(10 - 2 * attackTick) / 10.0F;
        this.head.rotateAngleX = lerp(toss, 0.87266463F, -0.34906584F);
        if (this.isChild) {
            this.head.rotationPointY = lerp(toss, 2.0F, 5.0F);
            this.mane.rotationPointZ = -3.0F;
        } else {
            this.head.rotationPointY = 2.0F;
            this.mane.rotationPointZ = -7.0F;
        }
        this.frontRightLeg.rotateAngleX = MathHelper.cos(limbSwing) * 1.2F * limbSwingAmount;
        this.frontLeftLeg.rotateAngleX = MathHelper.cos(limbSwing + (float) Math.PI) * 1.2F * limbSwingAmount;
        this.backRightLeg.rotateAngleX = this.frontLeftLeg.rotateAngleX;
        this.backLeftLeg.rotateAngleX = this.frontRightLeg.rotateAngleX;
    }

    private static float lerp(float progress, float from, float to) {
        return from + progress * (to - from);
    }
}
