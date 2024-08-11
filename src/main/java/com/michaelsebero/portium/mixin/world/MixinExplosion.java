package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.world.BedExplosions;
import net.minecraft.util.DamageSource;
import net.minecraft.world.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Explosion.class)
public abstract class MixinExplosion {

    @Redirect(method = "doExplosionA", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/DamageSource;causeExplosionDamage(Lnet/minecraft/world/Explosion;)Lnet/minecraft/util/DamageSource;"))
    private DamageSource portium$intentionalGameDesign(Explosion explosion) {
        return BedExplosions.isActive() ? BedExplosions.damageSource() : DamageSource.causeExplosionDamage(explosion);
    }
}
