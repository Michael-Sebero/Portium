package com.michaelsebero.portium.modern.adapter;

import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShape;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShapes;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;

/** What Portium's block adapters share: 1.13's shapes, states and names in 1.12's terms. */
public final class ModernBlocks {

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.13.2");
    private static final AxisAlignedBB NOTHING = new AxisAlignedBB(0, 0, 0, 0, 0, 0);

    /** For 1.12's getBoundingBox: the shape's bounds (a point for an empty shape, which 1.12 can't have). */
    public static AxisAlignedBB bounds(VoxelShape shape) {
        return shape.isEmpty() ? NOTHING : shape.getBoundingBox();
    }

    /** For 1.12's getCollisionBoundingBox. */
    @Nullable
    public static AxisAlignedBB collisionBounds(VoxelShape shape) {
        return shape.isEmpty() ? Block.NULL_AABB : shape.getBoundingBox();
    }

    /** For 1.12's addCollisionBoxToList: each box of the shape that meets the entity's. */
    public static void addCollisionBoxes(VoxelShape shape, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> boxes) {
        for (AxisAlignedBB box : shape.toBoundingBoxList()) {
            AxisAlignedBB placed = box.offset(pos);
            if (entityBox.intersects(placed)) boxes.add(placed);
        }
    }

    public static BlockFaceShape faceShape(VoxelShape shape) {
        return shape.isFullCube() ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    /** The shape a block's state has without its world, for 1.12's opacity checks, which have none. */
    public static VoxelShape shapeOf(IModernBlock block, IBlockState state) {
        try {
            return block.getShape(state, null, BlockPos.ORIGIN);
        } catch (RuntimeException e) {
            return VoxelShapes.fullCube();
        }
    }

    public static boolean anyValidPosition(Block block, World world, BlockPos pos) {
        for (IBlockState state : block.getBlockState().getValidStates()) {
            if (((IModernBlock) block).isValidPosition(state, world, pos)) return true;
        }
        return false;
    }

    // States: 1.13 blocks have no metadata; 1.12 needs one per state that's saved, so states are numbered in order.

    public static int metaFromState(Block block, IBlockState state) {
        List<IBlockState> states = block.getBlockState().getValidStates();
        int index = states.indexOf(state);
        if (states.size() > 16) {
            warnTooManyStates(block, states.size());
            return Math.max(0, Math.min(index, 15));
        }
        return Math.max(index, 0);
    }

    public static IBlockState stateFromMeta(Block block, int meta) {
        List<IBlockState> states = block.getBlockState().getValidStates();
        return meta >= 0 && meta < states.size() ? states.get(meta) : block.getDefaultState();
    }

    private static final java.util.Set<Block> WARNED = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    private static void warnTooManyStates(Block block, int count) {
        if (WARNED.add(block)) LOGGER.warn("{} has {} states; 1.12 saves 16 per block, so the rest save as one of those", block.getRegistryName(), count);
    }

    // Shape updates: 1.13 asks a block how a neighbour's change affects it; 1.12 tells it a neighbour changed.

    /** After 1.12's neighbour change: the 1.13 block's own update for that side, applied. */
    public static void applyShapeUpdate(IModernBlock block, World world, BlockPos pos, BlockPos fromPos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != block) return;
        EnumFacing facing = facingTowards(pos, fromPos);
        if (facing == null) return;
        IBlockState updated = block.updatePostPlacement(state, facing, world.getBlockState(fromPos), world, pos, fromPos);
        if (updated == state) return;
        if (updated.getBlock() == Blocks.AIR) {
            if (!world.isRemote) world.destroyBlock(pos, true);
        } else {
            world.setBlockState(pos, updated, 3);
        }
    }

    /** The side of {@code pos} that {@code other} is on, if it's a direct neighbour. */
    @Nullable
    private static EnumFacing facingTowards(BlockPos pos, BlockPos other) {
        for (EnumFacing facing : EnumFacing.values()) {
            if (pos.offset(facing).equals(other)) return facing;
        }
        return null;
    }

    // Names: 1.13 names blocks block.<namespace>.<path> and translates that key as it is.

    public static String translationKey(Block block, String fallback) {
        ResourceLocation name = block.getRegistryName();
        return name == null ? fallback : "block." + name.getNamespace() + "." + name.getPath().replace('/', '.');
    }

    public static String localize(String key) {
        return I18n.translateToLocal(key);
    }

    private ModernBlocks() {}
}
