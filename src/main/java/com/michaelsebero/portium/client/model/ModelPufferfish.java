package com.michaelsebero.portium.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/** The pufferfish in its three puff states (1.13): small, half puffed with spikes, and fully puffed. */
public class ModelPufferfish extends ModelBase {

    private final List<ModelRenderer> parts = new ArrayList<>();
    private ModelRenderer rightFin;
    private ModelRenderer leftFin;

    public static ModelPufferfish small() {
        ModelPufferfish model = new ModelPufferfish();
        model.part(0, 27, -1.5F, -2.0F, -1.5F, 3, 2, 3, 0.0F, 23.0F, 0.0F, 0.0F, 0.0F);
        model.part(24, 6, -1.5F, 0.0F, -1.5F, 1, 1, 1, 0.0F, 20.0F, 0.0F, 0.0F, 0.0F);
        model.part(28, 6, 0.5F, 0.0F, -1.5F, 1, 1, 1, 0.0F, 20.0F, 0.0F, 0.0F, 0.0F);
        model.part(-3, 0, -1.5F, 0.0F, 0.0F, 3, 0, 3, 0.0F, 22.0F, 1.5F, 0.0F, 0.0F);
        model.rightFin = model.part(25, 0, -1.0F, 0.0F, 0.0F, 1, 0, 2, -1.5F, 22.0F, -1.5F, 0.0F, 0.0F);
        model.leftFin = model.part(25, 0, 0.0F, 0.0F, 0.0F, 1, 0, 2, 1.5F, 22.0F, -1.5F, 0.0F, 0.0F);
        return model;
    }

    public static ModelPufferfish medium() {
        ModelPufferfish model = new ModelPufferfish();
        float quarter = (float) Math.PI / 4.0F;
        model.part(12, 22, -2.5F, -5.0F, -2.5F, 5, 5, 5, 0.0F, 22.0F, 0.0F, 0.0F, 0.0F);
        model.rightFin = model.part(24, 0, -2.0F, 0.0F, 0.0F, 2, 0, 2, -2.5F, 17.0F, -1.5F, 0.0F, 0.0F);
        model.leftFin = model.part(24, 3, 0.0F, 0.0F, 0.0F, 2, 0, 2, 2.5F, 17.0F, -1.5F, 0.0F, 0.0F);
        model.part(15, 16, -2.5F, -1.0F, 0.0F, 5, 1, 1, 0.0F, 17.0F, -2.5F, quarter, 0.0F);
        model.part(10, 16, -2.5F, -1.0F, -1.0F, 5, 1, 1, 0.0F, 17.0F, 2.5F, -quarter, 0.0F);
        model.part(8, 16, -1.0F, -5.0F, 0.0F, 1, 5, 1, -2.5F, 22.0F, -2.5F, 0.0F, -quarter);
        model.part(8, 16, 0.0F, -5.0F, 0.0F, 1, 5, 1, 2.5F, 22.0F, -2.5F, 0.0F, quarter);
        model.part(4, 16, 0.0F, -5.0F, 0.0F, 1, 5, 1, 2.5F, 22.0F, 2.5F, 0.0F, -quarter);
        model.part(0, 16, -1.0F, -5.0F, 0.0F, 1, 5, 1, -2.5F, 22.0F, 2.5F, 0.0F, quarter);
        model.part(18, 20, -2.5F, 0.0F, 0.0F, 5, 1, 1, 0.0F, 22.0F, 2.5F, quarter, 0.0F);
        model.part(17, 19, -2.5F, 0.0F, 0.0F, 5, 1, 1, 0.0F, 22.0F, -2.5F, -quarter, 0.0F);
        return model;
    }

    public static ModelPufferfish big() {
        ModelPufferfish model = new ModelPufferfish();
        float quarter = (float) Math.PI / 4.0F;
        model.part(0, 0, -4.0F, -8.0F, -4.0F, 8, 8, 8, 0.0F, 22.0F, 0.0F, 0.0F, 0.0F);
        model.rightFin = model.part(24, 0, -2.0F, 0.0F, -1.0F, 2, 1, 2, -4.0F, 15.0F, -2.0F, 0.0F, 0.0F);
        model.leftFin = model.part(24, 3, 0.0F, 0.0F, -1.0F, 2, 1, 2, 4.0F, 15.0F, -2.0F, 0.0F, 0.0F);
        model.part(15, 17, -4.0F, -1.0F, 0.0F, 8, 1, 0, 0.0F, 14.0F, -4.0F, quarter, 0.0F);
        model.part(14, 16, -4.0F, -1.0F, 0.0F, 8, 1, 1, 0.0F, 14.0F, 0.0F, 0.0F, 0.0F);
        model.part(23, 18, -4.0F, -1.0F, 0.0F, 8, 1, 0, 0.0F, 14.0F, 4.0F, -quarter, 0.0F);
        model.part(5, 17, -1.0F, -8.0F, 0.0F, 1, 8, 0, -4.0F, 22.0F, -4.0F, 0.0F, -quarter);
        model.part(1, 17, 0.0F, -8.0F, 0.0F, 1, 8, 0, 4.0F, 22.0F, -4.0F, 0.0F, quarter);
        model.part(15, 20, -4.0F, 0.0F, 0.0F, 8, 1, 0, 0.0F, 22.0F, -4.0F, -quarter, 0.0F);
        model.part(15, 20, -4.0F, 0.0F, 0.0F, 8, 1, 0, 0.0F, 22.0F, 0.0F, 0.0F, 0.0F);
        model.part(15, 20, -4.0F, 0.0F, 0.0F, 8, 1, 0, 0.0F, 22.0F, 4.0F, quarter, 0.0F);
        model.part(9, 17, -1.0F, -8.0F, 0.0F, 1, 8, 0, -4.0F, 22.0F, 4.0F, 0.0F, quarter);
        model.part(9, 17, 0.0F, -8.0F, 0.0F, 1, 8, 0, 4.0F, 22.0F, 4.0F, 0.0F, -quarter);
        return model;
    }

    private ModelPufferfish() {
        this.textureWidth = 32;
        this.textureHeight = 32;
    }

    private ModelRenderer part(int u, int v, float x, float y, float z, int width, int height, int depth, float pivotX, float pivotY, float pivotZ, float rotX, float rotY) {
        ModelRenderer part = new ModelRenderer(this, u, v);
        part.addBox(x, y, z, width, height, depth);
        part.setRotationPoint(pivotX, pivotY, pivotZ);
        part.rotateAngleX = rotX;
        part.rotateAngleY = rotY;
        this.parts.add(part);
        return part;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        for (ModelRenderer part : this.parts) part.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        this.rightFin.rotateAngleZ = -0.2F + 0.4F * MathHelper.sin(ageInTicks * 0.2F);
        this.leftFin.rotateAngleZ = 0.2F - 0.4F * MathHelper.sin(ageInTicks * 0.2F);
    }
}
