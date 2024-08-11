package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.compat.ModernSaves;
import net.minecraft.advancements.PlayerAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Reads advancement progress saved by 1.13, whose data version entry 1.12 would reject the whole file over. */
@Mixin(PlayerAdvancements.class)
public abstract class MixinPlayerAdvancements {

    @ModifyArg(method = "load", index = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/JsonUtils;gsonDeserialize(Lcom/google/gson/Gson;Ljava/lang/String;Ljava/lang/reflect/Type;)Ljava/lang/Object;"))
    private String portium$withoutDataVersion(String json) {
        return ModernSaves.withoutDataVersion(json);
    }
}
