package com.michaelsebero.portium.client.model;

import com.michaelsebero.portium.entity.passive.EntityStrider;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** 1.16's strider model: a body on two long legs, with three bristle fins down each side that sway as it walks. */
public class ModelStrider extends ModelBase {

    private final ModelRenderer rightLeg;
    private final ModelRenderer leftLeg;
    private final ModelRenderer body;
    private final ModelRenderer bristle0;
    private final ModelRenderer bristle1;
    private final ModelRenderer bristle2;
    private final ModelRenderer bristle3;
    private final ModelRenderer bristle4;
    private final ModelRenderer bristle5;

    public ModelStrider() {
        this.textureWidth = 64;
        this.textureHeight = 128;
        this.rightLeg = new ModelRenderer(this, 0, 32);
        this.rightLeg.setRotationPoint(-4.0F, 8.0F, 0.0F);
        this.rightLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 16, 4, 0.0F);
        this.leftLeg = new ModelRenderer(this, 0, 55);
        this.leftLeg.setRotationPoint(4.0F, 8.0F, 0.0F);
        this.leftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 16, 4, 0.0F);
        this.body = new ModelRenderer(this, 0, 0);
        this.body.setRotationPoint(0.0F, 1.0F, 0.0F);
        this.body.addBox(-8.0F, -6.0F, -8.0F, 16, 14, 16, 0.0F);
        this.bristle0 = bristle(16, 65, -8.0F, 4.0F, true, -1.2217305F);
        this.bristle1 = bristle(16, 49, -8.0F, -1.0F, true, -1.134464F);
        this.bristle2 = bristle(16, 33, -8.0F, -5.0F, true, -0.87266463F);
        this.bristle3 = bristle(16, 33, 8.0F, -6.0F, false, 0.87266463F);
        this.bristle4 = bristle(16, 49, 8.0F, -2.0F, false, 1.134464F);
        this.bristle5 = bristle(16, 65, 8.0F, 3.0F, false, 1.2217305F);
    }

    private ModelRenderer bristle(int u, int v, float x, float y, boolean mirror, float angle) {
        ModelRenderer bristle = new ModelRenderer(this, u, v);
        bristle.setRotationPoint(x, y, -8.0F);
        bristle.mirror = mirror;
        bristle.addBox(mirror ? -12.0F : 0.0F, 0.0F, 0.0F, 12, 0, 16, 0.0F);
        bristle.rotateAngleZ = angle;
        this.body.addChild(bristle);
        return bristle;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.body.render(scale);
        this.leftLeg.render(scale);
        this.rightLeg.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        limbSwingAmount = Math.min(0.25F, limbSwingAmount);
        if (entity.getPassengers().isEmpty()) {
            this.body.rotateAngleX = headPitch * 0.017453292F;
            this.body.rotateAngleY = netHeadYaw * 0.017453292F;
        } else {
            this.body.rotateAngleX = 0.0F;
            this.body.rotateAngleY = 0.0F;
        }
        this.body.rotateAngleZ = 0.1F * MathHelper.sin(limbSwing * 1.5F) * 4.0F * limbSwingAmount;
        this.body.rotationPointY = 2.0F - 2.0F * MathHelper.cos(limbSwing * 1.5F) * 2.0F * limbSwingAmount;
        this.leftLeg.rotateAngleX = MathHelper.sin(limbSwing * 1.5F * 0.5F) * 2.0F * limbSwingAmount;
        this.rightLeg.rotateAngleX = MathHelper.sin(limbSwing * 1.5F * 0.5F + (float) Math.PI) * 2.0F * limbSwingAmount;
        this.leftLeg.rotateAngleZ = 0.17453292F * MathHelper.cos(limbSwing * 1.5F * 0.5F) * limbSwingAmount;
        this.rightLeg.rotateAngleZ = 0.17453292F * MathHelper.cos(limbSwing * 1.5F * 0.5F + (float) Math.PI) * limbSwingAmount;
        this.leftLeg.rotationPointY = 8.0F + 2.0F * MathHelper.sin(limbSwing * 1.5F * 0.5F + (float) Math.PI) * 2.0F * limbSwingAmount;
        this.rightLeg.rotationPointY = 8.0F + 2.0F * MathHelper.sin(limbSwing * 1.5F * 0.5F) * 2.0F * limbSwingAmount;
        float sway = MathHelper.cos(limbSwing * 1.5F + (float) Math.PI) * limbSwingAmount;
        this.bristle0.rotateAngleZ = -1.2217305F + sway * 1.3F + 0.05F * MathHelper.sin(ageInTicks * -0.4F);
        this.bristle1.rotateAngleZ = -1.134464F + sway * 1.2F + 0.1F * MathHelper.sin(ageInTicks * 0.2F);
        this.bristle2.rotateAngleZ = -0.87266463F + sway * 0.6F + 0.1F * MathHelper.sin(ageInTicks * 0.4F);
        this.bristle3.rotateAngleZ = 0.87266463F + sway * 0.6F + 0.1F * MathHelper.sin(ageInTicks * 0.4F);
        this.bristle4.rotateAngleZ = 1.134464F + sway * 1.2F + 0.1F * MathHelper.sin(ageInTicks * 0.2F);
        this.bristle5.rotateAngleZ = 1.2217305F + sway * 1.3F + 0.05F * MathHelper.sin(ageInTicks * -0.4F);
    }
}
