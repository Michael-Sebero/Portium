package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.init.PortiumBiomes;
import com.michaelsebero.portium.world.WorldGeneration;
import com.michaelsebero.portium.world.end.BiomeProviderEnd113;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderEnd;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.13's End biomes ({@link BiomeProviderEnd113}), in worlds of 1.13.2 generation or newer. */
@Mixin(WorldProviderEnd.class)
public abstract class MixinWorldProviderEnd extends WorldProvider {

    @Inject(method = "init", at = @At("TAIL"))
    private void portium$endBiomes(CallbackInfo ci) {
        // The client keeps the biomes the server sends with each chunk.
        if (this.world.isRemote || !PortiumConfig.worldGen.endBiomes || PortiumBiomes.END_HIGHLANDS == null || !WorldGeneration.isModern(this.world)) return;
        this.biomeProvider = new BiomeProviderEnd113(this.world.getSeed());
    }
}
