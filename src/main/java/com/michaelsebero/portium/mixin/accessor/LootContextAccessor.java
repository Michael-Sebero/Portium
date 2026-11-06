package com.michaelsebero.portium.mixin.accessor;

import net.minecraft.util.DamageSource;
import net.minecraft.world.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LootContext.class)
public interface LootContextAccessor {

    @Accessor("damageSource")
    DamageSource portium$getDamageSource();
}
