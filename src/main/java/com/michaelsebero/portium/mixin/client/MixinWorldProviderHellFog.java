package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.client.NetherAmbience;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderHell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The Nether's fog takes 1.16's biome fog colours. */
@Mixin(WorldProviderHell.class)
public abstract class MixinWorldProviderHellFog extends WorldProvider {

    @Inject(method = "getFogColor", at = @At("HEAD"), cancellable = true)
    private void portium$biomeFog(float celestialAngle, float partialTicks, CallbackInfoReturnable<Vec3d> cir) {
        Vec3d color = NetherAmbience.fogColor(this.world, partialTicks);
        if (color != null) cir.setReturnValue(color);
    }
}
