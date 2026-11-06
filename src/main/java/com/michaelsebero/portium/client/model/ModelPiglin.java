package com.michaelsebero.portium.client.model;

import com.michaelsebero.portium.client.CrossbowPoses;
import com.michaelsebero.portium.entity.monster.piglin.EntityAbstractPiglin;
import com.michaelsebero.portium.entity.monster.piglin.PiglinAction;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;

/**
 * 1.16's piglin model: the player model with a wide snouted head and two flapping ears, and the piglin's poses
 * (dancing, admiring gold, swinging a weapon, loading and aiming a crossbow).
 */
public class ModelPiglin extends ModelPlayer {

    public final ModelRenderer rightEar;
    public final ModelRenderer leftEar;
    private final float[] bodyDefaults;
    private final float[] headDefaults;
    private final float[] leftArmDefaults;
    private final float[] rightArmDefaults;

    public ModelPiglin(float modelSize) {
        super(modelSize, false);
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.bipedBody = new ModelRenderer(this, 16, 16);
        this.bipedBody.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, modelSize);
        this.bipedHead = new ModelRenderer(this);
        this.bipedHead.setTextureOffset(0, 0).addBox(-5.0F, -8.0F, -4.0F, 10, 8, 8, modelSize);
        this.bipedHead.setTextureOffset(31, 1).addBox(-2.0F, -4.0F, -5.0F, 4, 4, 1, modelSize);
        this.bipedHead.setTextureOffset(2, 4).addBox(2.0F, -2.0F, -5.0F, 1, 2, 1, modelSize);
        this.bipedHead.setTextureOffset(2, 0).addBox(-3.0F, -2.0F, -5.0F, 1, 2, 1, modelSize);
        this.rightEar = new ModelRenderer(this);
        this.rightEar.setRotationPoint(4.5F, -6.0F, 0.0F);
        this.rightEar.setTextureOffset(51, 6).addBox(0.0F, 0.0F, -2.0F, 1, 5, 4, modelSize);
        this.bipedHead.addChild(this.rightEar);
        this.leftEar = new ModelRenderer(this);
        this.leftEar.setRotationPoint(-4.5F, -6.0F, 0.0F);
        this.leftEar.setTextureOffset(39, 6).addBox(-1.0F, 0.0F, -2.0F, 1, 5, 4, modelSize);
        this.bipedHead.addChild(this.leftEar);
        this.bipedHeadwear = new ModelRenderer(this);
        this.bodyDefaults = pose(this.bipedBody);
        this.headDefaults = pose(this.bipedHead);
        this.leftArmDefaults = pose(this.bipedLeftArm);
        this.rightArmDefaults = pose(this.bipedRightArm);
    }

    private static float[] pose(ModelRenderer part) {
        return new float[]{part.rotationPointX, part.rotationPointY, part.rotationPointZ, part.rotateAngleX, part.rotateAngleY, part.rotateAngleZ};
    }

    private static void restore(ModelRenderer part, float[] pose) {
        part.rotationPointX = pose[0];
        part.rotationPointY = pose[1];
        part.rotationPointZ = pose[2];
        part.rotateAngleX = pose[3];
        part.rotateAngleY = pose[4];
        part.rotateAngleZ = pose[5];
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        restore(this.bipedBody, this.bodyDefaults);
        restore(this.bipedHead, this.headDefaults);
        restore(this.bipedLeftArm, this.leftArmDefaults);
        restore(this.bipedRightArm, this.rightArmDefaults);
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        float phase = ageInTicks * 0.1F + limbSwing * 0.5F;
        float flap = 0.08F + limbSwingAmount * 0.4F;
        this.rightEar.rotateAngleZ = -0.5235988F - MathHelper.cos(phase * 1.2F) * flap;
        this.leftEar.rotateAngleZ = 0.5235988F + MathHelper.cos(phase) * flap;
        if (entity instanceof EntityAbstractPiglin) {
            EntityAbstractPiglin piglin = (EntityAbstractPiglin) entity;
            boolean rightHanded = ((EntityLivingBase) entity).getPrimaryHand() == EnumHandSide.RIGHT;
            PiglinAction action = piglin.getAction();
            if (action == PiglinAction.DANCING) {
                float dance = ageInTicks / 60.0F;
                this.leftEar.rotateAngleZ = 0.5235988F + 0.017453292F * MathHelper.sin(dance * 30.0F) * 10.0F;
                this.rightEar.rotateAngleZ = -0.5235988F - 0.017453292F * MathHelper.cos(dance * 30.0F) * 10.0F;
                this.bipedHead.rotationPointX = MathHelper.sin(dance * 10.0F);
                this.bipedHead.rotationPointY = MathHelper.sin(dance * 40.0F) + 0.4F;
                this.bipedRightArm.rotateAngleZ = 0.017453292F * (70.0F + MathHelper.cos(dance * 40.0F) * 10.0F);
                this.bipedLeftArm.rotateAngleZ = this.bipedRightArm.rotateAngleZ * -1.0F;
                this.bipedRightArm.rotationPointY = MathHelper.sin(dance * 40.0F) * 0.5F + 1.5F;
                this.bipedLeftArm.rotationPointY = MathHelper.sin(dance * 40.0F) * 0.5F + 1.5F;
                this.bipedBody.rotationPointY = MathHelper.sin(dance * 40.0F) * 0.35F;
            } else if (action == PiglinAction.ATTACKING_WITH_MELEE_WEAPON) {
                if (this.swingProgress > 0.0F) {
                    ModelHelper116.attackWithWeapon(this.bipedRightArm, this.bipedLeftArm, (EntityLivingBase) entity, this.swingProgress, ageInTicks);
                } else if (rightHanded) {
                    this.bipedRightArm.rotateAngleX = -1.8F;
                } else {
                    this.bipedLeftArm.rotateAngleX = -1.8F;
                }
            } else if (action == PiglinAction.CROSSBOW_HOLD) {
                CrossbowPoses.hold(this.bipedRightArm, this.bipedLeftArm, this.bipedHead, rightHanded);
            } else if (action == PiglinAction.CROSSBOW_CHARGE) {
                CrossbowPoses.charge(this.bipedRightArm, this.bipedLeftArm, (EntityLivingBase) entity, rightHanded);
            } else if (action == PiglinAction.ADMIRING_ITEM) {
                this.bipedHead.rotateAngleX = 0.5F;
                this.bipedHead.rotateAngleY = 0.0F;
                if (!rightHanded) {
                    this.bipedRightArm.rotateAngleY = -0.5F;
                    this.bipedRightArm.rotateAngleX = -0.9F;
                } else {
                    this.bipedLeftArm.rotateAngleY = 0.5F;
                    this.bipedLeftArm.rotateAngleX = -0.9F;
                }
            }
        } else if (entity instanceof EntityLiving) {
            ModelHelper116.armsForward(this.bipedLeftArm, this.bipedRightArm, ((EntityLiving) entity).getAttackTarget() != null, this.swingProgress, ageInTicks);
        }
        copyModelAngles(this.bipedLeftLeg, this.bipedLeftLegwear);
        copyModelAngles(this.bipedRightLeg, this.bipedRightLegwear);
        copyModelAngles(this.bipedLeftArm, this.bipedLeftArmwear);
        copyModelAngles(this.bipedRightArm, this.bipedRightArmwear);
        copyModelAngles(this.bipedBody, this.bipedBodyWear);
        copyModelAngles(this.bipedHead, this.bipedHeadwear);
    }
}
