package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.block.bee.BlockHoney;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The honey flecks the server asks for when something slides down a honey block (53) or a mob lands on one (54). */
@Mixin(Entity.class)
public abstract class MixinEntityHoneyStatus {

    @Inject(method = "handleStatusUpdate", at = @At("HEAD"))
    private void portium$honeyParticles(byte id, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (id == BlockHoney.SLIDE_STATUS) BlockHoney.slideParticles(self, 5);
        else if (id == BlockHoney.LAND_STATUS && self instanceof EntityLivingBase) BlockHoney.slideParticles(self, 10);
    }
}
