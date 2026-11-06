package com.michaelsebero.portium.client.model;

import com.michaelsebero.portium.entity.passive.EntityFox;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;

/** The fox (1.14): standing, sitting, curled up asleep, crouched to pounce, or stuck nose-down in snow. */
public class ModelFox extends ModelBase {

    public final ModelRenderer head;
    private final ModelRenderer body;
    private final ModelRenderer legBackRight;
    private final ModelRenderer legBackLeft;
    private final ModelRenderer legFrontRight;
    private final ModelRenderer legFrontLeft;
    private final ModelRenderer tail;
    private float stuckTicks;

    public ModelFox() {
        this.textureWidth = 48;
        this.textureHeight = 32;
        this.head = new ModelRenderer(this, 1, 5);
        this.head.addBox(-3.0F, -2.0F, -5.0F, 8, 6, 6);
        this.head.setRotationPoint(-1.0F, 16.5F, -3.0F);
        ModelRenderer rightEar = new ModelRenderer(this, 8, 1);
        rightEar.addBox(-3.0F, -4.0F, -4.0F, 2, 2, 1);
        ModelRenderer leftEar = new ModelRenderer(this, 15, 1);
        leftEar.addBox(3.0F, -4.0F, -4.0F, 2, 2, 1);
        ModelRenderer snout = new ModelRenderer(this, 6, 18);
        snout.addBox(-1.0F, 2.01F, -8.0F, 4, 2, 3);
        this.head.addChild(rightEar);
        this.head.addChild(leftEar);
        this.head.addChild(snout);
        this.body = new ModelRenderer(this, 24, 15);
        this.body.addBox(-3.0F, 3.999F, -3.5F, 6, 11, 6);
        this.body.setRotationPoint(0.0F, 16.0F, -6.0F);
        this.legBackRight = leg(13, 24, -5.0F, 7.0F);
        this.legBackLeft = leg(4, 24, -1.0F, 7.0F);
        this.legFrontRight = leg(13, 24, -5.0F, 0.0F);
        this.legFrontLeft = leg(4, 24, -1.0F, 0.0F);
        this.tail = new ModelRenderer(this, 30, 0);
        this.tail.addBox(2.0F, 0.0F, -1.0F, 4, 9, 5);
        this.tail.setRotationPoint(-4.0F, 15.0F, -1.0F);
        this.body.addChild(this.tail);
    }

