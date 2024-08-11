package com.michaelsebero.portium.mixin.accessor;

import com.google.gson.Gson;
import net.minecraft.world.storage.loot.LootTableManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LootTableManager.class)
public interface LootTableManagerAccessor {

    @Accessor("GSON_INSTANCE")
    static Gson portium$getGson() {
        throw new AssertionError();
    }
}
