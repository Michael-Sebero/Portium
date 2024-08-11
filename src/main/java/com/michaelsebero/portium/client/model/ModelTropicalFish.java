package com.michaelsebero.portium.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** The two tropical fish body shapes (1.13): A is small and slim, B is tall and flat. */
public class ModelTropicalFish extends ModelBase {

    private final ModelRenderer[] parts;
    private final ModelRenderer tail;

    public static ModelTropicalFish shapeA(float inflate) {
        return new ModelTropicalFish(false, inflate);
    }

    public static ModelTropicalFish shapeB(float inflate) {
        return new ModelTropicalFish(true, inflate);
    }

    private ModelTropicalFish(boolean tall, float inflate) {
        this.textureWidth = 32;
        this.textureHeight = 32;
        float quarter = (float) Math.PI / 4.0F;
        if (!tall) {
            ModelRenderer body = this.part(0, 0, -1.0F, -1.5F, -3.0F, 2, 3, 6, inflate, 0.0F, 22.0F, 0.0F, 0.0F);
            this.tail = this.part(22, -6, 0.0F, -1.5F, 0.0F, 0, 3, 6, inflate, 0.0F, 22.0F, 3.0F, 0.0F);
            ModelRenderer rightFin = this.part(2, 16, -2.0F, -1.0F, 0.0F, 2, 2, 0, inflate, -1.0F, 22.5F, 0.0F, quarter);
            ModelRenderer leftFin = this.part(2, 12, 0.0F, -1.0F, 0.0F, 2, 2, 0, inflate, 1.0F, 22.5F, 0.0F, -quarter);
            ModelRenderer topFin = this.part(10, -5, 0.0F, -3.0F, 0.0F, 0, 3, 6, inflate, 0.0F, 20.5F, -3.0F, 0.0F);
            this.parts = new ModelRenderer[] {body, this.tail, rightFin, leftFin, topFin};
        } else {
            ModelRenderer body = this.part(0, 20, -1.0F, -3.0F, -3.0F, 2, 6, 6, inflate, 0.0F, 19.0F, 0.0F, 0.0F);
            this.tail = this.part(21, 16, 0.0F, -3.0F, 0.0F, 0, 6, 5, inflate, 0.0F, 19.0F, 3.0F, 0.0F);
            ModelRenderer rightFin = this.part(2, 16, -2.0F, 0.0F, 0.0F, 2, 2, 0, inflate, -1.0F, 20.0F, 0.0F, quarter);
            ModelRenderer leftFin = this.part(2, 12, 0.0F, 0.0F, 0.0F, 2, 2, 0, inflate, 1.0F, 20.0F, 0.0F, -quarter);
            ModelRenderer topFin = this.part(20, 11, 0.0F, -4.0F, 0.0F, 0, 4, 6, inflate, 0.0F, 16.0F, -3.0F, 0.0F);
            ModelRenderer bottomFin = this.part(20, 21, 0.0F, 0.0F, 0.0F, 0, 4, 6, inflate, 0.0F, 22.0F, -3.0F, 0.0F);
            this.parts = new ModelRenderer[] {body, this.tail, rightFin, leftFin, topFin, bottomFin};
        }
    }

    private ModelRenderer part(int u, int v, float x, float y, float z, int width, int height, int depth, float inflate, float pivotX, float pivotY, float pivotZ, float rotY) {
        ModelRenderer part = new ModelRenderer(this, u, v);
        part.addBox(x, y, z, width, height, depth, inflate);
        part.setRotationPoint(pivotX, pivotY, pivotZ);
        part.rotateAngleY = rotY;
        return part;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        for (ModelRenderer part : this.parts) part.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        float speed = entity.isInWater() ? 1.0F : 1.5F;
        this.tail.rotateAngleY = -speed * 0.45F * MathHelper.sin(0.6F * ageInTicks);
    }
}
