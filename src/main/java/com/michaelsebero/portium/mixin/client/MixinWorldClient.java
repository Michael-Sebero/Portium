package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.client.NetherAmbience;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * A waterlogged cell shows its water's random effects as well as its block's: underwater particles and drips (1.13).
 * The same random spots get 1.16's Nether biome particles, and 1.12's cave sounds give way to the Nether biomes' own
 * mood sounds.
 */
@Mixin(WorldClient.class)
public abstract class MixinWorldClient {

    @Inject(method = "showBarrierParticles", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/block/Block;randomDisplayTick(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)V"))
    private void portium$waterloggedDisplayTick(int x, int y, int z, int offset, Random random, boolean holdingBarrier, BlockPos.MutableBlockPos pos, CallbackInfo ci) {
        World world = (World) (Object) this;
        if (Waterlogging.isWaterlogged(world, pos)) Blocks.WATER.randomDisplayTick(Blocks.WATER.getDefaultState(), world, pos, random);
    }

    @Inject(method = "showBarrierParticles", at = @At("TAIL"))
    private void portium$netherParticles(int x, int y, int z, int offset, Random random, boolean holdingBarrier, BlockPos.MutableBlockPos pos, CallbackInfo ci) {
        World world = (World) (Object) this;
        NetherAmbience.ambientParticle(world, pos, world.getBlockState(pos));
    }

    @Redirect(method = "playMoodSoundAndCheckLight", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/WorldClient;playSound(DDDLnet/minecraft/util/SoundEvent;Lnet/minecraft/util/SoundCategory;FFZ)V"))
    private void portium$netherMood(WorldClient world, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch,
                                    boolean distanceDelay) {
        if (sound == SoundEvents.AMBIENT_CAVE && NetherAmbience.replacesCaveSounds(world)) return;
        world.playSound(x, y, z, sound, category, volume, pitch, distanceDelay);
    }
}
