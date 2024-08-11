package com.michaelsebero.portium.modern.shim;

import com.michaelsebero.portium.modern.adapter.IModernBlock;
import com.michaelsebero.portium.modern.adapter.ModernBlockBed;
import com.michaelsebero.portium.modern.mc.block.Block$Properties;
import com.michaelsebero.portium.modern.mc.item.BlockItemUseContext;
import com.michaelsebero.portium.modern.mc.util.IItemProvider;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShape;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShapes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** 1.13's block APIs on 1.12 blocks. */
public final class BlockShims {

    /** Properties of a 1.13 block whose 1.12 constructor is running on this thread. */
    private static final ThreadLocal<Block$Properties> PENDING = new ThreadLocal<>();

    /**
     * For {@code new Block(Properties)}, which becomes 1.12's {@code new Block(Material, MapColor)}: the material, and
     * the properties set aside for the constructor to apply when it finishes.
     */
    public static Material material(Block$Properties properties) {
        PENDING.set(properties);
        return properties.getMaterial();
    }

    public static MapColor mapColor(Block$Properties properties) {
        return properties.getMapColor();
    }

    /** The properties set aside for the block being made, if it's a 1.13 block; clears them. */
    @Nullable
    public static Block$Properties takePending() {
        Block$Properties properties = PENDING.get();
        PENDING.remove();
        return properties;
    }

    // Methods 1.13 code calls on any block: a 1.13 mod's block answers itself, 1.12's blocks through 1.12's methods.

    public static int getStateId(IBlockState state) {
        return Block.getStateId(state);
    }

    public static IItemProvider getItemDropped(Block block, IBlockState state, World world, BlockPos pos, int fortune) {
        return (IItemProvider) block.getItemDropped(state, world.rand, fortune);
    }

    /** 1.13's {@code Block.makeCuboidShape}: a box in sixteenths of a block. */
    public static VoxelShape makeCuboidShape(double x1, double y1, double z1, double x2, double y2, double z2) {
        return VoxelShapes.create(x1 / 16.0D, y1 / 16.0D, z1 / 16.0D, x2 / 16.0D, y2 / 16.0D, z2 / 16.0D);
    }

    @SuppressWarnings("deprecation")
    public static VoxelShape getShape(Block block, IBlockState state, IBlockAccess world, BlockPos pos) {
        if (block instanceof IModernBlock) return ((IModernBlock) block).getShape(state, world, pos);
        return VoxelShapes.create(state.getBoundingBox(world, pos));
    }

    public static ItemStack getItem(Block block, IBlockAccess world, BlockPos pos, IBlockState state) {
        if (block instanceof IModernBlock) return ((IModernBlock) block).getItem(world, pos, state);
        return world instanceof World ? block.getItem((World) world, pos, state) : new ItemStack(block);
    }

    /** 1.13 beds are one block per colour; 1.12's one bed block is red when asked. */
    public static EnumDyeColor getBedColor(BlockBed bed) {
        return bed instanceof ModernBlockBed ? ((ModernBlockBed) bed).getColor() : EnumDyeColor.RED;
    }

    public static void dropBlockAsItem(IBlockState state, World world, BlockPos pos, int fortune) {
        state.getBlock().dropBlockAsItem(world, pos, state, fortune);
    }

    public static boolean isReplaceable(IBlockState state, BlockItemUseContext context) {
        return state.getBlock().isReplaceable(context.getWorld(), context.getPos());
    }

    public static boolean isValidPosition(IBlockState state, IBlockAccess world, BlockPos pos) {
        Block block = state.getBlock();
        if (block instanceof IModernBlock) return ((IModernBlock) block).isValidPosition(state, world, pos);
        return !(world instanceof World) || block.canPlaceBlockAt((World) world, pos);
    }

    /** 1.13's {@code with}, which returns its state type erased; 1.12's {@code withProperty}. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object with(IBlockState state, IProperty property, Comparable value) {
        return state.withProperty(property, value);
    }

    /** Forge 1.13's pick block takes a block reader ({@code IBlockAccess} on 1.12) where 1.12's takes a world. */
    public static ItemStack getPickBlock(Block block, IBlockState state, RayTraceResult target, IBlockAccess world, BlockPos pos, EntityPlayer player) {
        return block.getPickBlock(state, target, world instanceof World ? (World) world : player.world, pos, player);
    }

    private BlockShims() {}

    // Forge 1.13's block state methods, which Forge 1.12 has on the block.

    public static boolean isAir(IBlockState state) {
        return state.getMaterial() == net.minecraft.block.material.Material.AIR;
    }

    public static boolean isAir(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getBlock().isAir(state, world, pos);
    }

    public static boolean canBeConnectedTo(IBlockState state, IBlockAccess world, BlockPos pos, net.minecraft.util.EnumFacing facing) {
        return state.getBlock().canBeConnectedTo(world, pos, facing);
    }

    public static boolean hasTileEntity(IBlockState state) {
        return state.getBlock().hasTileEntity(state);
    }

    /** 1.13's {@code has}: whether the state has a property. */
    public static boolean has(IBlockState state, IProperty<?> property) {
        return state.getPropertyKeys().contains(property);
    }

    /** 1.13's {@code ITileEntityProvider.createNewTileEntity(IBlockReader)} on any provider. */
    @Nullable
    public static net.minecraft.tileentity.TileEntity createNewTileEntity(net.minecraft.block.ITileEntityProvider provider, IBlockAccess world) {
        if (provider instanceof IModernBlock) return ((IModernBlock) provider).createNewTileEntity(world);
        return provider.createNewTileEntity(world instanceof World ? (World) world : null, 0);
    }

    // 1.13's block state defaults whose 1.13 bodies read Block fields 1.12 doesn't have.

    public static int getOpacity(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getLightOpacity(world, pos);
    }

    public static boolean propagatesSkylightDown(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getLightOpacity(world, pos) == 0;
    }

    public static boolean isSolid(IBlockState state) {
        return state.isOpaqueCube();
    }

    public static boolean isSolid(Block block, IBlockState state) {
        return state.isOpaqueCube();
    }

    /** 1.12 doesn't cache opacity per state, so none varies in 1.13's sense. */
    public static boolean isVariableOpacity(Block block) {
        return false;
    }

    public static VoxelShape getCollisionShape(Block block, IBlockState state, IBlockAccess world, BlockPos pos) {
        if (block instanceof IModernBlock) return ((IModernBlock) block).getCollisionShape(state, world, pos);
        net.minecraft.util.math.AxisAlignedBB box = state.getCollisionBoundingBox(world, pos);
        return box == null ? VoxelShapes.empty() : VoxelShapes.create(box);
    }

    public static VoxelShape getCollisionShape(IBlockState state, IBlockAccess world, BlockPos pos) {
        return getCollisionShape(state.getBlock(), state, world, pos);
    }

    public static com.google.common.collect.ImmutableMap<IProperty<?>, Comparable<?>> getValues(IBlockState state) {
        return state.getProperties();
    }

    /**
     * 1.13's {@code getBlockState} from a world: the whole state, as 1.13 keeps it. 1.12 keeps some properties out of
     * the world (a door's hinge on its lower half, a fence's connections) and works them out in {@code getActualState}.
     */
    public static IBlockState getActualBlockState(IBlockAccess world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        try {
            return state.getActualState(world, pos);
        } catch (RuntimeException e) {
            return state;
        }
    }
}
