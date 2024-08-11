package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.compat.ModernSaves;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.scoreboard.ScoreboardSaveData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reads scoreboards saved by 1.13: JSON names and prefixes, and 1.13's criteria names. */
@Mixin(ScoreboardSaveData.class)
public abstract class MixinScoreboardSaveData {

    @Inject(method = "readFromNBT", at = @At("HEAD"))
    private void portium$convertModern(NBTTagCompound data, CallbackInfo ci) {
        ModernSaves.convertScoreboard(data);
    }
}
