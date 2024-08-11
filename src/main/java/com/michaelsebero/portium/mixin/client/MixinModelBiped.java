package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.api.IPosedPlayer;
import com.michaelsebero.portium.client.PortiumRenderState;
import com.michaelsebero.portium.item.ItemTrident;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The 1.13 swimming/crawling stroke and the raised arm while aiming a trident. */
@Mixin(ModelBiped.class)
public abstract class MixinModelBiped extends ModelBase {

    @Shadow public ModelRenderer bipedHead;
    @Shadow public ModelRenderer bipedHeadwear;
    @Shadow public ModelRenderer bipedRightArm;
    @Shadow public ModelRenderer bipedLeftArm;
    @Shadow public ModelRenderer bipedRightLeg;
    @Shadow public ModelRenderer bipedLeftLeg;

    @Inject(method = "setRotationAngles", at = @At("TAIL"))
    private void portium$poseLimbs(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof EntityLivingBase) || PortiumRenderState.renderingFirstPersonArm) return;
        EntityLivingBase living = (EntityLivingBase) entity;
        this.portium$aimTrident(living, limbSwing, limbSwingAmount);
        if (living instanceof EntityPlayer) this.portium$swim((EntityPlayer) living, limbSwing, headPitch);
    }

    @Unique
    private void portium$aimTrident(EntityLivingBase living, float limbSwing, float limbSwingAmount) {
        if (!living.isHandActive() || !(living.getActiveItemStack().getItem() instanceof ItemTrident)) return;
        EnumHandSide side = living.getActiveHand() == EnumHand.MAIN_HAND ? living.getPrimaryHand() : living.getPrimaryHand().opposite();
        boolean right = side == EnumHandSide.RIGHT;
        ModelRenderer arm = right ? this.bipedRightArm : this.bipedLeftArm;
        float walkSwing = MathHelper.cos(limbSwing * 0.6662F + (right ? (float) Math.PI : 0.0F)) * limbSwingAmount * 0.5F;
        arm.rotateAngleX = walkSwing * 0.5F - (float) Math.PI;
        arm.rotateAngleY = 0.0F;
    }

    @Unique
    private void portium$swim(EntityPlayer player, float limbSwing, float headPitch) {
        IPosedPlayer posed = IPosedPlayer.of(player);
        float swim = posed.portium$getSwimAnimation(Minecraft.getMinecraft().getRenderPartialTicks());
        if (swim <= 0.0F || player.getTicksElytraFlying() > 4) return;

        float headTarget = posed.portium$isSwimming() ? -(float) Math.PI / 4.0F : headPitch * 0.017453292F;
        this.bipedHead.rotateAngleX = rotLerp(this.bipedHead.rotateAngleX, headTarget, swim);

        float stroke = limbSwing % 26.0F;
        float right = this.swingProgress > 0.0F ? 0.0F : swim;
        float pi = (float) Math.PI;
        if (stroke < 14.0F) {
            this.bipedLeftArm.rotateAngleX = rotLerp(this.bipedLeftArm.rotateAngleX, 0.0F, swim);
            this.bipedRightArm.rotateAngleX = lerp(this.bipedRightArm.rotateAngleX, 0.0F, right);
            this.bipedLeftArm.rotateAngleY = rotLerp(this.bipedLeftArm.rotateAngleY, pi, swim);
            this.bipedRightArm.rotateAngleY = lerp(this.bipedRightArm.rotateAngleY, pi, right);
            this.bipedLeftArm.rotateAngleZ = rotLerp(this.bipedLeftArm.rotateAngleZ, pi + 1.8707964F * strokeCurve(stroke) / strokeCurve(14.0F), swim);
            this.bipedRightArm.rotateAngleZ = lerp(this.bipedRightArm.rotateAngleZ, pi - 1.8707964F * strokeCurve(stroke) / strokeCurve(14.0F), right);
        } else if (stroke < 22.0F) {
            float t = (stroke - 14.0F) / 8.0F;
            this.bipedLeftArm.rotateAngleX = rotLerp(this.bipedLeftArm.rotateAngleX, pi / 2.0F * t, swim);
            this.bipedRightArm.rotateAngleX = lerp(this.bipedRightArm.rotateAngleX, pi / 2.0F * t, right);
            this.bipedLeftArm.rotateAngleY = rotLerp(this.bipedLeftArm.rotateAngleY, pi, swim);
            this.bipedRightArm.rotateAngleY = lerp(this.bipedRightArm.rotateAngleY, pi, right);
            this.bipedLeftArm.rotateAngleZ = rotLerp(this.bipedLeftArm.rotateAngleZ, 5.012389F - 1.8707964F * t, swim);
            this.bipedRightArm.rotateAngleZ = lerp(this.bipedRightArm.rotateAngleZ, 1.2707963F + 1.8707964F * t, right);
        } else {
            float t = (stroke - 22.0F) / 4.0F;
            this.bipedLeftArm.rotateAngleX = rotLerp(this.bipedLeftArm.rotateAngleX, pi / 2.0F - pi / 2.0F * t, swim);
            this.bipedRightArm.rotateAngleX = lerp(this.bipedRightArm.rotateAngleX, pi / 2.0F - pi / 2.0F * t, right);
            this.bipedLeftArm.rotateAngleY = rotLerp(this.bipedLeftArm.rotateAngleY, pi, swim);
            this.bipedRightArm.rotateAngleY = lerp(this.bipedRightArm.rotateAngleY, pi, right);
            this.bipedLeftArm.rotateAngleZ = rotLerp(this.bipedLeftArm.rotateAngleZ, pi, swim);
            this.bipedRightArm.rotateAngleZ = lerp(this.bipedRightArm.rotateAngleZ, pi, right);
        }
        this.bipedLeftLeg.rotateAngleX = lerp(this.bipedLeftLeg.rotateAngleX, 0.3F * MathHelper.cos(limbSwing * 0.33333334F + pi), swim);
        this.bipedRightLeg.rotateAngleX = lerp(this.bipedRightLeg.rotateAngleX, 0.3F * MathHelper.cos(limbSwing * 0.33333334F), swim);
        ModelBase.copyModelAngles(this.bipedHead, this.bipedHeadwear);
    }

    @Unique
    private static float strokeCurve(float x) {
        return -65.0F * x + x * x;
    }

    @Unique
    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    /** Interpolates angles the short way round. */
    @Unique
    private static float rotLerp(float from, float to, float t) {
        float diff = (to - from) % ((float) Math.PI * 2.0F);
        if (diff < -(float) Math.PI) diff += (float) Math.PI * 2.0F;
        if (diff >= (float) Math.PI) diff -= (float) Math.PI * 2.0F;
        return from + t * diff;
    }
}
