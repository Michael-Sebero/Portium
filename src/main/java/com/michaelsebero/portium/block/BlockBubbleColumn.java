package com.michaelsebero.portium.block;

import com.michaelsebero.portium.block.water.WaterUtil;
import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.proxy.ParticleKind;
import com.michaelsebero.portium.entity.BubbleColumnPhysics;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * Bubble columns (1.13). A still water source that pushes entities up (above soul sand) or pulls them down
 * ({@link #DRAG}, above magma). Columns extend upward through still water sources.
 */
public class BlockBubbleColumn extends BlockStaticLiquid {

    /** True for a downward whirlpool over magma, false for an upward column over soul sand. */
    public static final PropertyBool DRAG = PropertyBool.create("drag");

    public BlockBubbleColumn() {
        super(Material.WATER);
        this.setDefaultState(this.blockState.getBaseState().withProperty(LEVEL, 0).withProperty(DRAG, true));
        this.setHardness(100.0F);
        this.setLightOpacity(3);
        this.disableStats();
        this.setTickRandomly(false);
    }

    // ---- Placement ----

    /** Whether a column may extend into {@code pos}: only a still water source block. */
    public static boolean canHoldBubbleColumn(World world, BlockPos pos) {
        return !world.provider.doesWaterVaporize() && WaterUtil.isPlainWaterSource(world.getBlockState(pos));
    }

    public static void placeBubbleColumn(World world, BlockPos pos, boolean drag) {
        if (PortiumBlocks.BUBBLE_COLUMN != null && canHoldBubbleColumn(world, pos)) {
            world.setBlockState(pos, PortiumBlocks.BUBBLE_COLUMN.getDefaultState().withProperty(DRAG, drag), 2);
        }
    }

    /** A column above magma, or above another downward column, drags down; soul sand pushes up. */
    private static boolean getDrag(IBlockAccess world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == PortiumBlocks.BUBBLE_COLUMN) return state.getValue(DRAG);
        return state.getBlock() != Blocks.SOUL_SAND;
    }

    private static boolean hasValidBase(IBlockAccess world, BlockPos pos) {
        Block below = world.getBlockState(pos.down()).getBlock();
        return below == PortiumBlocks.BUBBLE_COLUMN || below == Blocks.MAGMA || below == Blocks.SOUL_SAND;
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        placeBubbleColumn(world, pos.up(), getDrag(world, pos.down()));
    }

    @Override
    public int tickRate(World world) {
        return 5;
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        placeBubbleColumn(world, pos.up(), getDrag(world, pos));
    }

    /**
     * Replaces vanilla's still-water behaviour, which would turn this block into flowing water on any neighbour update.
     */
    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (!hasValidBase(world, pos)) {
            world.setBlockState(pos, WaterUtil.waterSource(), 3);
            return;
        }
        if (fromPos.equals(pos.down())) {
            boolean drag = getDrag(world, fromPos);
            if (drag != state.getValue(DRAG)) world.setBlockState(pos, state.withProperty(DRAG, drag), 2);
        } else if (fromPos.equals(pos.up())) {
            if (world.getBlockState(fromPos).getBlock() != this && canHoldBubbleColumn(world, fromPos)) {
                world.scheduleUpdate(pos, this, this.tickRate(world));
            }
        } else {
            this.spreadInto(world, fromPos);
        }
    }

    /** Lets water flow out of the column like it would from any other source. */
    private void spreadInto(World world, BlockPos target) {
        IBlockState targetState = world.getBlockState(target);
        Material material = targetState.getMaterial();
        if (material.isLiquid() || material.blocksMovement() || material == Material.PORTAL || material == Material.STRUCTURE_VOID) return;
        if (!targetState.getBlock().isReplaceable(world, target)) return;
        if (material != Material.AIR) targetState.getBlock().dropBlockAsItem(world, target, targetState, 0);
        world.setBlockState(target, Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL, 1), 3);
    }

    // ---- Entities ----

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        boolean drag = state.getValue(DRAG);
        if (world.isAirBlock(pos.up())) {
            BubbleColumnPhysics.onSurface(entity, drag);
            if (world instanceof WorldServer) {
                WorldServer server = (WorldServer) world;
                for (int i = 0; i < 2; ++i) {
                    server.spawnParticle(EnumParticleTypes.WATER_SPLASH, pos.getX() + world.rand.nextFloat(), pos.getY() + 1.0D, pos.getZ() + world.rand.nextFloat(), 1, 0.0D, 0.0D, 0.0D, 1.0D);
                    server.spawnParticle(EnumParticleTypes.WATER_BUBBLE, pos.getX() + world.rand.nextFloat(), pos.getY() + 1.0D, pos.getZ() + world.rand.nextFloat(), 1, 0.0D, 0.01D, 0.0D, 0.2D);
                }
            }
        } else {
            BubbleColumnPhysics.inside(entity, drag);
        }
    }

    // ---- Client ----

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        if (state.getValue(DRAG)) {
            Portium.proxy.spawnParticle(ParticleKind.CURRENT_DOWN, world, x + 0.5, y + 0.8, z, 0, 0, 0);
            if (rand.nextInt(200) == 0) {
                world.playSound(x, y, z, PortiumSounds.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, SoundCategory.BLOCKS, 0.2F + rand.nextFloat() * 0.2F, 0.9F + rand.nextFloat() * 0.15F, false);
            }
        } else {
            Portium.proxy.spawnParticle(ParticleKind.BUBBLE_COLUMN_UP, world, x + 0.5, y, z + 0.5, 0, 0.04, 0);
            Portium.proxy.spawnParticle(ParticleKind.BUBBLE_COLUMN_UP, world, x + rand.nextFloat(), y + rand.nextFloat(), z + rand.nextFloat(), 0, 0.04, 0);
            if (rand.nextInt(200) == 0) {
                world.playSound(x, y, z, PortiumSounds.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundCategory.BLOCKS, 0.2F + rand.nextFloat() * 0.2F, 0.9F + rand.nextFloat() * 0.15F, false);
            }
        }
    }

    // ---- State ----

    @Nonnull
    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(DRAG, (meta & 1) == 1);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(DRAG) ? 1 : 0;
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, LEVEL, DRAG);
    }
}
