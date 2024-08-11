package com.michaelsebero.portium.world.gen.feature;

import com.michaelsebero.portium.block.coral.BlockCoralFan;
import com.michaelsebero.portium.block.coral.CoralType;
import com.michaelsebero.portium.block.water.IWaterloggable;
import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;

import java.util.Random;

/** Random live coral pieces for reefs and underwater bone meal, waterlogged. */
public final class CoralPicker {

    private static CoralType randomType(Random rand) {
        CoralType[] types = CoralType.values();
        return types[rand.nextInt(types.length)];
    }

    public static IBlockState block(Random rand) {
        return PortiumBlocks.CORAL_BLOCKS.get(randomType(rand)).getDefaultState();
    }

    /** A coral plant or a floor fan, like 1.13's {@code #corals} tag. */
    public static IBlockState plantOrFan(Random rand) {
        CoralType type = randomType(rand);
        IBlockState state = (rand.nextBoolean() ? PortiumBlocks.CORALS : PortiumBlocks.CORAL_FANS).get(type).getDefaultState();
        return state.withProperty(IWaterloggable.WATERLOGGED, true);
    }

    public static IBlockState wallFan(Random rand, EnumFacing facing) {
        return PortiumBlocks.CORAL_FANS.get(randomType(rand)).getDefaultState()
                .withProperty(BlockCoralFan.FACING, facing).withProperty(IWaterloggable.WATERLOGGED, true);
    }

    private CoralPicker() {}
}
