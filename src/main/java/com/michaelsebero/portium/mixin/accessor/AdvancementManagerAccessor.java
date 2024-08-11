package com.michaelsebero.portium.mixin.accessor;

import com.google.gson.Gson;
import net.minecraft.advancements.AdvancementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AdvancementManager.class)
public interface AdvancementManagerAccessor {

    @Accessor("GSON")
    static Gson portium$getGson() {
        throw new AssertionError();
    }
}
