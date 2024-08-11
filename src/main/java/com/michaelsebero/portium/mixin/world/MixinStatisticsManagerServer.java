package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.compat.ModernSaves;
import net.minecraft.stats.StatisticsManagerServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Reads statistics saved by 1.13, which groups them by type, as 1.12's flat list. */
@Mixin(StatisticsManagerServer.class)
public abstract class MixinStatisticsManagerServer {

    @ModifyVariable(method = "parseJson", at = @At("HEAD"), argsOnly = true)
    private String portium$legacyStats(String json) {
        return ModernSaves.legacyStats(json);
    }
}
