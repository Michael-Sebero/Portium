package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.client.ParticleRendering116;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.16 mods' particles, batched while 1.12 draws its particles that set their own drawing up, are drawn at the pass's end. */
@Mixin(ParticleManager.class)
public abstract class MixinParticleManager116 {

    /** A 1.16 particle whose tick reaches something Portium doesn't cover yet is dropped, and its kind switched off, rather than crashing the game. */
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "tickParticle",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;onUpdate()V"))
    private void portium$tickParticle116(net.minecraft.client.particle.Particle particle,
                                         com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        if (!(particle instanceof com.michaelsebero.portium.mods116.adapter.Particle116)) {
            original.call(particle);
            return;
        }
        try {
            original.call(particle);
        } catch (LinkageError e) {
            ParticleRendering116.failed(particle, e);
        } catch (RuntimeException e) {
            if (!com.michaelsebero.portium.modern.ModernTicks.isLayerFault(e)) throw e;
            ParticleRendering116.failed(particle, e);
        }
    }

    @Inject(method = "renderLitParticles", at = @At("RETURN"))
    private void portium$drawParticles116(Entity entity, float partialTicks, CallbackInfo callback) {
        ParticleRendering116.flush();
    }
}