    private ModelRenderer leg(int u, int v, float x, float z) {
        ModelRenderer leg = new ModelRenderer(this, u, v);
        leg.addBox(2.0F, 0.5F, -1.0F, 2, 6, 2, 0.001F);
        leg.setRotationPoint(x, 17.5F, z);
        return leg;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        if (this.isChild) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.75F, 0.75F, 0.75F);
            GlStateManager.translate(0.0F, 8.0F * scale, 3.35F * scale);
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
        this.legBackRight.render(scale);
        this.legBackLeft.render(scale);
        this.legFrontRight.render(scale);
        this.legFrontLeft.render(scale);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks) {
        EntityFox fox = (EntityFox) entity;
        this.body.rotateAngleX = ((float) Math.PI / 2F);
        this.tail.rotateAngleX = -0.05235988F;
        this.legBackRight.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.legBackLeft.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
        this.legFrontRight.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
        this.legFrontLeft.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.head.setRotationPoint(-1.0F, 16.5F, -3.0F);
        this.head.rotateAngleY = 0.0F;
        this.head.rotateAngleZ = fox.getInterestedAngle(partialTicks);
        this.legBackRight.showModel = true;
        this.legBackLeft.showModel = true;
        this.legFrontRight.showModel = true;
        this.legFrontLeft.showModel = true;
        this.body.setRotationPoint(0.0F, 16.0F, -6.0F);
        this.body.rotateAngleZ = 0.0F;
        this.legBackRight.setRotationPoint(-5.0F, 17.5F, 7.0F);
        this.legBackLeft.setRotationPoint(-1.0F, 17.5F, 7.0F);
        if (fox.isCrouching()) {
            this.body.rotateAngleX = 1.6755161F;
            float crouch = fox.getCrouchAmount(partialTicks);
            this.body.setRotationPoint(0.0F, 16.0F + crouch, -6.0F);
            this.head.setRotationPoint(-1.0F, 16.5F + crouch, -3.0F);
            this.head.rotateAngleY = 0.0F;
        } else if (fox.isSleeping()) {
            this.body.rotateAngleZ = -((float) Math.PI / 2F);
            this.body.setRotationPoint(0.0F, 21.0F, -6.0F);
            this.tail.rotateAngleX = -2.6179938F;
            if (this.isChild) {
                this.tail.rotateAngleX = -2.1816616F;
                this.body.setRotationPoint(0.0F, 21.0F, -2.0F);
            }
            this.head.setRotationPoint(1.0F, 19.49F, -3.0F);
            this.head.rotateAngleX = 0.0F;
            this.head.rotateAngleY = -2.0943952F;
            this.head.rotateAngleZ = 0.0F;
            this.legBackRight.showModel = false;
            this.legBackLeft.showModel = false;
            this.legFrontRight.showModel = false;
            this.legFrontLeft.showModel = false;
        } else if (fox.isSitting()) {
            this.body.rotateAngleX = ((float) Math.PI / 6F);
            this.body.setRotationPoint(0.0F, 9.0F, -3.0F);
            this.tail.rotateAngleX = ((float) Math.PI / 4F);
            this.tail.setRotationPoint(-4.0F, 15.0F, -2.0F);
            this.head.setRotationPoint(-1.0F, 10.0F, -0.25F);
            this.head.rotateAngleX = 0.0F;
            this.head.rotateAngleY = 0.0F;
            if (this.isChild) this.head.setRotationPoint(-1.0F, 13.0F, -3.75F);
            this.legBackRight.rotateAngleX = -1.3089969F;
            this.legBackRight.setRotationPoint(-5.0F, 21.5F, 6.75F);
            this.legBackLeft.rotateAngleX = -1.3089969F;
            this.legBackLeft.setRotationPoint(-1.0F, 21.5F, 6.75F);
            this.legFrontRight.rotateAngleX = -0.2617994F;
            this.legFrontLeft.rotateAngleX = -0.2617994F;
        }
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        EntityFox fox = (EntityFox) entity;
        if (!fox.isSleeping() && !fox.isStuck() && !fox.isCrouching()) {
            this.head.rotateAngleX = headPitch * 0.017453292F;
            this.head.rotateAngleY = netHeadYaw * 0.017453292F;
        }
        if (fox.isSleeping()) {
            this.head.rotateAngleX = 0.0F;
            this.head.rotateAngleY = -2.0943952F;
            this.head.rotateAngleZ = MathHelper.cos(ageInTicks * 0.027F) / 22.0F;
        }
        if (fox.isCrouching()) {
            float wobble = MathHelper.cos(ageInTicks) * 0.01F;
            this.body.rotateAngleY = wobble;
            this.legBackRight.rotateAngleZ = wobble;
            this.legBackLeft.rotateAngleZ = wobble;
            this.legFrontRight.rotateAngleZ = wobble / 2.0F;
            this.legFrontLeft.rotateAngleZ = wobble / 2.0F;
        }
        if (fox.isStuck()) {
            this.stuckTicks += 0.67F;
            this.legBackRight.rotateAngleX = MathHelper.cos(this.stuckTicks * 0.4662F) * 0.1F;
            this.legBackLeft.rotateAngleX = MathHelper.cos(this.stuckTicks * 0.4662F + (float) Math.PI) * 0.1F;
            this.legFrontRight.rotateAngleX = MathHelper.cos(this.stuckTicks * 0.4662F + (float) Math.PI) * 0.1F;
            this.legFrontLeft.rotateAngleX = MathHelper.cos(this.stuckTicks * 0.4662F) * 0.1F;
        }
    }
}
