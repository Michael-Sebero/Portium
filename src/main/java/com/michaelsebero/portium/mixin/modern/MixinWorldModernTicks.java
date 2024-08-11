package com.michaelsebero.portium.mixin.modern;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.michaelsebero.portium.modern.ModernTicks;
import com.michaelsebero.portium.modern.adapter.ModernTileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Newer mods' tile entities tick through {@link ModernTicks}, which keeps Portium's gaps from crashing the game. */
@Mixin(World.class)
public abstract class MixinWorldModernTicks {

    @WrapOperation(method = "updateEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ITickable;update()V"))
    private void portium$tickModern(ITickable tickable, Operation<Void> original) {
        if (!(tickable instanceof ModernTileEntity)) {
            original.call(tickable);
            return;
        }
        ModernTileEntity tile = (ModernTileEntity) tickable;
        if (ModernTicks.isOff(tile)) return;
        try {
            original.call(tickable);
        } catch (LinkageError e) {
            ModernTicks.switchOff(tile, e);
        }
    }
}
