package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.loot.LootOrigin;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fishing loot is made where the bobber is (1.13's loot context origin), for its location checks (jungle bamboo). */
@Mixin(EntityFishHook.class)
public abstract class MixinEntityFishHookLootOrigin {

    @Inject(method = "handleHookRetraction", at = @At("HEAD"))
    private void portium$origin(CallbackInfoReturnable<Integer> cir) {
        LootOrigin.set(new BlockPos((EntityFishHook) (Object) this));
    }

    @Inject(method = "handleHookRetraction", at = @At("RETURN"))
    private void portium$clearOrigin(CallbackInfoReturnable<Integer> cir) {
        LootOrigin.set(null);
    }
}
