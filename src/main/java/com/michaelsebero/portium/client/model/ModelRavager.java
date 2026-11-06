package com.michaelsebero.portium.client.model;

import com.michaelsebero.portium.entity.monster.EntityRavager;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;

/** 1.16's ravager model: the head on a neck that lunges when it bites, sways when stunned, and opens its jaw to roar. */
public class ModelRavager extends ModelBase {

    private final ModelRenderer head;
    private final ModelRenderer jaw;
    private final ModelRenderer body;
    private final ModelRenderer legBackRight;
    private final ModelRenderer legBackLeft;
    private final ModelRenderer legFrontRight;
    private final ModelRenderer legFrontLeft;
    private final ModelRenderer neck;

    public ModelRavager() {
        this.textureWidth = 128;
        this.textureHeight = 128;
        this.neck = new ModelRenderer(this);
        this.neck.setRotationPoint(0.0F, -7.0F, -1.5F);
        this.neck.setTextureOffset(68, 73).addBox(-5.0F, -1.0F, -18.0F, 10, 10, 18, 0.0F);
        this.head = new ModelRenderer(this);
        this.head.setRotationPoint(0.0F, 16.0F, -17.0F);
        this.head.setTextureOffset(0, 0).addBox(-8.0F, -20.0F, -14.0F, 16, 20, 16, 0.0F);
        this.head.setTextureOffset(0, 0).addBox(-2.0F, -6.0F, -18.0F, 4, 8, 4, 0.0F);
        ModelRenderer rightHorn = new ModelRenderer(this);
        rightHorn.setRotationPoint(-10.0F, -14.0F, -8.0F);
        rightHorn.setTextureOffset(74, 55).addBox(0.0F, -14.0F, -2.0F, 2, 14, 4, 0.0F);
        rightHorn.rotateAngleX = 1.0995574F;
        this.head.addChild(rightHorn);
        ModelRenderer leftHorn = new ModelRenderer(this);
        leftHorn.mirror = true;
        leftHorn.setRotationPoint(8.0F, -14.0F, -8.0F);
        leftHorn.setTextureOffset(74, 55).addBox(0.0F, -14.0F, -2.0F, 2, 14, 4, 0.0F);
        leftHorn.rotateAngleX = 1.0995574F;
        this.head.addChild(leftHorn);
        this.jaw = new ModelRenderer(this);
        this.jaw.setRotationPoint(0.0F, -2.0F, 2.0F);
        this.jaw.setTextureOffset(0, 36).addBox(-8.0F, 0.0F, -16.0F, 16, 3, 16, 0.0F);
        this.head.addChild(this.jaw);
        this.neck.addChild(this.head);
        this.body = new ModelRenderer(this);
        this.body.setTextureOffset(0, 55).addBox(-7.0F, -10.0F, -7.0F, 14, 16, 20, 0.0F);
        this.body.setTextureOffset(0, 91).addBox(-6.0F, 6.0F, -7.0F, 12, 13, 18, 0.0F);
        this.body.setRotationPoint(0.0F, 1.0F, 2.0F);
        this.legBackRight = new ModelRenderer(this, 96, 0);
        this.legBackRight.addBox(-4.0F, 0.0F, -4.0F, 8, 37, 8, 0.0F);
        this.legBackRight.setRotationPoint(-8.0F, -13.0F, 18.0F);
        this.legBackLeft = new ModelRenderer(this, 96, 0);
        this.legBackLeft.mirror = true;
        this.legBackLeft.addBox(-4.0F, 0.0F, -4.0F, 8, 37, 8, 0.0F);
        this.legBackLeft.setRotationPoint(8.0F, -13.0F, 18.0F);
        this.legFrontRight = new ModelRenderer(this, 64, 0);
        this.legFrontRight.addBox(-4.0F, 0.0F, -4.0F, 8, 37, 8, 0.0F);
        this.legFrontRight.setRotationPoint(-8.0F, -13.0F, -5.0F);
        this.legFrontLeft = new ModelRenderer(this, 64, 0);
        this.legFrontLeft.mirror = true;
        this.legFrontLeft.addBox(-4.0F, 0.0F, -4.0F, 8, 37, 8, 0.0F);
        this.legFrontLeft.setRotationPoint(8.0F, -13.0F, -5.0F);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.neck.render(scale);
        this.body.render(scale);
        this.legBackRight.render(scale);
        this.legBackLeft.render(scale);
        this.legFrontRight.render(scale);
        this.legFrontLeft.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        this.head.rotateAngleX = headPitch * 0.017453292F;
        this.head.rotateAngleY = netHeadYaw * 0.017453292F;
        this.body.rotateAngleX = ((float) Math.PI / 2F);
        float swing = 0.4F * limbSwingAmount;
        this.legBackRight.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * swing;
        this.legBackLeft.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * swing;
        this.legFrontRight.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * swing;
        this.legFrontLeft.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * swing;
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks) {
        super.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
        EntityRavager ravager = (EntityRavager) entity;
        int stunTick = ravager.getStunTick();
        int roarTick = ravager.getRoarTick();
        int attackTick = ravager.getAttackTick();
        if (attackTick > 0) {
            float wave = triangleWave((float) attackTick - partialTicks, 10.0F);
            float lunge = (1.0F + wave) * 0.5F;
            float reach = lunge * lunge * lunge * 12.0F;
            float rise = reach * MathHelper.sin(this.neck.rotateAngleX);
            this.neck.rotationPointZ = -6.5F + reach;
            this.neck.rotationPointY = -7.0F - rise;
            if (attackTick > 5) {
                this.jaw.rotateAngleX = MathHelper.sin(((float) (-4 + attackTick) - partialTicks) / 4.0F) * (float) Math.PI * 0.4F;
            } else {
                this.jaw.rotateAngleX = 0.15707964F * MathHelper.sin((float) Math.PI * ((float) attackTick - partialTicks) / 10.0F);
            }
        } else {
            float rise = -1.0F * MathHelper.sin(this.neck.rotateAngleX);
            this.neck.rotationPointX = 0.0F;
            this.neck.rotationPointY = -7.0F - rise;
            this.neck.rotationPointZ = 5.5F;
            boolean stunned = stunTick > 0;
            this.neck.rotateAngleX = stunned ? 0.21991149F : 0.0F;
            this.jaw.rotateAngleX = (float) Math.PI * (stunned ? 0.05F : 0.01F);
            if (stunned) {
                double progress = (double) stunTick / 40.0D;
                this.neck.rotationPointX = (float) Math.sin(progress * 10.0D) * 3.0F;
            } else if (roarTick > 0) {
                float open = MathHelper.sin(((float) (20 - roarTick) - partialTicks) / 20.0F * (float) Math.PI * 0.25F);
                this.jaw.rotateAngleX = ((float) Math.PI / 2F) * open;
            }
        }
    }

    /** 1.16's {@code MathHelper} triangle wave. */
    private static float triangleWave(float value, float period) {
        return (Math.abs(value % period - period * 0.5F) - period * 0.25F) / (period * 0.25F);
    }
}
