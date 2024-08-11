package com.michaelsebero.portium.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.michaelsebero.portium.PortiumConfig;
import net.minecraft.entity.EnumCreatureType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 1.13 raised the water creature cap from 5 to 15 to make room for fish. */
@Mixin(EnumCreatureType.class)
public abstract class MixinEnumCreatureType {

    @ModifyReturnValue(method = "getMaxNumberOfCreature", at = @At("RETURN"))
    private int portium$waterCreatureCap(int cap) {
        return PortiumConfig.mechanics.moreWaterMobs && (Object) this == EnumCreatureType.WATER_CREATURE ? 15 : cap;
    }
}
