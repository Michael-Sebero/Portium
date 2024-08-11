package com.michaelsebero.portium.mixin.accessor;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityLookHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityLiving.class)
public interface EntityLivingAccessor {

    @Accessor("inventoryHandsDropChances")
    float[] portium$getHandDropChances();

    @Accessor("inventoryArmorDropChances")
    float[] portium$getArmorDropChances();

    @Accessor("lookHelper")
    @Mutable
    void portium$setLookHelper(EntityLookHelper lookHelper);
}
