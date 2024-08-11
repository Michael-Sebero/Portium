package com.michaelsebero.portium.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** The phantom (1.13): beating two-part wings and a swaying tail. */
public class ModelPhantom extends ModelBase {

    private final ModelRenderer body;
    private final ModelRenderer leftWing;
    private final ModelRenderer leftWingTip;
    private final ModelRenderer rightWing;
    private final ModelRenderer rightWingTip;
    private final ModelRenderer tailBase;
    private final ModelRenderer tailTip;

    public ModelPhantom() {
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.body = new ModelRenderer(this, 0, 8);
        this.body.addBox(-3.0F, -2.0F, -8.0F, 5, 3, 9);
        this.tailBase = new ModelRenderer(this, 3, 20);
        this.tailBase.addBox(-2.0F, 0.0F, 0.0F, 3, 2, 6);
        this.tailBase.setRotationPoint(0.0F, -2.0F, 1.0F);
        this.body.addChild(this.tailBase);
        this.tailTip = new ModelRenderer(this, 4, 29);
        this.tailTip.addBox(-1.0F, 0.0F, 0.0F, 1, 1, 6);
        this.tailTip.setRotationPoint(0.0F, 0.5F, 6.0F);
        this.tailBase.addChild(this.tailTip);
        this.leftWing = new ModelRenderer(this, 23, 12);
        this.leftWing.addBox(0.0F, 0.0F, 0.0F, 6, 2, 9);
        this.leftWing.setRotationPoint(2.0F, -2.0F, -8.0F);
        this.leftWingTip = new ModelRenderer(this, 16, 24);
        this.leftWingTip.addBox(0.0F, 0.0F, 0.0F, 13, 1, 9);
        this.leftWingTip.setRotationPoint(6.0F, 0.0F, 0.0F);
        this.leftWing.addChild(this.leftWingTip);
        this.rightWing = new ModelRenderer(this, 23, 12);
        this.rightWing.mirror = true;
        this.rightWing.addBox(-6.0F, 0.0F, 0.0F, 6, 2, 9);
        this.rightWing.setRotationPoint(-3.0F, -2.0F, -8.0F);
        this.rightWingTip = new ModelRenderer(this, 16, 24);
        this.rightWingTip.mirror = true;
        this.rightWingTip.addBox(-13.0F, 0.0F, 0.0F, 13, 1, 9);
        this.rightWingTip.setRotationPoint(-6.0F, 0.0F, 0.0F);
        this.rightWing.addChild(this.rightWingTip);
        this.body.rotateAngleX = -0.1F;
        ModelRenderer head = new ModelRenderer(this, 0, 0);
        head.addBox(-4.0F, -2.0F, -5.0F, 7, 3, 5);
        head.setRotationPoint(0.0F, 1.0F, -7.0F);
        head.rotateAngleX = 0.2F;
        this.body.addChild(head);
        this.body.addChild(this.leftWing);
        this.body.addChild(this.rightWing);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.body.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        float phase = (entity.getEntityId() * 3 + ageInTicks) * 0.13F;
        float wing = MathHelper.cos(phase) * 16.0F * 0.017453292F;
        this.leftWing.rotateAngleZ = wing;
        this.leftWingTip.rotateAngleZ = wing;
        this.rightWing.rotateAngleZ = -wing;
        this.rightWingTip.rotateAngleZ = -wing;
        float tail = -(5.0F + MathHelper.cos(phase * 2.0F) * 5.0F) * 0.017453292F;
        this.tailBase.rotateAngleX = tail;
        this.tailTip.rotateAngleX = tail;
    }
}
