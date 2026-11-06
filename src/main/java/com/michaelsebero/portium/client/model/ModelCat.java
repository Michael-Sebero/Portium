package com.michaelsebero.portium.client.model;

import com.michaelsebero.portium.entity.passive.EntityCat;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;

/**
 * The cat (1.14's CatModel on its OcelotModel): sneaking, sprinting, sitting, lying on its side by its owner, and
 * relaxed. {@code inflate} makes the collar's slightly larger copy.
 */
public class ModelCat extends ModelBase {

    private final ModelRenderer backLeftLeg;
    private final ModelRenderer backRightLeg;
    private final ModelRenderer frontLeftLeg;
    private final ModelRenderer frontRightLeg;
    private final ModelRenderer tail;
    private final ModelRenderer tail2;
    private final ModelRenderer head;
    private final ModelRenderer body;
    private int state = 1;
    private float lieDown;
    private float lieDownTail;
    private float relax;

    public ModelCat(float inflate) {
        this.textureWidth = 64;
        this.textureHeight = 32;
        this.head = new ModelRenderer(this, 0, 0);
        this.head.addBox(-2.5F, -2.0F, -3.0F, 5, 4, 5, inflate);
        this.head.setTextureOffset(0, 24).addBox(-1.5F, 0.0F, -4.0F, 3, 2, 2, inflate);
        this.head.setTextureOffset(0, 10).addBox(-2.0F, -3.0F, 0.0F, 1, 1, 2, inflate);
        this.head.setTextureOffset(6, 10).addBox(1.0F, -3.0F, 0.0F, 1, 1, 2, inflate);
        this.head.setRotationPoint(0.0F, 15.0F, -9.0F);
        this.body = new ModelRenderer(this, 20, 0);
        this.body.addBox(-2.0F, 3.0F, -8.0F, 4, 16, 6, inflate);
        this.body.setRotationPoint(0.0F, 12.0F, -10.0F);
        this.tail = new ModelRenderer(this, 0, 15);
        this.tail.addBox(-0.5F, 0.0F, 0.0F, 1, 8, 1, inflate);
        this.tail.rotateAngleX = 0.9F;
        this.tail.setRotationPoint(0.0F, 15.0F, 8.0F);
        this.tail2 = new ModelRenderer(this, 4, 15);
        this.tail2.addBox(-0.5F, 0.0F, 0.0F, 1, 8, 1, inflate);
        this.tail2.setRotationPoint(0.0F, 20.0F, 14.0F);
        this.backLeftLeg = new ModelRenderer(this, 8, 13);
        this.backLeftLeg.addBox(-1.0F, 0.0F, 1.0F, 2, 6, 2, inflate);
        this.backLeftLeg.setRotationPoint(1.1F, 18.0F, 5.0F);
        this.backRightLeg = new ModelRenderer(this, 8, 13);
        this.backRightLeg.addBox(-1.0F, 0.0F, 1.0F, 2, 6, 2, inflate);
        this.backRightLeg.setRotationPoint(-1.1F, 18.0F, 5.0F);
        this.frontLeftLeg = new ModelRenderer(this, 40, 0);
        this.frontLeftLeg.addBox(-1.0F, 0.0F, 0.0F, 2, 10, 2, inflate);
        this.frontLeftLeg.setRotationPoint(1.2F, 14.1F, -5.0F);
        this.frontRightLeg = new ModelRenderer(this, 40, 0);
        this.frontRightLeg.addBox(-1.0F, 0.0F, 0.0F, 2, 10, 2, inflate);
        this.frontRightLeg.setRotationPoint(-1.2F, 14.1F, -5.0F);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        if (this.isChild) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.75F, 0.75F, 0.75F);
            GlStateManager.translate(0.0F, 10.0F * scale, 4.0F * scale);
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
        this.backLeftLeg.render(scale);
        this.backRightLeg.render(scale);
        this.frontLeftLeg.render(scale);
        this.frontRightLeg.render(scale);
        this.tail.render(scale);
        this.tail2.render(scale);
    }

    private static float turn(float from, float to, float amount) {
        float difference = to - from;
        while (difference < -(float) Math.PI) difference += ((float) Math.PI * 2F);
        while (difference >= (float) Math.PI) difference -= ((float) Math.PI * 2F);
        return from + amount * difference;
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks) {
        EntityCat cat = (EntityCat) entity;
        this.lieDown = cat.getLieDownAmount(partialTicks);
        this.lieDownTail = cat.getLieDownAmountTail(partialTicks);
        this.relax = cat.getRelaxStateOneAmount(partialTicks);
        if (this.lieDown <= 0.0F) {
            this.head.rotateAngleX = 0.0F;
            this.head.rotateAngleZ = 0.0F;
            this.frontLeftLeg.rotateAngleX = 0.0F;
            this.frontLeftLeg.rotateAngleZ = 0.0F;
            this.frontRightLeg.rotateAngleX = 0.0F;
            this.frontRightLeg.rotateAngleZ = 0.0F;
            this.frontRightLeg.rotationPointX = -1.2F;
            this.backLeftLeg.rotateAngleX = 0.0F;
            this.backRightLeg.rotateAngleX = 0.0F;
            this.backRightLeg.rotateAngleZ = 0.0F;
            this.backRightLeg.rotationPointX = -1.1F;
            this.backRightLeg.rotationPointY = 18.0F;
        }
        // 1.14's OcelotModel.
        this.body.rotationPointY = 12.0F;
        this.body.rotationPointZ = -10.0F;
        this.head.rotationPointY = 15.0F;
        this.head.rotationPointZ = -9.0F;
        this.tail.rotationPointY = 15.0F;
        this.tail.rotationPointZ = 8.0F;
        this.tail2.rotationPointY = 20.0F;
        this.tail2.rotationPointZ = 14.0F;
        this.frontLeftLeg.rotationPointY = 14.1F;
        this.frontLeftLeg.rotationPointZ = -5.0F;
        this.frontRightLeg.rotationPointY = 14.1F;
        this.frontRightLeg.rotationPointZ = -5.0F;
        this.backLeftLeg.rotationPointY = 18.0F;
        this.backLeftLeg.rotationPointZ = 5.0F;
        this.backRightLeg.rotationPointY = 18.0F;
        this.backRightLeg.rotationPointZ = 5.0F;
        this.tail.rotateAngleX = 0.9F;
        if (cat.isSneaking()) {
            ++this.body.rotationPointY;
            this.head.rotationPointY += 2.0F;
            ++this.tail.rotationPointY;
            this.tail2.rotationPointY += -4.0F;
            this.tail2.rotationPointZ += 2.0F;
            this.tail.rotateAngleX = ((float) Math.PI / 2F);
            this.tail2.rotateAngleX = ((float) Math.PI / 2F);
            this.state = 0;
        } else if (cat.isSprinting()) {
            this.tail2.rotationPointY = this.tail.rotationPointY;
            this.tail2.rotationPointZ += 2.0F;
            this.tail.rotateAngleX = ((float) Math.PI / 2F);
            this.tail2.rotateAngleX = ((float) Math.PI / 2F);
            this.state = 2;
        } else {
            this.state = 1;
        }
        // 1.14's CatModel: sitting.
        if (cat.isSitting()) {
            this.body.rotateAngleX = ((float) Math.PI / 4F);
            this.body.rotationPointY += -4.0F;
            this.body.rotationPointZ += 5.0F;
            this.head.rotationPointY += -3.3F;
            ++this.head.rotationPointZ;
            this.tail.rotationPointY += 8.0F;
            this.tail.rotationPointZ += -2.0F;
            this.tail2.rotationPointY += 2.0F;
            this.tail2.rotationPointZ += -0.8F;
            this.tail.rotateAngleX = 1.7278761F;
            this.tail2.rotateAngleX = 2.670354F;
            this.frontLeftLeg.rotateAngleX = -0.15707964F;
            this.frontLeftLeg.rotationPointY = 16.1F;
            this.frontLeftLeg.rotationPointZ = -7.0F;
            this.frontRightLeg.rotateAngleX = -0.15707964F;
            this.frontRightLeg.rotationPointY = 16.1F;
            this.frontRightLeg.rotationPointZ = -7.0F;
            this.backLeftLeg.rotateAngleX = (-(float) Math.PI / 2F);
            this.backLeftLeg.rotationPointY = 21.0F;
            this.backLeftLeg.rotationPointZ = 1.0F;
            this.backRightLeg.rotateAngleX = (-(float) Math.PI / 2F);
            this.backRightLeg.rotationPointY = 21.0F;
            this.backRightLeg.rotationPointZ = 1.0F;
            this.state = 3;
        }
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        this.head.rotateAngleX = headPitch * 0.017453292F;
        this.head.rotateAngleY = netHeadYaw * 0.017453292F;
        if (this.state != 3) {
            this.body.rotateAngleX = ((float) Math.PI / 2F);
            if (this.state == 2) {
                this.backLeftLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * limbSwingAmount;
                this.backRightLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + 0.3F) * limbSwingAmount;
                this.frontLeftLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI + 0.3F) * limbSwingAmount;
                this.frontRightLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount;
                this.tail2.rotateAngleX = 1.7278761F + ((float) Math.PI / 10F) * MathHelper.cos(limbSwing) * limbSwingAmount;
            } else {
                this.backLeftLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * limbSwingAmount;
                this.backRightLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount;
                this.frontLeftLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount;
                this.frontRightLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * limbSwingAmount;
                if (this.state == 1) this.tail2.rotateAngleX = 1.7278761F + ((float) Math.PI / 4F) * MathHelper.cos(limbSwing) * limbSwingAmount;
                else this.tail2.rotateAngleX = 1.7278761F + 0.47123894F * MathHelper.cos(limbSwing) * limbSwingAmount;
            }
        }
        if (this.lieDown > 0.0F) {
            this.head.rotateAngleZ = turn(this.head.rotateAngleZ, -1.2707963F, this.lieDown);
            this.head.rotateAngleY = turn(this.head.rotateAngleY, 1.2707963F, this.lieDown);
            this.frontLeftLeg.rotateAngleX = -1.2707963F;
            this.frontRightLeg.rotateAngleX = -0.47079635F;
            this.frontRightLeg.rotateAngleZ = -0.2F;
            this.frontRightLeg.rotationPointX = -0.2F;
            this.backLeftLeg.rotateAngleX = -0.4F;
            this.backRightLeg.rotateAngleX = 0.5F;
            this.backRightLeg.rotateAngleZ = -0.5F;
            this.backRightLeg.rotationPointX = -0.3F;
            this.backRightLeg.rotationPointY = 20.0F;
            this.tail.rotateAngleX = turn(this.tail.rotateAngleX, 0.8F, this.lieDownTail);
            this.tail2.rotateAngleX = turn(this.tail2.rotateAngleX, -0.4F, this.lieDownTail);
        }
        if (this.relax > 0.0F) this.head.rotateAngleX = turn(this.head.rotateAngleX, -0.58177644F, this.relax);
    }
}
