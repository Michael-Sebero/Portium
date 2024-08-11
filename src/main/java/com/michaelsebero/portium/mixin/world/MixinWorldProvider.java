package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.world.WorldGeneration;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tells world generation hooks which world they are for while its biome source and chunk generator are built, since
 * Forge's biome layer and map generator events don't say.
 */
@Mixin(WorldProvider.class)
public abstract class MixinWorldProvider {

    @Shadow protected World world;

    @Inject(method = "init", at = @At("HEAD"))
    private void portium$startInit(CallbackInfo ci) {
        WorldGeneration.startBuilding(this.world.getWorldInfo());
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void portium$endInit(CallbackInfo ci) {
        WorldGeneration.stopBuilding();
    }

    @Inject(method = "createChunkGenerator", at = @At("HEAD"))
    private void portium$startGenerator(CallbackInfoReturnable<?> cir) {
        WorldGeneration.startBuilding(this.world.getWorldInfo());
    }

    @Inject(method = "createChunkGenerator", at = @At("RETURN"))
    private void portium$endGenerator(CallbackInfoReturnable<?> cir) {
        WorldGeneration.stopBuilding();
    }
}
