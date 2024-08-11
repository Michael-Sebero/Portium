package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.world.WorldGeneration;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenPumpkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Pumpkin patches generate uncarved pumpkins (1.13). */
@Mixin(WorldGenPumpkin.class)
public abstract class MixinWorldGenPumpkin {

    @Redirect(method = "generate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z"))
    private boolean portium$placeUncarved(World world, BlockPos pos, IBlockState state, int flags) {
        return world.setBlockState(pos, PortiumConfig.worldGen.uncarvedPumpkins && PortiumBlocks.PUMPKIN != null && WorldGeneration.isModern(world) ? PortiumBlocks.PUMPKIN.getDefaultState() : state, flags);
    }
}
