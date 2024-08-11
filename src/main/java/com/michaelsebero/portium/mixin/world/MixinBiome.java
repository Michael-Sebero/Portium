package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.world.biome.BiomeFrozenOcean;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public abstract class MixinBiome {

    @Inject(method = "getTemperature", at = @At("HEAD"), cancellable = true)
    private void portium$patchyFrozenOcean(BlockPos pos, CallbackInfoReturnable<Float> cir) {
        Object self = this;
        if (self instanceof BiomeFrozenOcean && !((BiomeFrozenOcean) self).isDeep()) {
            cir.setReturnValue(((BiomeFrozenOcean) self).getPatchyTemperature(pos));
        }
    }
}
