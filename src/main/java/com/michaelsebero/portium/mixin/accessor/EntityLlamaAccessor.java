package com.michaelsebero.portium.mixin.accessor;

import net.minecraft.entity.passive.EntityLlama;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityLlama.class)
public interface EntityLlamaAccessor {

    @Invoker("setStrength")
    void portium$setStrength(int strength);
}
