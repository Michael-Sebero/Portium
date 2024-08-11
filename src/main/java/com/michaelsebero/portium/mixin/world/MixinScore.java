package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.PortiumConfig;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** New scores start locked, as in 1.13, so a trigger only works once {@code scoreboard players enable} allows it. */
@Mixin(Score.class)
public abstract class MixinScore {

    @Shadow private boolean locked;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void portium$startLocked(Scoreboard scoreboard, ScoreObjective objective, String owner, CallbackInfo ci) {
        if (PortiumConfig.mechanics.modernCommands) this.locked = true;
    }
}
