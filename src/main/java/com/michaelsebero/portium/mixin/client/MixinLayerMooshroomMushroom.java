package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.entity.IMooshroomType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.entity.layers.LayerMooshroomMushroom;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.init.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Brown mooshrooms grow brown mushrooms on their backs (1.14). */
@Mixin(LayerMooshroomMushroom.class)
public abstract class MixinLayerMooshroomMushroom {

    @Unique
    private boolean portium$brown;

    @Inject(method = "doRenderLayer(Lnet/minecraft/entity/passive/EntityMooshroom;FFFFFFF)V", at = @At("HEAD"))
    private void portium$which(EntityMooshroom mooshroom, float a, float b, float c, float d, float e, float f, float g, CallbackInfo ci) {
        this.portium$brown = mooshroom instanceof IMooshroomType && ((IMooshroomType) mooshroom).portium$isBrown();
    }

    @ModifyArg(method = "doRenderLayer(Lnet/minecraft/entity/passive/EntityMooshroom;FFFFFFF)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/BlockRendererDispatcher;renderBlockBrightness(Lnet/minecraft/block/state/IBlockState;F)V"))
    private IBlockState portium$brownMushroom(IBlockState state) {
        return this.portium$brown ? Blocks.BROWN_MUSHROOM.getDefaultState() : state;
    }
}
