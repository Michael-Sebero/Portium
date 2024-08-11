package com.michaelsebero.portium.handler;

import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;
import java.util.Map;

/** Which blocks an axe strips into which (1.13): logs into stripped logs, wood into stripped wood. */
public final class Stripping {

    @Nullable
    public static IBlockState strip(IBlockState state) {
        Block block = state.getBlock();
        Block stripped = strippedNether(block);
        if (stripped != null) return withAxis(stripped, state.getValue(BlockRotatedPillar.AXIS));
        if (PortiumBlocks.STRIPPED_LOGS.isEmpty()) return null;
        if (block == Blocks.LOG) return fromVanillaLog(state, state.getValue(BlockOldLog.VARIANT));
        if (block == Blocks.LOG2) return fromVanillaLog(state, state.getValue(BlockNewLog.VARIANT));
        for (Map.Entry<BlockPlanks.EnumType, Block> entry : PortiumBlocks.WOOD.entrySet()) {
            if (entry.getValue() == block) return withAxis(PortiumBlocks.STRIPPED_WOOD.get(entry.getKey()), state.getValue(BlockRotatedPillar.AXIS));
        }
        return null;
    }

    private static IBlockState fromVanillaLog(IBlockState state, BlockPlanks.EnumType wood) {
        BlockLog.EnumAxis axis = state.getValue(BlockLog.LOG_AXIS);
        // 1.12's all-bark log (axis NONE) is 1.13's wood block.
        if (axis == BlockLog.EnumAxis.NONE) return withAxis(PortiumBlocks.STRIPPED_WOOD.get(wood), EnumFacing.Axis.Y);
        EnumFacing.Axis pillarAxis = axis == BlockLog.EnumAxis.X ? EnumFacing.Axis.X : axis == BlockLog.EnumAxis.Z ? EnumFacing.Axis.Z : EnumFacing.Axis.Y;
        return withAxis(PortiumBlocks.STRIPPED_LOGS.get(wood), pillarAxis);
    }

    /** 1.16's crimson and warped stems and hyphae. */
    @Nullable
    private static Block strippedNether(Block block) {
        if (block == null || PortiumBlocks.CRIMSON_STEM == null) return null;
        if (block == PortiumBlocks.CRIMSON_STEM) return PortiumBlocks.STRIPPED_CRIMSON_STEM;
        if (block == PortiumBlocks.WARPED_STEM) return PortiumBlocks.STRIPPED_WARPED_STEM;
        if (block == PortiumBlocks.CRIMSON_HYPHAE) return PortiumBlocks.STRIPPED_CRIMSON_HYPHAE;
        if (block == PortiumBlocks.WARPED_HYPHAE) return PortiumBlocks.STRIPPED_WARPED_HYPHAE;
        return null;
    }

    private static IBlockState withAxis(Block block, EnumFacing.Axis axis) {
        return block.getDefaultState().withProperty(BlockRotatedPillar.AXIS, axis);
    }

    private Stripping() {}
}
