package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.entity.IMooshroomType;
import net.minecraft.client.renderer.entity.RenderMooshroom;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Brown mooshrooms' own texture (1.14). */
@Mixin(RenderMooshroom.class)
public abstract class MixinRenderMooshroom {

    @Unique
    private static final ResourceLocation PORTIUM_BROWN = new ResourceLocation(Tags.MOD_ID, "textures/entity/cow/brown_mooshroom.png");

    @Inject(method = "getEntityTexture(Lnet/minecraft/entity/passive/EntityMooshroom;)Lnet/minecraft/util/ResourceLocation;", at = @At("HEAD"),
            cancellable = true)
    private void portium$brownTexture(EntityMooshroom mooshroom, CallbackInfoReturnable<ResourceLocation> cir) {
        if (mooshroom instanceof IMooshroomType && ((IMooshroomType) mooshroom).portium$isBrown()) cir.setReturnValue(PORTIUM_BROWN);
    }
}
