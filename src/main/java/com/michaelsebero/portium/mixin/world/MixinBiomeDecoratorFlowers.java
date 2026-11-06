package com.michaelsebero.portium.mixin.world;

import com.llamalad7.mixinextras.sugar.Local;
import com.michaelsebero.portium.world.gen.plant.Plants114;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.gen.feature.WorldGenFlowers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

/** Plains and the flower forest pick their flower patches as 1.14 does, with its new flowers (see {@link Plants114}). */
@Mixin(BiomeDecorator.class)
public abstract class MixinBiomeDecoratorFlowers {

    @Redirect(method = "genDecorations", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/gen/feature/WorldGenFlowers;generate(Lnet/minecraft/world/World;Ljava/util/Random;Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean portium$flowers114(WorldGenFlowers flowers, World world, Random rand, BlockPos pos, @Local(argsOnly = true) Biome biome) {
        if (Plants114.applies(world)) {
            IBlockState flower = Plants114.pickFlower(biome, rand, pos);
            if (flower != null) {
                Plants114.flowerPatch(world, rand, pos, flower);
                return true;
            }
        }
        return flowers.generate(world, rand, pos);
    }
}
