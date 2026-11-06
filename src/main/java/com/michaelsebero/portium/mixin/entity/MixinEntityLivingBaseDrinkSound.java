package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.item.ICustomDrinkSound;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Drinks with their own sound (honey bottles) play it while being drunk (1.15). */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBaseDrinkSound {

    @Redirect(method = "updateItemUse", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/entity/EntityLivingBase;playSound(Lnet/minecraft/util/SoundEvent;FF)V"))
    private void portium$drinkSound(EntityLivingBase self, SoundEvent sound, float volume, float pitch, ItemStack stack, int particles) {
        if (stack.getItem() instanceof ICustomDrinkSound) sound = ((ICustomDrinkSound) stack.getItem()).getDrinkSound();
        if (self instanceof com.michaelsebero.portium.item.IDrinkSoundEntity) sound = ((com.michaelsebero.portium.item.IDrinkSoundEntity) self).portium$getDrinkSound(stack);
        self.playSound(sound, volume, pitch);
    }
}
