package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.client.BiomeBlend;
import com.michaelsebero.portium.client.BiomeBlendCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.13's Biome Blend: grass, foliage and water colours average a square of the chosen size instead of 1.12's fixed
 * 3x3. The square's corners ({@code pos.add(-1, 0, -1)} and {@code pos.add(1, 0, 1)}) and the count it divides by
 * (9) follow the setting. While chunks are built, each column's blend is worked out once ({@link BiomeBlendCache}).
 */
@Mixin(BiomeColorHelper.class)
public abstract class MixinBiomeColorHelper {

    @Inject(method = "getColorAtPos", at = @At("HEAD"), cancellable = true)
    private static void portium$cachedBlend(IBlockAccess world, BlockPos pos, @Coerce Object resolver, CallbackInfoReturnable<Integer> cir) {
        int color = BiomeBlendCache.get(world, pos, resolver);
        if (color != BiomeBlendCache.MISS) cir.setReturnValue(color);
    }

    @Inject(method = "getColorAtPos", at = @At("RETURN"))
    private static void portium$rememberBlend(IBlockAccess world, BlockPos pos, @Coerce Object resolver, CallbackInfoReturnable<Integer> cir) {
        BiomeBlendCache.put(world, pos, resolver, cir.getReturnValueI());
    }

    @ModifyConstant(method = "getColorAtPos", constant = @Constant(intValue = -1))
    private static int portium$blendFrom(int offset) {
        return -BiomeBlend.effectiveRadius();
    }

    @ModifyConstant(method = "getColorAtPos", constant = @Constant(intValue = 1))
    private static int portium$blendTo(int offset) {
        return BiomeBlend.effectiveRadius();
    }

    @ModifyConstant(method = "getColorAtPos", constant = @Constant(intValue = 9))
    private static int portium$blendArea(int area) {
        int width = BiomeBlend.effectiveRadius() * 2 + 1;
        return width * width;
    }
}
