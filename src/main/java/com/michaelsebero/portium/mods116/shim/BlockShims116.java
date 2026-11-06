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

    /** 1.16's {@code RedstoneWireBlock.getRGBByPower}: 1.12's colour of wire at that power. */
    public static int redstoneColor(int power) {
        return net.minecraft.block.BlockRedstoneWire.colorMultiplier(power);
    }

    // Forge 1.16's block state methods (IForgeBlockState), on Forge 1.12's block methods they come from.

    public static net.minecraft.block.SoundType soundType(IBlockState state, net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos,
                                                          @javax.annotation.Nullable net.minecraft.entity.Entity entity) {
        return state.getBlock().getSoundType(state, world, pos, entity);
    }

    public static float slipperiness(IBlockState state, net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos,
                                     @javax.annotation.Nullable net.minecraft.entity.Entity entity) {
        return state.getBlock().getSlipperiness(state, world, pos, entity);
    }

    public static IBlockState rotate(IBlockState state, net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, net.minecraft.util.Rotation rotation) {
        return state.withRotation(rotation);
    }

    @javax.annotation.Nullable
    public static net.minecraft.tileentity.TileEntity createTileEntity(IBlockState state, net.minecraft.world.IBlockAccess world) {
        return state.getBlock().createTileEntity(world instanceof net.minecraft.world.World ? (net.minecraft.world.World) world : null, state);
    }

    public static boolean removedByPlayer(IBlockState state, net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, EntityPlayer player,
                                          boolean willHarvest, Object fluid) {
        return state.getBlock().removedByPlayer(state, world, pos, player, willHarvest);
    }

    public static boolean canSustainPlant(IBlockState state, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos,
                                          net.minecraft.util.EnumFacing facing, net.minecraftforge.common.IPlantable plantable) {
        return state.getBlock().canSustainPlant(state, world, pos, facing, plantable);
    }

    /** Forge 1.16: whether two blocks a piston moves stick together (slime; 1.12 has no honey). */
    public static boolean canStickTo(IBlockState state, IBlockState other) {
        return state.getBlock().isStickyBlock(state) || other.getBlock().isStickyBlock(other);
    }

    public static boolean canHarvestBlock(IBlockState state, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos, EntityPlayer player) {
        return state.getBlock().canHarvestBlock(world, pos, player);
    }

    public static boolean addRunningEffects(IBlockState state, net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, net.minecraft.entity.Entity entity) {
        return state.getBlock().addRunningEffects(state, world, pos, entity);
    }

    @javax.annotation.Nullable
    private static java.lang.reflect.Field mapColor;

    /** 1.16's {@code block.getMaterialColor()}: 1.12's block map colour (its default state's, else the block's own). */
    public static net.minecraft.block.material.MapColor materialColor(net.minecraft.block.Block block) {
        try {
            return block.getDefaultState().getMapColor((net.minecraft.world.IBlockAccess) null, net.minecraft.util.math.BlockPos.ORIGIN);
        } catch (RuntimeException e) {
            try {
                if (mapColor == null) {
                    mapColor = net.minecraftforge.fml.common.ObfuscationReflectionHelper.findField(net.minecraft.block.Block.class, "field_181083_K");
                }
                return (net.minecraft.block.material.MapColor) mapColor.get(block);
            } catch (ReflectiveOperationException | RuntimeException again) {
                return net.minecraft.block.material.MapColor.AIR;
            }
        }
    }

    public static net.minecraft.dispenser.IBehaviorDispenseItem noDispense() {
        return net.minecraft.dispenser.IBehaviorDispenseItem.DEFAULT_BEHAVIOR;
    }

    /** 1.16's {@code Block.getDrops(state, world, pos, tile[, entity, tool])}: 1.12's drops, with the tool's fortune. */
    public static java.util.List<net.minecraft.item.ItemStack> drops(net.minecraft.block.state.IBlockState state, net.minecraft.world.World world,
            net.minecraft.util.math.BlockPos pos, @javax.annotation.Nullable net.minecraft.tileentity.TileEntity tile) {
        return drops(state, world, pos, tile, null, net.minecraft.item.ItemStack.EMPTY);
    }

    public static java.util.List<net.minecraft.item.ItemStack> drops(net.minecraft.block.state.IBlockState state, net.minecraft.world.World world,
            net.minecraft.util.math.BlockPos pos, @javax.annotation.Nullable net.minecraft.tileentity.TileEntity tile, @javax.annotation.Nullable net.minecraft.entity.Entity entity,
            net.minecraft.item.ItemStack tool) {
        net.minecraft.util.NonNullList<net.minecraft.item.ItemStack> drops = net.minecraft.util.NonNullList.create();
        int fortune = tool == null || tool.isEmpty() ? 0 : net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.init.Enchantments.FORTUNE, tool);
        state.getBlock().getDrops(drops, world, pos, state, fortune);
        return drops;
    }

    /** 1.16's {@code Block.spawnDrops(state, world, pos[, tile[, entity, tool]])}. */
    public static void spawnDrops(net.minecraft.block.state.IBlockState state, net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos) {
        spawnDrops(state, world, pos, null, null, net.minecraft.item.ItemStack.EMPTY);
    }

    public static void spawnDrops(net.minecraft.block.state.IBlockState state, net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos,
            @javax.annotation.Nullable net.minecraft.tileentity.TileEntity tile) {
        spawnDrops(state, world, pos, tile, null, net.minecraft.item.ItemStack.EMPTY);
    }

    public static void spawnDrops(net.minecraft.block.state.IBlockState state, net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos,
            @javax.annotation.Nullable net.minecraft.tileentity.TileEntity tile, @javax.annotation.Nullable net.minecraft.entity.Entity entity, net.minecraft.item.ItemStack tool) {
        if (world.isRemote) return;
        for (net.minecraft.item.ItemStack drop : drops(state, world, pos, tile, entity, tool)) net.minecraft.block.Block.spawnAsEntity(world, pos, drop);
    }

    /** Forge 1.16's {@code ForgeHooks.getBurnTime(stack[, recipeType])}: 1.12's furnace fuel value. */
    public static int burnTime(net.minecraft.item.ItemStack stack) {
        return net.minecraft.tileentity.TileEntityFurnace.getItemBurnTime(stack);
    }

    public static int burnTime(net.minecraft.item.ItemStack stack, Object recipeType) {
        return burnTime(stack);
    }

    /** Forge 1.16's {@code ForgeHooks.onRightClickBlock(player, hand, pos, hit)}. */
    public static net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock rightClickBlock(net.minecraft.entity.player.EntityPlayer player,
            net.minecraft.util.EnumHand hand, net.minecraft.util.math.BlockPos pos, net.minecraft.util.math.RayTraceResult hit) {
        return net.minecraftforge.common.ForgeHooks.onRightClickBlock(player, hand, pos, hit.sideHit, hit.hitVec);
    }

    /**
     * {@code x instanceof AbstractFurnaceBlock}: 1.16's furnaces, 1.12's furnace (lit or not) and Portium's blast
     * furnace and smoker.
     */
    public static boolean isFurnace(Object block) {
        if (block instanceof net.minecraft.block.BlockFurnace || block instanceof com.michaelsebero.portium.block.workstation.BlockCookingFurnace) return true;
        for (Class<?> type = block == null ? null : block.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().endsWith("net.minecraft.block.AbstractFurnaceBlock")) return true;
        }
        return false;
    }
}
