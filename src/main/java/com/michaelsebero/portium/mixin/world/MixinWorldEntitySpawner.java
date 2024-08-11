package com.michaelsebero.portium.mixin.world;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 1.13 checks a spawn position's fluid: water mobs can spawn in waterlogged cells, and land mobs can't spawn in them
 * or below one. The block a land mob stands on is still judged by its shape.
 */
@Mixin(WorldEntitySpawner.class)
public abstract class MixinWorldEntitySpawner {

    @WrapOperation(method = "canCreatureTypeSpawnBody", remap = false, at = {
            // The spawn cell, the cell below it for water mobs, and the cell above it for land mobs.
            @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;", ordinal = 0, remap = true),
            @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;", ordinal = 1, remap = true),
            @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;", ordinal = 5, remap = true)})
    private static IBlockState portium$waterloggedIsWater(World world, BlockPos pos, Operation<IBlockState> original) {
        return Waterlogging.isWaterlogged(world, pos) ? Blocks.WATER.getDefaultState() : original.call(world, pos);
    }
}
