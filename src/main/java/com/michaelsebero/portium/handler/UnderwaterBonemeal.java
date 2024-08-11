package com.michaelsebero.portium.handler;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.block.BlockSeagrass;
import com.michaelsebero.portium.block.water.WaterUtil;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.world.biome.BiomePortiumOcean;
import com.michaelsebero.portium.world.biome.OceanTemperature;
import com.michaelsebero.portium.world.gen.feature.CoralPicker;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

/**
 * Bone meal on the sea floor grows seagrass around it, and in warm oceans also coral (1.13 growSeagrass).
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class UnderwaterBonemeal {

    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        if (!PortiumConfig.mechanics.underwaterBonemeal || PortiumBlocks.SEAGRASS == null) return;
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState clicked = event.getBlock();
        if (clicked.getBlock() instanceof IGrowable && ((IGrowable) clicked.getBlock()).canGrow(world, pos, clicked, world.isRemote)) return;
        BlockPos water = pos.up();
        if (!WaterUtil.isPlainWaterSource(world.getBlockState(water)) || !world.isSideSolid(pos, EnumFacing.UP)) return;

        event.setResult(Event.Result.ALLOW);
        if (world.isRemote) return;
        Random rand = world.rand;
        attempts:
        for (int i = 0; i < 128; i++) {
            BlockPos target = water;
            for (int step = 0; step < i / 16; step++) {
                target = target.add(rand.nextInt(3) - 1, (rand.nextInt(3) - 1) * rand.nextInt(3) / 2, rand.nextInt(3) - 1);
                if (world.getBlockState(target).isNormalCube()) continue attempts;
            }
            IBlockState toPlace = PortiumBlocks.SEAGRASS.getDefaultState();
            if (isWarmOcean(world.getBiome(target)) && rand.nextInt(4) == 0 && !PortiumBlocks.CORALS.isEmpty()) {
                toPlace = CoralPicker.plantOrFan(rand);
            }
            IBlockState existing = world.getBlockState(target);
            if (WaterUtil.isPlainWaterSource(existing) && world.isSideSolid(target.down(), EnumFacing.UP) && toPlace.getBlock().canPlaceBlockAt(world, target)) {
                world.setBlockState(target, toPlace, 3);
            } else if (existing.getBlock() == PortiumBlocks.SEAGRASS && rand.nextInt(10) == 0) {
                ((BlockSeagrass) PortiumBlocks.SEAGRASS).grow(world, rand, target, existing);
            }
        }
    }

    private static boolean isWarmOcean(Biome biome) {
        return biome instanceof BiomePortiumOcean && ((BiomePortiumOcean) biome).getOceanTemperature() == OceanTemperature.WARM;
    }

    private UnderwaterBonemeal() {}
}
