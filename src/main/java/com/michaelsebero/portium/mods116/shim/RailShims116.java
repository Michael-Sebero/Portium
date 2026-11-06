package com.michaelsebero.portium.mods116.shim;

import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Forge 1.16's rail hooks, which take the state first, called on any rail: 1.12 Forge's of the same purpose (rails of
 * 1.16 mods hand those on to their own; see {@code AbstractRailBlock116}).
 */
public final class RailShims116 {

    public static BlockRailBase.EnumRailDirection getRailDirection(BlockRailBase rail, IBlockState state, IBlockAccess world, BlockPos pos,
                                                                  @Nullable EntityMinecart cart) {
        return rail.getRailDirection(world, pos, state, cart);
    }

    public static float getRailMaxSpeed(BlockRailBase rail, IBlockState state, World world, BlockPos pos, EntityMinecart cart) {
        return rail.getRailMaxSpeed(world, cart, pos);
    }

    public static boolean isFlexibleRail(BlockRailBase rail, IBlockState state, IBlockAccess world, BlockPos pos) {
        return rail.isFlexibleRail(world, pos);
    }

    public static boolean canMakeSlopes(BlockRailBase rail, IBlockState state, IBlockAccess world, BlockPos pos) {
        return rail.canMakeSlopes(world, pos);
    }

    public static void onMinecartPass(BlockRailBase rail, IBlockState state, World world, BlockPos pos, EntityMinecart cart) {
        rail.onMinecartPass(world, cart, pos);
    }

    private RailShims116() {}
}
