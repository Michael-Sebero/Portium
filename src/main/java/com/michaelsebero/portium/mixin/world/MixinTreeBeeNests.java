package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.world.gen.bee.BeeNests;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenBigTree;
import net.minecraft.world.gen.feature.WorldGenBirchTree;
import net.minecraft.world.gen.feature.WorldGenTrees;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/** Oaks, fancy oaks and birches may get a bee nest once grown (1.15's beehive tree decorator). */
@Mixin({WorldGenTrees.class, WorldGenBirchTree.class, WorldGenBigTree.class})
public abstract class MixinTreeBeeNests {

    @Inject(method = "generate", at = @At("RETURN"))
    private void portium$beeNest(World world, Random random, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) BeeNests.afterTree(world, random, pos);
    }
}
