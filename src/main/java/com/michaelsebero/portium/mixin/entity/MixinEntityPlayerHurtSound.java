package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.init.PortiumDamageSources;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Players pricked by a sweet berry bush make 1.14's own hurt sound. */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayerHurtSound {

    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    private void portium$berryBushHurt(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
        if (source == PortiumDamageSources.SWEET_BERRY_BUSH) cir.setReturnValue(PortiumSounds.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH);
    }
}
