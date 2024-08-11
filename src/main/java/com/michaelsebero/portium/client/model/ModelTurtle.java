package com.michaelsebero.portium.client.model;

import com.michaelsebero.portium.entity.passive.EntityTurtle;
import net.minecraft.client.model.ModelQuadruped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** The sea turtle (1.13): flippers that paddle in water and shuffle on land, and a visible egg bump when carrying eggs. */
public class ModelTurtle extends ModelQuadruped {

    private final ModelRenderer eggBelly;

    public ModelTurtle() {
        super(12, 0.0F);
        this.textureWidth = 128;
        this.textureHeight = 64;
        this.head = new ModelRenderer(this, 3, 0);
        this.head.addBox(-3.0F, -1.0F, -3.0F, 6, 5, 6);
        this.head.setRotationPoint(0.0F, 19.0F, -10.0F);
        this.body = new ModelRenderer(this);
        this.body.setTextureOffset(7, 37).addBox(-9.5F, 3.0F, -10.0F, 19, 20, 6);
        this.body.setTextureOffset(31, 1).addBox(-5.5F, 3.0F, -13.0F, 11, 18, 3);
        this.body.setRotationPoint(0.0F, 11.0F, -10.0F);
        this.eggBelly = new ModelRenderer(this);
        this.eggBelly.setTextureOffset(70, 33).addBox(-4.5F, 3.0F, -14.0F, 9, 18, 1);
        this.eggBelly.setRotationPoint(0.0F, 11.0F, -10.0F);
        this.leg1 = new ModelRenderer(this, 1, 23);
        this.leg1.addBox(-2.0F, 0.0F, 0.0F, 4, 1, 10);
        this.leg1.setRotationPoint(-3.5F, 22.0F, 11.0F);
        this.leg2 = new ModelRenderer(this, 1, 12);
        this.leg2.addBox(-2.0F, 0.0F, 0.0F, 4, 1, 10);
        this.leg2.setRotationPoint(3.5F, 22.0F, 11.0F);
        this.leg3 = new ModelRenderer(this, 27, 30);
        this.leg3.addBox(-13.0F, 0.0F, -2.0F, 13, 1, 5);
        this.leg3.setRotationPoint(-5.0F, 21.0F, -4.0F);
        this.leg4 = new ModelRenderer(this, 27, 24);
        this.leg4.addBox(0.0F, 0.0F, -2.0F, 13, 1, 5);
        this.leg4.setRotationPoint(5.0F, 21.0F, -4.0F);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        GlStateManager.pushMatrix();
        if (this.isChild) {
            GlStateManager.scale(1.0F / 6.0F, 1.0F / 6.0F, 1.0F / 6.0F);
            GlStateManager.translate(0.0F, 120.0F * scale, 0.0F);
        } else if (entity instanceof EntityTurtle && ((EntityTurtle) entity).hasEgg()) {
            GlStateManager.translate(0.0F, -0.08F, 0.0F);
        }
        this.head.render(scale);
        this.body.render(scale);
        this.leg1.render(scale);
        this.leg2.render(scale);
        this.leg3.render(scale);
        this.leg4.render(scale);
        if (!this.isChild && entity instanceof EntityTurtle && ((EntityTurtle) entity).hasEgg()) this.eggBelly.render(scale);
        GlStateManager.popMatrix();
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        float pi = (float) Math.PI;
        this.leg1.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F * 0.6F) * 0.5F * limbSwingAmount;
        this.leg2.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F * 0.6F + pi) * 0.5F * limbSwingAmount;
        this.leg3.rotateAngleZ = MathHelper.cos(limbSwing * 0.6662F * 0.6F + pi) * 0.5F * limbSwingAmount;
        this.leg4.rotateAngleZ = MathHelper.cos(limbSwing * 0.6662F * 0.6F) * 0.5F * limbSwingAmount;
        this.leg3.rotateAngleX = 0.0F;
        this.leg4.rotateAngleX = 0.0F;
        this.leg3.rotateAngleY = 0.0F;
        this.leg4.rotateAngleY = 0.0F;
        this.leg1.rotateAngleY = 0.0F;
        this.leg2.rotateAngleY = 0.0F;
        this.eggBelly.rotateAngleX = pi / 2.0F;
        if (!entity.isInWater() && entity.onGround) {
            boolean digging = entity instanceof EntityTurtle && ((EntityTurtle) entity).isDigging();
            float speed = digging ? 4.0F : 1.0F;
            float reach = digging ? 2.0F : 1.0F;
            this.leg3.rotateAngleY = MathHelper.cos(speed * limbSwing * 5.0F + pi) * 8.0F * limbSwingAmount * reach;
            this.leg3.rotateAngleZ = 0.0F;
            this.leg4.rotateAngleY = MathHelper.cos(speed * limbSwing * 5.0F) * 8.0F * limbSwingAmount * reach;
            this.leg4.rotateAngleZ = 0.0F;
            this.leg1.rotateAngleY = MathHelper.cos(limbSwing * 5.0F + pi) * 3.0F * limbSwingAmount;
            this.leg1.rotateAngleX = 0.0F;
            this.leg2.rotateAngleY = MathHelper.cos(limbSwing * 5.0F) * 3.0F * limbSwingAmount;
            this.leg2.rotateAngleX = 0.0F;
        }
    }
}
