package com.michaelsebero.portium.mixin.modern;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.michaelsebero.portium.modern.ModernTicks;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Newer mods' entities update through {@link ModernTicks}: one whose update reaches a part of newer Minecraft that
 * Portium doesn't cover yet stops updating (its class, logged once) rather than crashing the world.
 */
@Mixin(World.class)
public abstract class MixinWorldModernEntities {

    @WrapOperation(method = "updateEntityWithOptionalForce", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;onUpdate()V"))
    private void portium$updateModern(Entity entity, Operation<Void> original) {
        ClassLoader loader = entity.getClass().getClassLoader();
        if (loader == null || !loader.getClass().getName().startsWith("com.michaelsebero.portium.")) {
            original.call(entity);
            return;
        }
        if (ModernTicks.isOff(entity)) return;
        try {
            original.call(entity);
        } catch (LinkageError e) {
            ModernTicks.switchOff(entity, e);
        } catch (RuntimeException e) {
            // Any failure: a newer mod's mob mostly fails where Portium's cover ends (a value it lacks), seldom on its own.
            ModernTicks.switchOff(entity, e);
        }
    }

    @WrapOperation(method = "updateEntityWithOptionalForce", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;updateRidden()V"))
    private void portium$updateRiddenModern(Entity entity, Operation<Void> original) {
        ClassLoader loader = entity.getClass().getClassLoader();
        if (loader == null || !loader.getClass().getName().startsWith("com.michaelsebero.portium.")) {
            original.call(entity);
            return;
        }
        if (ModernTicks.isOff(entity)) return;
        try {
            original.call(entity);
        } catch (LinkageError e) {
            ModernTicks.switchOff(entity, e);
        } catch (RuntimeException e) {
            // Any failure: a newer mod's mob mostly fails where Portium's cover ends (a value it lacks), seldom on its own.
            ModernTicks.switchOff(entity, e);
        }
    }
}
