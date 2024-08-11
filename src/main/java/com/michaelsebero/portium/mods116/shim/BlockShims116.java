package com.michaelsebero.portium.mods116.shim;

import com.michaelsebero.portium.modern.adapter.IModernBlock;
import com.michaelsebero.portium.modern.mc.util.math.BlockRayTraceResult;
import com.michaelsebero.portium.modern.mc.util.math.shapes.ISelectionContext;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShape;
import com.michaelsebero.portium.modern.shim.BlockShims;
import com.michaelsebero.portium.mods116.adapter.Block116;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * 1.16's block and block state methods, called on any block: 1.16 blocks ({@link Block116}) answer for themselves,
 * 1.12's blocks through their own methods (a vanilla lever still flips when a mod's deployer uses it).
 */
public final class BlockShims116 {

    // Shapes

    public static VoxelShape getShape(Block block, IBlockState state, IBlockAccess world, BlockPos pos, ISelectionContext context) {
        if (block instanceof Block116) return ((Block116) block).getShape(state, world, pos, context);
        return BlockShims.getShape(block, state, world, pos);
    }

    public static VoxelShape getCollisionShape(Block block, IBlockState state, IBlockAccess world, BlockPos pos, ISelectionContext context) {
        if (block instanceof Block116) return ((Block116) block).getCollisionShape(state, world, pos, context);
        return BlockShims.getCollisionShape(block, state, world, pos);
    }

    public static VoxelShape getShape(IBlockState state, IBlockAccess world, BlockPos pos) {
        return getShape(state.getBlock(), state, world, pos, ISelectionContext.dummy());
    }

    public static VoxelShape getShape(IBlockState state, IBlockAccess world, BlockPos pos, ISelectionContext context) {
        return getShape(state.getBlock(), state, world, pos, context);
    }

    public static VoxelShape getCollisionShape(IBlockState state, IBlockAccess world, BlockPos pos) {
        return getCollisionShape(state.getBlock(), state, world, pos, ISelectionContext.dummy());
    }

    public static VoxelShape getCollisionShape(IBlockState state, IBlockAccess world, BlockPos pos, ISelectionContext context) {
        return getCollisionShape(state.getBlock(), state, world, pos, context);
    }

    public static VoxelShape getRenderShape(IBlockState state, IBlockAccess world, BlockPos pos) {
        Block block = state.getBlock();
        if (block instanceof Block116) return ((Block116) block).getRenderShape(state, world, pos);
        return getShape(block, state, world, pos, ISelectionContext.dummy());
    }

    public static VoxelShape getRayTraceShape(IBlockState state, IBlockAccess world, BlockPos pos, ISelectionContext context) {
        Block block = state.getBlock();
        if (block instanceof Block116) return ((Block116) block).getRayTraceShape(state, world, pos, context);
        return getCollisionShape(block, state, world, pos, context);
    }

    // Clicks

    public static EnumActionResult onBlockActivated(Block block, IBlockState state, World world, BlockPos pos, EntityPlayer player, EnumHand hand,
                                                    BlockRayTraceResult hit) {
        if (block instanceof Block116) return ((Block116) block).onBlockActivated(state, world, pos, player, hand, hit);
        Vec3d at = hit.getHitVec();
        boolean used = block.onBlockActivated(world, pos, state, player, hand, hit.getFace(), (float) (at.x - pos.getX()), (float) (at.y - pos.getY()),
                (float) (at.z - pos.getZ()));
        return used ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
    }

    public static EnumActionResult onBlockActivated(IBlockState state, World world, EntityPlayer player, EnumHand hand, BlockRayTraceResult hit) {
        return onBlockActivated(state.getBlock(), state, world, hit.getPos(), player, hand, hit);
    }

    public static void onBlockClicked(IBlockState state, World world, BlockPos pos, EntityPlayer player) {
        Block block = state.getBlock();
        if (block instanceof Block116) {
            ((Block116) block).onBlockClicked(state, world, pos, player);
        } else {
            block.onBlockClicked(world, pos, player);
        }
    }

    // Updates

    /** 1.16's shape update; 1.12's blocks don't change shape for their neighbours. */
    public static IBlockState updatePostPlacement(IBlockState state, EnumFacing facing, IBlockState facingState, World world, BlockPos pos,
                                                  BlockPos facingPos) {
        Block block = state.getBlock();
        if (block instanceof IModernBlock) return ((IModernBlock) block).updatePostPlacement(state, facing, facingState, world, pos, facingPos);
        return state;
    }

    public static void neighborChanged(IBlockState state, World world, BlockPos pos, Block from, BlockPos fromPos, boolean isMoving) {
        Block block = state.getBlock();
        if (block instanceof Block116) {
            ((Block116) block).neighborChanged(state, world, pos, from, fromPos, isMoving);
        } else {
            state.neighborChanged(world, pos, from, fromPos);
        }
    }

    public static boolean isValidPosition(IBlockState state, World world, BlockPos pos) {
        return BlockShims.isValidPosition(state, world, pos);
    }

    // Forge's

    public static ItemStack getPickBlock(IBlockState state, RayTraceResult target, IBlockAccess world, BlockPos pos, EntityPlayer player) {
        Block block = state.getBlock();
        if (block instanceof Block116) return ((Block116) block).getPickBlock(state, target, world, pos, player);
        return block.getPickBlock(state, target, world instanceof World ? (World) world : player.world, pos, player);
    }

    private BlockShims116() {}
}
