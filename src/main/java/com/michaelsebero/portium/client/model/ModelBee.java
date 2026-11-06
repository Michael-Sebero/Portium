package com.michaelsebero.portium.client.model;

import com.michaelsebero.portium.entity.passive.bee.EntityBee;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;

/** 1.15's bee model: body with stinger and antennae, buzzing wings, three pairs of legs; rolls over when near its target. */
public class ModelBee extends ModelBase {

    private final ModelRenderer body;
    private final ModelRenderer torso;
    private final ModelRenderer rightWing;
    private final ModelRenderer leftWing;
    private final ModelRenderer frontLegs;
    private final ModelRenderer middleLegs;
    private final ModelRenderer backLegs;
    private final ModelRenderer stinger;
    private final ModelRenderer leftAntenna;
    private final ModelRenderer rightAntenna;
    private float bodyPitch;

    public ModelBee() {
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.body = new ModelRenderer(this);
        this.body.setRotationPoint(0.0F, 19.0F, 0.0F);
        this.torso = new ModelRenderer(this, 0, 0);
        this.body.addChild(this.torso);
        this.torso.addBox(-3.5F, -4.0F, -5.0F, 7, 7, 10, 0.0F);
        this.stinger = new ModelRenderer(this, 26, 7);
        this.stinger.addBox(0.0F, -1.0F, 5.0F, 0, 1, 2, 0.0F);
        this.torso.addChild(this.stinger);
        this.leftAntenna = new ModelRenderer(this, 2, 0);
        this.leftAntenna.setRotationPoint(0.0F, -2.0F, -5.0F);
        this.leftAntenna.addBox(1.5F, -2.0F, -3.0F, 1, 2, 3, 0.0F);
        this.rightAntenna = new ModelRenderer(this, 2, 3);
        this.rightAntenna.setRotationPoint(0.0F, -2.0F, -5.0F);
        this.rightAntenna.addBox(-2.5F, -2.0F, -3.0F, 1, 2, 3, 0.0F);
        this.torso.addChild(this.leftAntenna);
        this.torso.addChild(this.rightAntenna);
        this.rightWing = new ModelRenderer(this, 0, 18);
        this.rightWing.setRotationPoint(-1.5F, -4.0F, -3.0F);
        this.rightWing.rotateAngleY = -0.2618F;
        this.body.addChild(this.rightWing);
        this.rightWing.addBox(-9.0F, 0.0F, 0.0F, 9, 0, 6, 0.001F);
        this.leftWing = new ModelRenderer(this, 0, 18);
        this.leftWing.setRotationPoint(1.5F, -4.0F, -3.0F);
        this.leftWing.rotateAngleY = 0.2618F;
        this.leftWing.mirror = true;
        this.body.addChild(this.leftWing);
        this.leftWing.addBox(0.0F, 0.0F, 0.0F, 9, 0, 6, 0.001F);
        this.frontLegs = legs(-2.0F, 1);
        this.middleLegs = legs(0.0F, 3);
        this.backLegs = legs(2.0F, 5);
    }

    private ModelRenderer legs(float z, int v) {
        ModelRenderer legs = new ModelRenderer(this, 26, v);
        legs.setRotationPoint(1.5F, 3.0F, z);
        legs.addBox(-5.0F, 0.0F, 0.0F, 7, 2, 0, 0.0F);
        this.body.addChild(legs);
        return legs;
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks) {
        EntityBee bee = (EntityBee) entity;
        this.bodyPitch = bee.getBodyPitch(partialTicks);
        this.stinger.showModel = !bee.hasStung();
    }

    /** 1.15's {@code AgeableModel(false, 24, 0)}: a baby bee is half size, its head not scaled apart. */
    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        if (this.isChild) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.5F, 0.5F, 0.5F);
            GlStateManager.translate(0.0F, 24.0F * scale, 0.0F);
            this.body.render(scale);
            GlStateManager.popMatrix();
        } else {
            this.body.render(scale);
        }
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        EntityBee bee = (EntityBee) entity;
        this.rightWing.rotateAngleX = 0.0F;
        this.leftAntenna.rotateAngleX = 0.0F;
        this.rightAntenna.rotateAngleX = 0.0F;
        this.body.rotateAngleX = 0.0F;
        this.body.rotationPointY = 19.0F;
        boolean resting = bee.onGround && bee.motionX * bee.motionX + bee.motionY * bee.motionY + bee.motionZ * bee.motionZ < 1.0E-7D;
        if (resting) {
            this.rightWing.rotateAngleY = -0.2618F;
            this.rightWing.rotateAngleZ = 0.0F;
            this.leftWing.rotateAngleX = 0.0F;
            this.leftWing.rotateAngleY = 0.2618F;
            this.leftWing.rotateAngleZ = 0.0F;
            this.frontLegs.rotateAngleX = 0.0F;
            this.middleLegs.rotateAngleX = 0.0F;
            this.backLegs.rotateAngleX = 0.0F;
        } else {
            float flap = ageInTicks * 2.1F;
            this.rightWing.rotateAngleY = 0.0F;
            this.rightWing.rotateAngleZ = MathHelper.cos(flap) * (float) Math.PI * 0.15F;
            this.leftWing.rotateAngleX = this.rightWing.rotateAngleX;
            this.leftWing.rotateAngleY = this.rightWing.rotateAngleY;
            this.leftWing.rotateAngleZ = -this.rightWing.rotateAngleZ;
            this.frontLegs.rotateAngleX = 0.7853982F;
            this.middleLegs.rotateAngleX = 0.7853982F;
            this.backLegs.rotateAngleX = 0.7853982F;
            this.body.rotateAngleX = 0.0F;
            this.body.rotateAngleY = 0.0F;
            this.body.rotateAngleZ = 0.0F;
        }
        if (!bee.isAngry()) {
            this.body.rotateAngleX = 0.0F;
            this.body.rotateAngleY = 0.0F;
            this.body.rotateAngleZ = 0.0F;
            if (!resting) {
                float bob = MathHelper.cos(ageInTicks * 0.18F);
                this.body.rotateAngleX = 0.1F + bob * (float) Math.PI * 0.025F;
                this.leftAntenna.rotateAngleX = bob * (float) Math.PI * 0.03F;
                this.rightAntenna.rotateAngleX = bob * (float) Math.PI * 0.03F;
                this.frontLegs.rotateAngleX = -bob * (float) Math.PI * 0.1F + 0.3926991F;
                this.backLegs.rotateAngleX = -bob * (float) Math.PI * 0.05F + 0.7853982F;
                this.body.rotationPointY = 19.0F - MathHelper.cos(ageInTicks * 0.18F) * 0.9F;
            }
        }
        if (this.bodyPitch > 0.0F) this.body.rotateAngleX = rotLerp(this.body.rotateAngleX, 3.0915928F, this.bodyPitch);
    }

    /** 1.15's {@code ModelUtils.func_228283_a_}: angle interpolation the short way round. */
    private static float rotLerp(float from, float to, float progress) {
        float delta = to - from;
        while (delta < -(float) Math.PI) delta += (float) (Math.PI * 2);
        while (delta >= (float) Math.PI) delta -= (float) (Math.PI * 2);
        return from + progress * delta;
    }
}
