package com.michaelsebero.portium.mixin.network;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.michaelsebero.portium.PortiumConfig;
import net.minecraft.network.NetHandlerPlayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The server accepts elytra deployment while the player is still moving up (1.15). */
@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer {

    @ModifyExpressionValue(method = "processEntityAction", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayerMP;motionY:D"))
    private double portium$allowRisingTakeoff(double motionY) {
        return PortiumConfig.mechanics.easyElytraTakeoff ? -1.0D : motionY;
    }
}
