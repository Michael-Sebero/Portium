package com.michaelsebero.portium.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** The salmon (1.13): a two-part body that bends as it swims. */
public class ModelSalmon extends ModelBase {

    private final ModelRenderer bodyFront;
    private final ModelRenderer bodyBack;
    private final ModelRenderer head;
    private final ModelRenderer rightFin;
    private final ModelRenderer leftFin;

    public ModelSalmon() {
        this.textureWidth = 32;
        this.textureHeight = 32;
        this.bodyFront = new ModelRenderer(this, 0, 0);
        this.bodyFront.addBox(-1.5F, -2.5F, 0.0F, 3, 5, 8);
        this.bodyFront.setRotationPoint(0.0F, 20.0F, 0.0F);
        this.bodyBack = new ModelRenderer(this, 0, 13);
        this.bodyBack.addBox(-1.5F, -2.5F, 0.0F, 3, 5, 8);
        this.bodyBack.setRotationPoint(0.0F, 20.0F, 8.0F);
        this.head = new ModelRenderer(this, 22, 0);
        this.head.addBox(-1.0F, -2.0F, -3.0F, 2, 4, 3);
        this.head.setRotationPoint(0.0F, 20.0F, 0.0F);
        ModelRenderer tail = new ModelRenderer(this, 20, 10);
        tail.addBox(0.0F, -2.5F, 0.0F, 0, 5, 6);
        tail.setRotationPoint(0.0F, 0.0F, 8.0F);
        this.bodyBack.addChild(tail);
        ModelRenderer topFinFront = new ModelRenderer(this, 2, 1);
        topFinFront.addBox(0.0F, 0.0F, 0.0F, 0, 2, 3);
        topFinFront.setRotationPoint(0.0F, -4.5F, 5.0F);
        this.bodyFront.addChild(topFinFront);
        ModelRenderer topFinBack = new ModelRenderer(this, 0, 2);
        topFinBack.addBox(0.0F, 0.0F, 0.0F, 0, 2, 4);
        topFinBack.setRotationPoint(0.0F, -4.5F, -1.0F);
        this.bodyBack.addChild(topFinBack);
        this.rightFin = new ModelRenderer(this, -4, 0);
        this.rightFin.addBox(-2.0F, 0.0F, 0.0F, 2, 0, 2);
        this.rightFin.setRotationPoint(-1.5F, 21.5F, 0.0F);
        this.rightFin.rotateAngleZ = -(float) Math.PI / 4.0F;
        this.leftFin = new ModelRenderer(this, 0, 0);
        this.leftFin.addBox(0.0F, 0.0F, 0.0F, 2, 0, 2);
        this.leftFin.setRotationPoint(1.5F, 21.5F, 0.0F);
        this.leftFin.rotateAngleZ = (float) Math.PI / 4.0F;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.bodyFront.render(scale);
        this.bodyBack.render(scale);
        this.head.render(scale);
        this.rightFin.render(scale);
        this.leftFin.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        float amplitude = entity.isInWater() ? 1.0F : 1.3F;
        float frequency = entity.isInWater() ? 1.0F : 1.7F;
        this.bodyBack.rotateAngleY = -amplitude * 0.25F * MathHelper.sin(frequency * 0.6F * ageInTicks);
    }
}
