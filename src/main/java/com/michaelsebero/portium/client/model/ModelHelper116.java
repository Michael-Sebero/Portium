package com.michaelsebero.portium.client.model;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;

/** 1.16's {@code ModelHelper} arm poses for mobs. */
public final class ModelHelper116 {

    /** Swinging a held weapon (illagers, piglins). */
    public static void attackWithWeapon(ModelRenderer rightArm, ModelRenderer leftArm, EntityLivingBase entity, float swingProgress, float ageInTicks) {
        float swing = MathHelper.sin(swingProgress * (float) Math.PI);
        float swingEased = MathHelper.sin((1.0F - (1.0F - swingProgress) * (1.0F - swingProgress)) * (float) Math.PI);
        rightArm.rotateAngleZ = 0.0F;
        leftArm.rotateAngleZ = 0.0F;
        rightArm.rotateAngleY = 0.15707964F;
        leftArm.rotateAngleY = -0.15707964F;
        if (entity.getPrimaryHand() == EnumHandSide.RIGHT) {
            rightArm.rotateAngleX = -1.8849558F + MathHelper.cos(ageInTicks * 0.09F) * 0.15F;
            leftArm.rotateAngleX = -0.0F + MathHelper.cos(ageInTicks * 0.19F) * 0.5F;
            rightArm.rotateAngleX += swing * 2.2F - swingEased * 0.4F;
            leftArm.rotateAngleX += swing * 1.2F - swingEased * 0.4F;
        } else {
            rightArm.rotateAngleX = -0.0F + MathHelper.cos(ageInTicks * 0.19F) * 0.5F;
            leftArm.rotateAngleX = -1.8849558F + MathHelper.cos(ageInTicks * 0.09F) * 0.15F;
            rightArm.rotateAngleX += swing * 1.2F - swingEased * 0.4F;
            leftArm.rotateAngleX += swing * 2.2F - swingEased * 0.4F;
        }
        bobArms(rightArm, leftArm, ageInTicks);
    }

    /** Arms out in front, zombie-like (an attacking illager with empty hands). */
    public static void armsForward(ModelRenderer leftArm, ModelRenderer rightArm, boolean aggressive, float swingProgress, float ageInTicks) {
        float swing = MathHelper.sin(swingProgress * (float) Math.PI);
        float swingEased = MathHelper.sin((1.0F - (1.0F - swingProgress) * (1.0F - swingProgress)) * (float) Math.PI);
        rightArm.rotateAngleZ = 0.0F;
        leftArm.rotateAngleZ = 0.0F;
        rightArm.rotateAngleY = -(0.1F - swing * 0.6F);
        leftArm.rotateAngleY = 0.1F - swing * 0.6F;
        float raised = -(float) Math.PI / (aggressive ? 1.5F : 2.25F);
        rightArm.rotateAngleX = raised;
        leftArm.rotateAngleX = raised;
        rightArm.rotateAngleX += swing * 1.2F - swingEased * 0.4F;
        leftArm.rotateAngleX += swing * 1.2F - swingEased * 0.4F;
        bobArms(rightArm, leftArm, ageInTicks);
    }

    /** The idle sway of raised arms. */
    public static void bobArms(ModelRenderer rightArm, ModelRenderer leftArm, float ageInTicks) {
        rightArm.rotateAngleZ += MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        leftArm.rotateAngleZ -= MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        rightArm.rotateAngleX += MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
        leftArm.rotateAngleX -= MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
    }

    private ModelHelper116() {}
}
