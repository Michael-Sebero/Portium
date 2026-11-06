package com.michaelsebero.portium.client.model;

import com.michaelsebero.portium.client.CrossbowPoses;
import com.michaelsebero.portium.entity.monster.IllagerArmPose116;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;

import java.util.function.Function;

/**
 * 1.16's {@code IllagerModel}: 1.12's illager shape, with arms that swing as it walks, a riding pose, and the crossbow,
 * celebrating and neutral arm poses. The pose comes from the renderer, since 1.12's illagers don't have 1.14's.
 */
public class ModelIllager116 extends ModelBase {

    public final ModelRenderer head;
    public final ModelRenderer hat;
    public final ModelRenderer body;
    public final ModelRenderer arms;
    public final ModelRenderer rightLeg;
    public final ModelRenderer leftLeg;
    public final ModelRenderer rightArm;
    public final ModelRenderer leftArm;
    private final Function<EntityLivingBase, IllagerArmPose116> poses;

    public ModelIllager116(float scale, float yOffset, int textureWidth, int textureHeight, Function<EntityLivingBase, IllagerArmPose116> poses) {
        this.poses = poses;
        this.head = new ModelRenderer(this).setTextureSize(textureWidth, textureHeight);
        this.head.setRotationPoint(0.0F, 0.0F + yOffset, 0.0F);
        this.head.setTextureOffset(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8, 10, 8, scale);
        this.hat = new ModelRenderer(this, 32, 0).setTextureSize(textureWidth, textureHeight);
        this.hat.addBox(-4.0F, -10.0F, -4.0F, 8, 12, 8, scale + 0.45F);
        this.head.addChild(this.hat);
        this.hat.showModel = false;
        ModelRenderer nose = new ModelRenderer(this).setTextureSize(textureWidth, textureHeight);
        nose.setRotationPoint(0.0F, yOffset - 2.0F, 0.0F);
        nose.setTextureOffset(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2, 4, 2, scale);
        this.head.addChild(nose);
        this.body = new ModelRenderer(this).setTextureSize(textureWidth, textureHeight);
        this.body.setRotationPoint(0.0F, 0.0F + yOffset, 0.0F);
        this.body.setTextureOffset(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8, 12, 6, scale);
        this.body.setTextureOffset(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8, 18, 6, scale + 0.5F);
        this.arms = new ModelRenderer(this).setTextureSize(textureWidth, textureHeight);
        this.arms.setRotationPoint(0.0F, 0.0F + yOffset + 2.0F, 0.0F);
        this.arms.setTextureOffset(44, 22).addBox(-8.0F, -2.0F, -2.0F, 4, 8, 4, scale);
        ModelRenderer leftHand = new ModelRenderer(this, 44, 22).setTextureSize(textureWidth, textureHeight);
        leftHand.mirror = true;
        leftHand.addBox(4.0F, -2.0F, -2.0F, 4, 8, 4, scale);
        this.arms.addChild(leftHand);
        this.arms.setTextureOffset(40, 38).addBox(-4.0F, 2.0F, -2.0F, 8, 4, 4, scale);
        this.rightLeg = new ModelRenderer(this, 0, 22).setTextureSize(textureWidth, textureHeight);
        this.rightLeg.setRotationPoint(-2.0F, 12.0F + yOffset, 0.0F);
        this.rightLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, scale);
        this.leftLeg = new ModelRenderer(this, 0, 22).setTextureSize(textureWidth, textureHeight);
        this.leftLeg.mirror = true;
        this.leftLeg.setRotationPoint(2.0F, 12.0F + yOffset, 0.0F);
        this.leftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, scale);
        this.rightArm = new ModelRenderer(this, 40, 46).setTextureSize(textureWidth, textureHeight);
        this.rightArm.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, scale);
        this.rightArm.setRotationPoint(-5.0F, 2.0F + yOffset, 0.0F);
        this.leftArm = new ModelRenderer(this, 40, 46).setTextureSize(textureWidth, textureHeight);
        this.leftArm.mirror = true;
        this.leftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, scale);
        this.leftArm.setRotationPoint(5.0F, 2.0F + yOffset, 0.0F);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.head.render(scale);
        this.body.render(scale);
        this.rightLeg.render(scale);
        this.leftLeg.render(scale);
        this.arms.render(scale);
        this.rightArm.render(scale);
        this.leftArm.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        EntityLivingBase illager = (EntityLivingBase) entity;
        this.head.rotateAngleY = netHeadYaw * 0.017453292F;
        this.head.rotateAngleX = headPitch * 0.017453292F;
        this.arms.rotationPointY = 3.0F;
        this.arms.rotationPointZ = -1.0F;
        this.arms.rotateAngleX = -0.75F;
        if (this.isRiding) {
            this.rightArm.rotateAngleX = -0.62831855F;
            this.rightArm.rotateAngleY = 0.0F;
            this.rightArm.rotateAngleZ = 0.0F;
            this.leftArm.rotateAngleX = -0.62831855F;
            this.leftArm.rotateAngleY = 0.0F;
            this.leftArm.rotateAngleZ = 0.0F;
            this.rightLeg.rotateAngleX = -1.4137167F;
            this.rightLeg.rotateAngleY = 0.31415927F;
            this.rightLeg.rotateAngleZ = 0.07853982F;
            this.leftLeg.rotateAngleX = -1.4137167F;
            this.leftLeg.rotateAngleY = -0.31415927F;
            this.leftLeg.rotateAngleZ = -0.07853982F;
        } else {
            this.rightArm.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F;
            this.rightArm.rotateAngleY = 0.0F;
            this.rightArm.rotateAngleZ = 0.0F;
            this.leftArm.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
            this.leftArm.rotateAngleY = 0.0F;
            this.leftArm.rotateAngleZ = 0.0F;
            this.rightLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount * 0.5F;
            this.rightLeg.rotateAngleY = 0.0F;
            this.rightLeg.rotateAngleZ = 0.0F;
            this.leftLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount * 0.5F;
            this.leftLeg.rotateAngleY = 0.0F;
            this.leftLeg.rotateAngleZ = 0.0F;
        }
        IllagerArmPose116 pose = this.poses.apply(illager);
        if (pose == IllagerArmPose116.ATTACKING) {
            if (illager.getHeldItemMainhand().isEmpty()) ModelHelper116.armsForward(this.leftArm, this.rightArm, true, this.swingProgress, ageInTicks);
            else ModelHelper116.attackWithWeapon(this.rightArm, this.leftArm, illager, this.swingProgress, ageInTicks);
        } else if (pose == IllagerArmPose116.SPELLCASTING) {
            this.rightArm.rotationPointZ = 0.0F;
            this.rightArm.rotationPointX = -5.0F;
            this.leftArm.rotationPointZ = 0.0F;
            this.leftArm.rotationPointX = 5.0F;
            this.rightArm.rotateAngleX = MathHelper.cos(ageInTicks * 0.6662F) * 0.25F;
            this.leftArm.rotateAngleX = MathHelper.cos(ageInTicks * 0.6662F) * 0.25F;
            this.rightArm.rotateAngleZ = 2.3561945F;
            this.leftArm.rotateAngleZ = -2.3561945F;
            this.rightArm.rotateAngleY = 0.0F;
            this.leftArm.rotateAngleY = 0.0F;
        } else if (pose == IllagerArmPose116.BOW_AND_ARROW) {
            this.rightArm.rotateAngleY = -0.1F + this.head.rotateAngleY;
            this.rightArm.rotateAngleX = -((float) Math.PI / 2F) + this.head.rotateAngleX;
            this.leftArm.rotateAngleX = -0.9424779F + this.head.rotateAngleX;
            this.leftArm.rotateAngleY = this.head.rotateAngleY - 0.4F;
            this.leftArm.rotateAngleZ = ((float) Math.PI / 2F);
        } else if (pose == IllagerArmPose116.CROSSBOW_HOLD) {
            CrossbowPoses.hold(this.rightArm, this.leftArm, this.head, true);
        } else if (pose == IllagerArmPose116.CROSSBOW_CHARGE) {
            CrossbowPoses.charge(this.rightArm, this.leftArm, illager, true);
        } else if (pose == IllagerArmPose116.CELEBRATING) {
            this.rightArm.rotationPointZ = 0.0F;
            this.rightArm.rotationPointX = -5.0F;
            this.rightArm.rotateAngleX = MathHelper.cos(ageInTicks * 0.6662F) * 0.05F;
            this.rightArm.rotateAngleZ = 2.670354F;
            this.rightArm.rotateAngleY = 0.0F;
            this.leftArm.rotationPointZ = 0.0F;
            this.leftArm.rotationPointX = 5.0F;
            this.leftArm.rotateAngleX = MathHelper.cos(ageInTicks * 0.6662F) * 0.05F;
            this.leftArm.rotateAngleZ = -2.3561945F;
            this.leftArm.rotateAngleY = 0.0F;
        }
        boolean crossed = pose == IllagerArmPose116.CROSSED;
        this.arms.showModel = crossed;
        this.leftArm.showModel = !crossed;
        this.rightArm.showModel = !crossed;
    }

    public ModelRenderer getArm(EnumHandSide side) {
        return side == EnumHandSide.LEFT ? this.leftArm : this.rightArm;
    }
}
