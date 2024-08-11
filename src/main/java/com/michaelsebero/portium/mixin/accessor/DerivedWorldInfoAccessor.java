package com.michaelsebero.portium.mixin.accessor;

import net.minecraft.world.storage.DerivedWorldInfo;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DerivedWorldInfo.class)
public interface DerivedWorldInfoAccessor {

    @Accessor("delegate")
    WorldInfo portium$getDelegate();
}
