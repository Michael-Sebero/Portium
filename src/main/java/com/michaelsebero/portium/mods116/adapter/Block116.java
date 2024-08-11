package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.adapter.ModernBlock;
import com.michaelsebero.portium.modern.adapter.ModernBlocks;
import com.michaelsebero.portium.modern.mc.block.Block$Properties;
import com.michaelsebero.portium.modern.mc.util.math.BlockRayTraceResult;
import com.michaelsebero.portium.modern.mc.util.math.shapes.ISelectionContext;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShape;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShapes;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * What a 1.16 mod's blocks extend in place of 1.16's {@code Block}, as do Minecraft 1.16.5's own block classes that
 * mods build on: Portium's 1.13 block with what 1.16 changed. Shapes are asked with a selection context, clicks get a
 * block ray trace result and an action result, ticks a server world, and neighbour changes, additions and
 * replacements carry a flag for blocks moved by pistons. 1.12's methods call these, so a mod's overrides take effect;
 * these keep 1.16's defaults (Forge's where Forge patches them), for mods that call {@code super}.
 * <p>
 * 1.12 tells a block it was added or replaced only when the block changes; 1.16 tells it on every change of state,
 * which {@code MixinChunkBlock116} adds. 1.16's diagonal neighbour updates come from {@code MixinWorldBlock116}.
 */
public class Block116 extends ModernBlock {

    public Block116(Block$Properties properties) {
        super(properties);
    }

    // Shapes: 1.16 asks with who's asking; 1.13's methods, which 1.12's call, ask as nobody.

    @Srg("func_220053_a")
    public VoxelShape getShape(IBlockState state, IBlockAccess world, BlockPos pos, ISelectionContext context) {
        return VoxelShapes.fullCube();
    }

    @Srg("func_220071_b")
    public VoxelShape getCollisionShape(IBlockState state, IBlockAccess world, BlockPos pos, ISelectionContext context) {
        return portium$canCollide() ? getShape(state, world, pos, ISelectionContext.dummy()) : VoxelShapes.empty();
    }

    @Srg("func_230322_a_")
    public VoxelShape getRayTraceShape(IBlockState state, IBlockAccess world, BlockPos pos, ISelectionContext context) {
        return getCollisionShape(state, world, pos, context);
    }

    /** The extra shape a ray hits first (a cauldron's inside); none by default. */
    @Srg("func_199600_g")
    public VoxelShape getRaytraceShape(IBlockState state, IBlockAccess world, BlockPos pos) {
        return VoxelShapes.empty();
    }

    /** The shape that hides neighbouring faces and casts ambient occlusion. */
    @Srg("func_196247_c")
    public VoxelShape getRenderShape(IBlockState state, IBlockAccess world, BlockPos pos) {
        return getShape(state, world, pos, ISelectionContext.dummy());
    }

    @Override
    public VoxelShape getShape(IBlockState state, IBlockAccess world, BlockPos pos) {
        return getShape(state, world, pos, ISelectionContext.dummy());
    }

    @Override
    public VoxelShape getCollisionShape(IBlockState state, IBlockAccess world, BlockPos pos) {
        return getCollisionShape(state, world, pos, ISelectionContext.dummy());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> boxes,
                                      @Nullable Entity entity, boolean isActualState) {
        ISelectionContext context = entity == null ? ISelectionContext.dummy() : ISelectionContext.forEntity(entity);
        ModernBlocks.addCollisionBoxes(getCollisionShape(state, world, pos, context), pos, entityBox, boxes);
    }

    // Clicks

    @Srg("func_225533_a_")
    public EnumActionResult onBlockActivated(IBlockState state, World world, BlockPos pos, EntityPlayer player, EnumHand hand, BlockRayTraceResult hit) {
        return EnumActionResult.PASS;
    }

    /** 1.13's click, which 1.12's calls: 1.16's, with the hit as a ray trace result. Success or consumption is handled. */
    @Override
    public boolean onBlockActivated(IBlockState state, World world, BlockPos pos, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX,
                                    float hitY, float hitZ) {
        BlockRayTraceResult hit = new BlockRayTraceResult(new Vec3d(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ), side, pos, false);
        return onBlockActivated(state, world, pos, player, hand, hit) == EnumActionResult.SUCCESS;
    }

