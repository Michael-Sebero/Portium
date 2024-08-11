package com.michaelsebero.portium.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockFluidRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * 1.13 water: grayscale water textures tinted by the biome, including the underside, which 1.12 left untinted; and
 * waterlogged cells shape the water surface around them as still sources.
 */
@Mixin(BlockFluidRenderer.class)
public abstract class MixinBlockFluidRenderer {

    @ModifyArg(method = "initAtlasSprites", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureMap;getAtlasSprite(Ljava/lang/String;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"))
    private String portium$newWaterSprites(String sprite) {
        if (!PortiumConfig.visuals.newWaterColors) return sprite;
        if (sprite.equals("minecraft:blocks/water_still")) return Tags.MOD_ID + ":blocks/water_still";
        if (sprite.equals("minecraft:blocks/water_flow")) return Tags.MOD_ID + ":blocks/water_flow";
        return sprite;
    }

    @WrapOperation(method = "renderFluid",
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/block/state/IBlockState;getPackedLightmapCoords(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)I", ordinal = 1),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/block/state/IBlockState;getPackedLightmapCoords(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)I", ordinal = 2)),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/BufferBuilder;color(FFFF)Lnet/minecraft/client/renderer/BufferBuilder;"))
    private BufferBuilder portium$tintUnderside(BufferBuilder buffer, float red, float green, float blue, float alpha, Operation<BufferBuilder> original,
                                                @Local(argsOnly = true) IBlockState state,
                                                @Local(ordinal = 0) float tintRed, @Local(ordinal = 1) float tintGreen, @Local(ordinal = 2) float tintBlue) {
        if (PortiumConfig.visuals.newWaterColors && state.getMaterial() == Material.WATER) {
            return original.call(buffer, red * tintRed, green * tintGreen, blue * tintBlue, alpha);
        }
        return original.call(buffer, red, green, blue, alpha);
    }

    @Redirect(method = "getFluidHeight", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/IBlockAccess;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"))
    private IBlockState portium$waterloggedIsSource(IBlockAccess world, BlockPos pos) {
        return Waterlogging.isWaterlogged(world, pos) ? Blocks.WATER.getDefaultState() : world.getBlockState(pos);
    }
}
