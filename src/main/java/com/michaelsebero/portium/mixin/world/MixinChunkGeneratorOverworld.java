package com.michaelsebero.portium.mixin.world;

import com.llamalad7.mixinextras.sugar.Local;
import com.michaelsebero.portium.mixin.accessor.MapGenScatteredFeatureAccessor;
import com.michaelsebero.portium.world.buffet.BuffetSettings;
import com.michaelsebero.portium.world.buffet.IBuffetSurface;
import com.michaelsebero.portium.world.gen.structure.OceanStructures;
import com.michaelsebero.portium.world.gen.structure.TempleLocator;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * Lets {@code /locate} and explorer maps find shipwrecks, ocean ruins and buried treasure, and each kind of temple by
 * its 1.13 name; and serves as a Buffet world's surface generator, built from the world's default block and fluid.
 */
@Mixin(ChunkGeneratorOverworld.class)
public abstract class MixinChunkGeneratorOverworld implements IBuffetSurface {

    @Shadow @Final private boolean mapFeaturesEnabled;
    @Shadow private MapGenScatteredFeature scatteredFeatureGenerator;

    @Unique @Nullable private IBlockState portium$defaultBlock;
    @Unique @Nullable private IBlockState portium$defaultFluid;

    @Override
    public void portium$setDefaults(IBlockState block, IBlockState fluid) {
        this.portium$defaultBlock = block;
        this.portium$defaultFluid = fluid;
    }

    /** After the biome surfaces and before the caves, as 1.13's surfaces and carvers treat the default block. */
    @Inject(method = "generateChunk", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/world/gen/ChunkGeneratorOverworld;replaceBiomeBlocks(IILnet/minecraft/world/chunk/ChunkPrimer;[Lnet/minecraft/world/biome/Biome;)V"))
    private void portium$buffetDefaults(int x, int z, CallbackInfoReturnable<Chunk> cir, @Local ChunkPrimer primer) {
        if (this.portium$defaultBlock != null) BuffetSettings.replaceDefaults(primer, this.portium$defaultBlock, this.portium$defaultFluid);
    }

    @Inject(method = "getNearestStructurePos", at = @At("HEAD"), cancellable = true)
    private void portium$locateNewStructures(World world, String name, BlockPos pos, boolean findUnexplored, CallbackInfoReturnable<BlockPos> cir) {
        if (OceanStructures.isOurs(name)) {
            cir.setReturnValue(OceanStructures.findNearest(world, name, pos));
        } else if (TempleLocator.isTempleName(name)) {
            cir.setReturnValue(this.mapFeaturesEnabled && this.scatteredFeatureGenerator != null
                    ? TempleLocator.findNearest(world, name, pos, ((MapGenScatteredFeatureAccessor) this.scatteredFeatureGenerator).portium$getSpacing()) : null);
        }
    }
}