    @Srg("func_196270_a")
    public void onBlockClicked(IBlockState state, World world, BlockPos pos, EntityPlayer player) {}

    @Override
    public void onBlockClicked(World world, BlockPos pos, EntityPlayer player) {
        onBlockClicked(world.getBlockState(pos), world, pos, player);
    }

    // Neighbours, additions and replacements

    @Srg("func_220069_a")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {}

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        neighborChanged(state, world, pos, block, fromPos, false);
        ModernBlocks.applyShapeUpdate(this, world, pos, fromPos);
    }

    /** Told after a neighbour's change, two blocks out (redstone wire's corners); nothing by default. */
    @Srg("func_196248_b")
    public void updateDiagonalNeighbors(IBlockState state, World world, BlockPos pos, int flags, int recursionLeft) {}

    @Srg("func_220082_b")
    public void onBlockAdded(IBlockState state, World world, BlockPos pos, IBlockState oldState, boolean isMoving) {}

    /** 1.12's, once the block has changed; what was there before isn't told, but was another block. */
    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        onBlockAdded(state, world, pos, Blocks.AIR.getDefaultState(), false);
    }

    /** Forge 1.16's default: a block's tile entity goes with the block, or with the state's having one. */
    @Srg("func_196243_a")
    public void onReplaced(IBlockState state, World world, BlockPos pos, IBlockState newState, boolean isMoving) {
        if (hasTileEntity(state) && (state.getBlock() != newState.getBlock() || !newState.getBlock().hasTileEntity(newState))) world.removeTileEntity(pos);
    }

    /** 1.12's, once the block has changed and the new state is set. */
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        onReplaced(state, world, pos, world.getBlockState(pos), false);
    }

    // Ticks

    @Srg("func_225534_a_")
    public void tick(IBlockState state, WorldServer world, BlockPos pos, Random random) {}

    @Srg("func_225542_b_")
    public void randomTick(IBlockState state, WorldServer world, BlockPos pos, Random random) {
        tick(state, world, pos, random);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (world instanceof WorldServer) tick(state, (WorldServer) world, pos, random);
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (world instanceof WorldServer) randomTick(state, (WorldServer) world, pos, random);
    }

    // Entities

    @Srg("func_196262_a")
    public void onEntityCollision(IBlockState state, World world, BlockPos pos, Entity entity) {}

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        onEntityCollision(state, world, pos, entity);
    }

    /** Told when an entity lands on the block (a millstone takes dropped items so); 1.16 asks with a block reader. */
    @Srg("func_176216_a")
    public void onLanded(IBlockAccess world, Entity entity) {
        entity.motionY = 0.0D;
    }

    @Override
    public void onLanded(World world, Entity entity) {
        onLanded((IBlockAccess) world, entity);
    }

    // Sounds, picking and Forge's additions under 1.16's parameters

    @Srg("func_220072_p")
    @SuppressWarnings("deprecation")
    public SoundType getSoundType(IBlockState state) {
        return getSoundType();
    }

    /** Forge's, which 1.12 asks; 1.16's by default asks the state-only one. */
    @Override
    public SoundType getSoundType(IBlockState state, World world, BlockPos pos, @Nullable Entity entity) {
        return getSoundType(state);
    }

    /** Forge 1.16's, taking a block reader. */
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, IBlockAccess world, BlockPos pos, EntityPlayer player) {
        return getItem(world, pos, state);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return getPickBlock(state, target, (IBlockAccess) world, pos, player);
    }

    /** Forge 1.16's, taking a world reader. */
    public boolean shouldCheckWeakPower(IBlockState state, World world, BlockPos pos, EnumFacing side) {
        return super.shouldCheckWeakPower(state, world, pos, side);
    }

    @Override
    public boolean shouldCheckWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return world instanceof World ? shouldCheckWeakPower(state, (World) world, pos, side) : super.shouldCheckWeakPower(state, world, pos, side);
    }

    /** Forge 1.16's, with the state first. */
    @SideOnly(Side.CLIENT)
    public boolean addDestroyEffects(IBlockState state, World world, BlockPos pos, ParticleManager manager) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean addDestroyEffects(World world, BlockPos pos, ParticleManager manager) {
        return addDestroyEffects(world.getBlockState(pos), world, pos, manager);
    }
}
