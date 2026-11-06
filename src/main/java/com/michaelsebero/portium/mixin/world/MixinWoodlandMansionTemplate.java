package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.entity.raid.RaiderSpawns;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.monster.EntityVindicator;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.gen.structure.WoodlandMansionPieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Mansion vindicators spawn for a structure (1.14's STRUCTURE reason): never patrol leaders. */
@Mixin(WoodlandMansionPieces.MansionTemplate.class)
public abstract class MixinWoodlandMansionTemplate {

    @Redirect(method = "handleDataMarker", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/monster/EntityVindicator;onInitialSpawn(Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/entity/IEntityLivingData;)Lnet/minecraft/entity/IEntityLivingData;"))
    private IEntityLivingData portium$structureSpawn(EntityVindicator vindicator, DifficultyInstance difficulty, IEntityLivingData data) {
        IEntityLivingData[] result = new IEntityLivingData[1];
        RaiderSpawns.spawning(RaiderSpawns.Reason.STRUCTURE, () -> result[0] = vindicator.onInitialSpawn(difficulty, data));
        return result[0];
    }
}
