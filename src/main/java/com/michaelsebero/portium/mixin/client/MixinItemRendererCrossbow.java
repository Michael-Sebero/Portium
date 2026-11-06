package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.item.ItemCrossbow;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The crossbow in first person (1.14): pulled back and shaking as it loads, turned to aim when loaded, and held as a
 * two-handed weapon (the other hand isn't drawn while loading one or holding a loaded one).
 */
@Mixin(ItemRenderer.class)
public abstract class MixinItemRendererCrossbow {

    @Shadow
    private void transformSideFirstPerson(EnumHandSide hand, float equippedProgress) {}

    @Shadow
    private void transformFirstPerson(EnumHandSide hand, float swingProgress) {}

    @Shadow
    public abstract void renderItemSide(EntityLivingBase entity, ItemStack stack, ItemCameraTransforms.TransformType transform, boolean leftHanded);

    @Inject(method = "renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V",
            at = @At("HEAD"), cancellable = true)
    private void portium$crossbow(AbstractClientPlayer player, float partialTicks, float pitch, EnumHand hand, float swingProgress, ItemStack stack,
                                  float equippedProgress, CallbackInfo ci) {
        if (portium$hidden(player, hand)) {
            ci.cancel();
            return;
        }
        if (!(stack.getItem() instanceof ItemCrossbow)) return;
        ci.cancel();
        boolean charged = ItemCrossbow.isCharged(stack);
        EnumHandSide side = hand == EnumHand.MAIN_HAND ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
        boolean right = side == EnumHandSide.RIGHT;
        int sign = right ? 1 : -1;
        GlStateManager.pushMatrix();
        if (player.isHandActive() && player.getItemInUseCount() > 0 && player.getActiveHand() == hand) {
            this.transformSideFirstPerson(side, equippedProgress);
            GlStateManager.translate((float) sign * -0.4785682F, -0.094387F, 0.05731531F);
            GlStateManager.rotate(-11.935F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate((float) sign * 65.3F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate((float) sign * -9.785F, 0.0F, 0.0F, 1.0F);
            float used = (float) ItemCrossbow.USE_DURATION - ((float) player.getItemInUseCount() - partialTicks + 1.0F);
            float progress = Math.min(1.0F, used / (float) ItemCrossbow.getChargeTime(stack));
            if (progress > 0.1F) {
                float shake = MathHelper.sin((used - 0.1F) * 1.3F) * (progress - 0.1F);
                GlStateManager.translate(0.0F, shake * 0.004F, 0.0F);
            }
            GlStateManager.translate(0.0F, 0.0F, progress * 0.04F);
            GlStateManager.scale(1.0F, 1.0F, 1.0F + progress * 0.2F);
            GlStateManager.rotate((float) sign * 45.0F, 0.0F, -1.0F, 0.0F);
        } else {
            float x = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
            float y = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * ((float) Math.PI * 2F));
            float z = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
            GlStateManager.translate((float) sign * x, y, z);
            this.transformSideFirstPerson(side, equippedProgress);
            this.transformFirstPerson(side, swingProgress);
            if (charged && swingProgress < 0.001F) {
                GlStateManager.translate((float) sign * -0.641864F, 0.0F, 0.0F);
                GlStateManager.rotate((float) sign * 10.0F, 0.0F, 1.0F, 0.0F);
            }
        }
        this.renderItemSide(player, stack, right ? ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND
                : ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, !right);
        GlStateManager.popMatrix();
    }

    /** 1.14's two-handed crossbow: which hand isn't drawn. */
    private static boolean portium$hidden(AbstractClientPlayer player, EnumHand hand) {
        boolean showMain = true;
        boolean showOff = true;
        ItemStack main = player.getHeldItemMainhand();
        ItemStack off = player.getHeldItemOffhand();
        if (player.isHandActive()) {
            ItemStack active = player.getActiveItemStack();
            if (active.getItem() instanceof ItemCrossbow) {
                showMain = player.getActiveHand() == EnumHand.MAIN_HAND;
                showOff = !showMain;
            }
            if (player.getActiveHand() == EnumHand.MAIN_HAND && main.getItem() instanceof ItemCrossbow && ItemCrossbow.isCharged(main)) showOff = false;
        } else {
            if (main.getItem() instanceof ItemCrossbow && ItemCrossbow.isCharged(main)) showOff = false;
            if (off.getItem() instanceof ItemCrossbow && ItemCrossbow.isCharged(off)) {
                showMain = !main.isEmpty();
                showOff = !showMain;
            }
        }
        return hand == EnumHand.MAIN_HAND ? !showMain : !showOff;
    }
}
