package com.michaelsebero.portium.mixin.world;

import com.llamalad7.mixinextras.sugar.Local;
import com.michaelsebero.portium.handler.RespawnAnchors;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * A player whose respawn point is a working respawn anchor respawns at it wherever they died, as in 1.16 (1.12 only
 * sends players to their spawn dimension when the dimension they died in can't be respawned in).
 */
@Mixin(PlayerList.class)
public abstract class MixinPlayerListRespawn {

    @ModifyVariable(method = "recreatePlayerEntity", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int portium$respawnAnchor(int dimension, @Local(argsOnly = true) EntityPlayerMP player) {
        return RespawnAnchors.respawnDimension(player, dimension);
    }
}
