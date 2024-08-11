package com.michaelsebero.portium.mixin.client;

import net.minecraft.client.renderer.block.model.ModelBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A model's texture variables resolve as from 1.15 on: each name is looked up from the model itself up through its
 * parents, so a variable may point to another of the same model ({@code "particle": "#rail"} beside {@code "rail"}).
 * 1.12 refused that as an upward reference and drew the missing texture; only a loop of names is refused now.
 */
@Mixin(ModelBlock.class)
public abstract class MixinModelBlock {

    @Shadow @Final public Map<String, String> textures;
    @Shadow public ModelBlock parent;
    @Shadow public String name;

    @Inject(method = "resolveTextureName", at = @At("HEAD"), cancellable = true)
    private void portium$resolveChain(String textureName, CallbackInfoReturnable<String> cir) {
        String name = textureName.startsWith("#") ? textureName.substring(1) : textureName;
        List<String> seen = new ArrayList<>();
        while (true) {
            String found = portium$find(name);
            if (found == null) {
                cir.setReturnValue("missingno");
                return;
            }
            if (!found.startsWith("#")) {
                cir.setReturnValue(found);
                return;
            }
            name = found.substring(1);
            if (seen.contains(name)) {
                cir.setReturnValue("missingno");
                return;
            }
            seen.add(name);
        }
    }

    /** The variable's value in this model or the nearest parent that sets it, or null if none does. */
    private String portium$find(String variable) {
        for (ModelBlock model = (ModelBlock) (Object) this; model != null; model = model.parent) {
            String value = ((MixinModelBlock) (Object) model).textures.get(variable);
            if (value != null) return value;
        }
        return null;
    }
}
