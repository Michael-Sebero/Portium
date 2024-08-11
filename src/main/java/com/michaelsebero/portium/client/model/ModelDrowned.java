package com.michaelsebero.portium.client.model;

import com.michaelsebero.portium.entity.monster.EntityDrowned;
import com.michaelsebero.portium.item.ItemTrident;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;

/**
 * The drowned (1.13): a zombie whose right limbs use their own part of the texture, which raises its trident to throw
 * and swims with outstretched arms.
 */
public class ModelDrowned extends ModelZombie {

    private boolean rightArmThrowing;
    private boolean leftArmThrowing;
    private float swimAnimation;

    public ModelDrowned(float inflate) {
        super(inflate, false);
        this.bipedRightArm = new ModelRenderer(this, 32, 48);
        this.bipedRightArm.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, inflate);
        this.bipedRightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);
        this.bipedRightLeg = new ModelRenderer(this, 16, 48);
        this.bipedRightLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, inflate);
        this.bipedRightLeg.setRotationPoint(-1.9F, 12.0F, 0.0F);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks) {
        boolean throwing = entity instanceof EntityDrowned && entity.getHeldItemMainhand().getItem() instanceof ItemTrident && ((EntityDrowned) entity).isArmsRaised();
        boolean right = entity.getPrimaryHand() == EnumHandSide.RIGHT;
        this.rightArmThrowing = throwing && right;
        this.leftArmThrowing = throwing && !right;
        this.swimAnimation = entity instanceof EntityDrowned ? ((EntityDrowned) entity).getSwimAnimation(partialTicks) : 0.0F;
        super.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        if (this.leftArmThrowing) {
            this.bipedLeftArm.rotateAngleX = this.bipedLeftArm.rotateAngleX * 0.5F - (float) Math.PI;
            this.bipedLeftArm.rotateAngleY = 0.0F;
        }
        if (this.rightArmThrowing) {
            this.bipedRightArm.rotateAngleX = this.bipedRightArm.rotateAngleX * 0.5F - (float) Math.PI;
            this.bipedRightArm.rotateAngleY = 0.0F;
        }
        float swim = this.swimAnimation;
        if (swim > 0.0F) {
            float stroke = swim * MathHelper.sin(0.1F * ageInTicks);
            this.bipedRightArm.rotateAngleX = rotLerp(swim, this.bipedRightArm.rotateAngleX, -2.5132742F) + stroke * 0.35F;
            this.bipedLeftArm.rotateAngleX = rotLerp(swim, this.bipedLeftArm.rotateAngleX, -2.5132742F) - stroke * 0.35F;
            this.bipedRightArm.rotateAngleZ = rotLerp(swim, this.bipedRightArm.rotateAngleZ, -0.15F);
            this.bipedLeftArm.rotateAngleZ = rotLerp(swim, this.bipedLeftArm.rotateAngleZ, 0.15F);
            this.bipedLeftLeg.rotateAngleX -= stroke * 0.55F;
            this.bipedRightLeg.rotateAngleX += stroke * 0.55F;
            this.bipedHead.rotateAngleX = 0.0F;
            copyModelAngles(this.bipedHead, this.bipedHeadwear);
        }
    }

    private static float rotLerp(float t, float from, float to) {
        return from + t * MathHelper.wrapDegrees((to - from) * 57.295776F) * 0.017453292F;
    }
}
