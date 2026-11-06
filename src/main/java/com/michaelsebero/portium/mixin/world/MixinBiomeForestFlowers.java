package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.world.gen.plant.Plants114;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeForest;
import net.minecraft.world.gen.feature.WorldGenDoublePlant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * Forests' tall flower patches in 1.16.5 worlds: 1.14 picks among lilac, rose bush, peony and a lily of the valley
 * patch where 1.12 picks among the first three.
 */
@Mixin(BiomeForest.class)
public abstract class MixinBiomeForestFlowers {

    @Unique
    private static final WorldGenDoublePlant PORTIUM_DOUBLE_PLANTS = new WorldGenDoublePlant();

    @Inject(method = "addDoublePlants", at = @At("HEAD"), cancellable = true)
    private void portium$lilies(World world, Random rand, BlockPos pos, int count, CallbackInfo ci) {
        if (!Plants114.applies(world)) return;
        ci.cancel();
        for (int i = 0; i < count; i++) {
            int pick = rand.nextInt(4);
            if (pick == 3) {
                Plants114.lilyPatch(world, rand, pos);
                continue;
            }
            PORTIUM_DOUBLE_PLANTS.setPlantType(pick == 0 ? BlockDoublePlant.EnumPlantType.SYRINGA
                    : pick == 1 ? BlockDoublePlant.EnumPlantType.ROSE : BlockDoublePlant.EnumPlantType.PAEONIA);
            for (int k = 0; k < 5; k++) {
                int x = rand.nextInt(16) + 8;
                int z = rand.nextInt(16) + 8;
                int y = rand.nextInt(world.getHeight(pos.add(x, 0, z)).getY() + 32);
                if (PORTIUM_DOUBLE_PLANTS.generate(world, rand, new BlockPos(pos.getX() + x, y, pos.getZ() + z))) break;
            }
        }
    }
}
