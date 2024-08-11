package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.world.WorldGeneration;
import com.michaelsebero.portium.world.v116.nether.BiomeProviderNether116;
import com.michaelsebero.portium.world.v116.nether.ChunkGeneratorNether116;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderHell;
import net.minecraft.world.gen.IChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.16's Nether, biomes and generator, for worlds of 1.16.5 generation. */
@Mixin(WorldProviderHell.class)
public abstract class MixinWorldProviderHell extends WorldProvider {

    @Inject(method = "init", at = @At("TAIL"))
    private void portium$netherBiomes(CallbackInfo ci) {
        // The client keeps the biomes the server sends with each chunk.
        if (this.world.isRemote || !PortiumConfig.worldGen.netherUpdate || !WorldGeneration.is116(this.world)) return;
        this.biomeProvider = new BiomeProviderNether116(this.world.getSeed());
    }

    @Inject(method = "createChunkGenerator", at = @At("HEAD"), cancellable = true)
    private void portium$netherGenerator(CallbackInfoReturnable<IChunkGenerator> cir) {
        if (this.biomeProvider instanceof BiomeProviderNether116) {
            cir.setReturnValue(new ChunkGeneratorNether116(this.world, this.world.getWorldInfo().isMapFeaturesEnabled(), this.world.getSeed(),
                    (BiomeProviderNether116) this.biomeProvider));
        }
    }
}
