package com.michaelsebero.portium.mixin.accessor;

import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityAINearestAttackableTarget.class)
public interface EntityAINearestAttackableTargetAccessor {

    @Accessor("targetClass")
    Class<?> portium$getTargetClass();

    @Accessor("targetChance")
    int portium$getTargetChance();
}
