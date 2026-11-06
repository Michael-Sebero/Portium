package com.michaelsebero.portium.mixin.modern;

import com.google.common.collect.ImmutableSet;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootTableList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * The loot table names as they are now, not a live view: newer mods name tables while tables load (Supplementaries
 * does, from its loot table event), and 1.12's loot table manager is going through that same set.
 */
@Mixin(LootTableList.class)
public abstract class MixinLootTableList {

    @Inject(method = "getAll", at = @At("RETURN"), cancellable = true)
    private static void portium$snapshot(CallbackInfoReturnable<Set<ResourceLocation>> info) {
        info.setReturnValue(ImmutableSet.copyOf(info.getReturnValue()));
    }
}
