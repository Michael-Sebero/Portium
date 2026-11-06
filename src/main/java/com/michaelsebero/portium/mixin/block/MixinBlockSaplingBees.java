package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.world.gen.bee.BeeNests;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/** Marks trees grown from saplings, which get bee nests by nearby flowers rather than by biome (1.15). */
@Mixin(BlockSapling.class)
public abstract class MixinBlockSaplingBees {

    @Inject(method = "generateTree", at = @At("HEAD"))
    private void portium$saplingStart(World world, BlockPos pos, IBlockState state, Random random, CallbackInfo ci) {
        BeeNests.growingSapling(true);
    }

    @Inject(method = "generateTree", at = @At("RETURN"))
    private void portium$saplingEnd(World world, BlockPos pos, IBlockState state, Random random, CallbackInfo ci) {
        BeeNests.growingSapling(false);
    }
}
