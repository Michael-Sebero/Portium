package com.michaelsebero.portium.client;

import com.michaelsebero.portium.item.ItemCrossbow;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 1.14's crossbow arm poses (BipedModel's CROSSBOW_CHARGE and CROSSBOW_HOLD), for players, pillagers and piglins. */
@SideOnly(Side.CLIENT)
public final class CrossbowPoses {

    public static final int NONE = 0;
    public static final int CHARGE = 1;
    public static final int HOLD = 2;

    /** The pose for a hand: loading the crossbow it's using, or aiming a loaded one it holds (not mid-swing). */
    public static int pose(EntityLivingBase entity, EnumHand hand) {
        ItemStack stack = entity.getHeldItem(hand);
        if (!(stack.getItem() instanceof ItemCrossbow)) return NONE;
        if (entity.isHandActive() && entity.getActiveHand() == hand && entity.getItemInUseCount() > 0) return CHARGE;
        if (!entity.isSwingInProgress && ItemCrossbow.isCharged(stack)) return HOLD;
        return NONE;
    }

    public static void apply(int pose, ModelRenderer rightArm, ModelRenderer leftArm, ModelRenderer head, EntityLivingBase entity, boolean rightHanded) {
        if (pose == CHARGE) charge(rightArm, leftArm, entity, rightHanded);
        else if (pose == HOLD) hold(rightArm, leftArm, head, rightHanded);
    }

    /** Loading: the crossbow hand out, the other drawing back further as the charge builds. */
    public static void charge(ModelRenderer rightArm, ModelRenderer leftArm, EntityLivingBase entity, boolean rightHanded) {
        ModelRenderer main = rightHanded ? rightArm : leftArm;
        ModelRenderer off = rightHanded ? leftArm : rightArm;
        main.rotateAngleY = rightHanded ? -0.8F : 0.8F;
        main.rotateAngleX = -0.97079635F;
        off.rotateAngleX = main.rotateAngleX;
        float chargeTime = (float) ItemCrossbow.getChargeTime(entity.getActiveItemStack());
        float used = MathHelper.clamp((float) (ItemCrossbow.USE_DURATION - entity.getItemInUseCount()), 0.0F, chargeTime);
        float progress = used / chargeTime;
        off.rotateAngleY = (0.4F + progress * (0.85F - 0.4F)) * (rightHanded ? 1 : -1);
        off.rotateAngleX = off.rotateAngleX + progress * (-((float) Math.PI / 2F) - off.rotateAngleX);
    }

    /** Aiming a loaded crossbow along the look. */
    public static void hold(ModelRenderer rightArm, ModelRenderer leftArm, ModelRenderer head, boolean rightHanded) {
        ModelRenderer main = rightHanded ? rightArm : leftArm;
        ModelRenderer off = rightHanded ? leftArm : rightArm;
        main.rotateAngleY = (rightHanded ? -0.3F : 0.3F) + head.rotateAngleY;
        off.rotateAngleY = (rightHanded ? 0.6F : -0.6F) + head.rotateAngleY;
        main.rotateAngleX = -((float) Math.PI / 2F) + head.rotateAngleX + 0.1F;
        off.rotateAngleX = -1.5F + head.rotateAngleX;
    }

    private CrossbowPoses() {}
}
