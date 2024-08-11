package com.michaelsebero.portium.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** The dolphin (1.13): the whole body pitches with the dolphin and the tail beats while it moves. */
public class ModelDolphin extends ModelBase {

    private final ModelRenderer body;
    private final ModelRenderer tail;
    private final ModelRenderer tailFin;

    public ModelDolphin() {
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.body = new ModelRenderer(this, 22, 0);
        this.body.addBox(-4.0F, -7.0F, 0.0F, 8, 7, 13);
        this.body.setRotationPoint(0.0F, 22.0F, -5.0F);
        ModelRenderer backFin = new ModelRenderer(this, 51, 0);
        backFin.addBox(-0.5F, 0.0F, 8.0F, 1, 4, 5);
        backFin.rotateAngleX = (float) Math.PI / 3.0F;
        this.body.addChild(backFin);
        ModelRenderer leftFin = new ModelRenderer(this, 48, 20);
        leftFin.mirror = true;
        leftFin.addBox(-0.5F, -4.0F, 0.0F, 1, 4, 7);
        leftFin.setRotationPoint(2.0F, -2.0F, 4.0F);
        leftFin.rotateAngleX = (float) Math.PI / 3.0F;
        leftFin.rotateAngleZ = 2.0943952F;
        this.body.addChild(leftFin);
        ModelRenderer rightFin = new ModelRenderer(this, 48, 20);
        rightFin.addBox(-0.5F, -4.0F, 0.0F, 1, 4, 7);
        rightFin.setRotationPoint(-2.0F, -2.0F, 4.0F);
        rightFin.rotateAngleX = (float) Math.PI / 3.0F;
        rightFin.rotateAngleZ = -2.0943952F;
        this.body.addChild(rightFin);
        this.tail = new ModelRenderer(this, 0, 19);
        this.tail.addBox(-2.0F, -2.5F, 0.0F, 4, 5, 11);
        this.tail.setRotationPoint(0.0F, -2.5F, 11.0F);
        this.tail.rotateAngleX = -0.10471976F;
        this.body.addChild(this.tail);
        this.tailFin = new ModelRenderer(this, 19, 20);
        this.tailFin.addBox(-5.0F, -0.5F, 0.0F, 10, 1, 6);
        this.tailFin.setRotationPoint(0.0F, 0.0F, 9.0F);
        this.tail.addChild(this.tailFin);
        ModelRenderer head = new ModelRenderer(this, 0, 0);
        head.addBox(-4.0F, -3.0F, -3.0F, 8, 7, 6);
        head.setRotationPoint(0.0F, -4.0F, -3.0F);
        ModelRenderer nose = new ModelRenderer(this, 0, 13);
        nose.addBox(-1.0F, 2.0F, -7.0F, 2, 2, 4);
        head.addChild(nose);
        this.body.addChild(head);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.body.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        this.body.rotateAngleX = headPitch * 0.017453292F;
        this.body.rotateAngleY = netHeadYaw * 0.017453292F;
        if (entity.motionX * entity.motionX + entity.motionZ * entity.motionZ > 1.0E-7D) {
            this.body.rotateAngleX += -0.05F - 0.05F * MathHelper.cos(ageInTicks * 0.3F);
            this.tail.rotateAngleX = -0.1F * MathHelper.cos(ageInTicks * 0.3F);
            this.tailFin.rotateAngleX = -0.2F * MathHelper.cos(ageInTicks * 0.3F);
        }
    }
}
