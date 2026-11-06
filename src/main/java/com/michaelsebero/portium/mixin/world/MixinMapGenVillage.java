package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.world.gen.jigsaw.JigsawStructures;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenVillage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No 1.12 villages where 1.16.5's jigsaw villages generate instead (worlds made with 1.16.5 generation). */
@Mixin(MapGenVillage.class)
public abstract class MixinMapGenVillage extends MapGenStructure {

    @Inject(method = "canSpawnStructureAtCoords", at = @At("HEAD"), cancellable = true)
    private void portium$jigsawVillages(int chunkX, int chunkZ, CallbackInfoReturnable<Boolean> cir) {
        if (this.world != null && JigsawStructures.replacesVillages(this.world)) cir.setReturnValue(false);
    }
}
