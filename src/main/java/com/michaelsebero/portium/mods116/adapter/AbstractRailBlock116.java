package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.adapter.IBlockPropertyHolder;
import com.michaelsebero.portium.modern.adapter.ModernBlocks;
import com.michaelsebero.portium.modern.mc.block.Block$Properties;
import com.michaelsebero.portium.modern.mc.state.StateContainer$Builder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;

/**
 * What a 1.16 mod's rails extend instead of 1.12's {@code BlockRailBase}: 1.12's rail, so minecarts ride it, made
 * from 1.16's properties with 1.16's state container.
 */
public abstract class AbstractRailBlock116 extends BlockRailBase implements com.michaelsebero.portium.api.IManyStateBlock {

    public AbstractRailBlock116(boolean disableCorners, Block$Properties properties) {
        super(disableCorners);
        ((IBlockPropertyHolder) this).portium$applyBlockProperties(properties);
    }

    @Srg("func_206840_a")
    protected void fillStateContainer(StateContainer$Builder<Block, IBlockState> builder) {}

    @Override
    protected BlockStateContainer createBlockState() {
        StateContainer$Builder<Block, IBlockState> builder = new StateContainer$Builder<>(this);
        fillStateContainer(builder);
        return builder.build();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return ModernBlocks.metaFromState(this, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return ModernBlocks.stateFromMeta(this, meta);
    }

    @Override
    public String getTranslationKey() {
        return ModernBlocks.translationKey(this, super.getTranslationKey());
    }

    @Override
    public String getLocalizedName() {
        return ModernBlocks.localize(getTranslationKey());
    }

    /** 1.16's: the rail's shape worked out anew with its neighbours (1.12's {@code updateDir}, the same). */
    @Srg("func_208489_a")
    protected IBlockState updateDir116(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, IBlockState state, boolean placing) {
        return updateDir(world, pos, state, placing);
    }

    // 1.16's block events, which 1.12's hand on to (as Block116's do); by default 1.12's rail behaviour.

    @Srg("func_220069_a")
    public void neighborChanged(IBlockState state, net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, Block block,
                                net.minecraft.util.math.BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos);
    }

    @Override
    public void neighborChanged(IBlockState state, net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, Block block,
                                net.minecraft.util.math.BlockPos fromPos) {
        neighborChanged(state, world, pos, block, fromPos, false);
    }

    @Srg("func_220082_b")
    public void onBlockAdded(IBlockState state, net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, IBlockState oldState, boolean isMoving) {
        super.onBlockAdded(world, pos, state);
    }

    @Override
    public void onBlockAdded(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, IBlockState state) {
        onBlockAdded(state, world, pos, net.minecraft.init.Blocks.AIR.getDefaultState(), false);
    }

    @Srg("func_196243_a")
    public void onReplaced(IBlockState state, net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, IBlockState newState, boolean isMoving) {
        super.breakBlock(world, pos, state);
    }

    @Override
    public void breakBlock(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, IBlockState state) {
        onReplaced(state, world, pos, world.getBlockState(pos), false);
    }

    @Srg("func_225533_a_")
    public net.minecraft.util.EnumActionResult onBlockActivated(IBlockState state, net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos,
                                                                net.minecraft.entity.player.EntityPlayer player, net.minecraft.util.EnumHand hand,
                                                                com.michaelsebero.portium.modern.mc.util.math.BlockRayTraceResult hit) {
        return net.minecraft.util.EnumActionResult.PASS;
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, IBlockState state, net.minecraft.entity.player.EntityPlayer player,
                                    net.minecraft.util.EnumHand hand, net.minecraft.util.EnumFacing side, float hitX, float hitY, float hitZ) {
        com.michaelsebero.portium.modern.mc.util.math.BlockRayTraceResult hit = new com.michaelsebero.portium.modern.mc.util.math.BlockRayTraceResult(
                new net.minecraft.util.math.Vec3d(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ), side, pos, false);
        return onBlockActivated(state, world, pos, player, hand, hit) == net.minecraft.util.EnumActionResult.SUCCESS;
    }

    // Forge 1.16's rail hooks take the state first; 1.12's minecarts call 1.12's, which hand on to them.

    @Override
    public void onMinecartPass(net.minecraft.world.World world, net.minecraft.entity.item.EntityMinecart cart, net.minecraft.util.math.BlockPos pos) {
        onMinecartPass(world.getBlockState(pos), world, pos, cart);
    }

    /** Forge 1.16's {@code onMinecartPass(state, world, pos, cart)}: a minecart on the rail, each tick. */
    public void onMinecartPass(IBlockState state, net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, net.minecraft.entity.item.EntityMinecart cart) {}

    @Override
    public boolean canMakeSlopes(net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        return canMakeSlopes(world.getBlockState(pos), world, pos);
    }

    /** Forge 1.16's {@code canMakeSlopes(state, world, pos)}. */
    public boolean canMakeSlopes(IBlockState state, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        return true;
    }

    @Override
    public boolean isFlexibleRail(net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        return isFlexibleRail(world.getBlockState(pos), world, pos);
    }

    /** Forge 1.16's {@code isFlexibleRail(state, world, pos)}: whether it can curve. */
    public boolean isFlexibleRail(IBlockState state, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        return !this.isPowered;
    }

    @Override
    public BlockRailBase.EnumRailDirection getRailDirection(net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos, IBlockState state,
                                                           @javax.annotation.Nullable net.minecraft.entity.item.EntityMinecart cart) {
        return getRailDirection(state, world, pos, cart);
    }

    /** Forge 1.16's {@code getRailDirection(state, world, pos, cart)}: 1.12's way unless the rail has its own. */
    public BlockRailBase.EnumRailDirection getRailDirection(IBlockState state, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos,
                                                           @javax.annotation.Nullable net.minecraft.entity.item.EntityMinecart cart) {
        return super.getRailDirection(world, pos, state, cart);
    }

    @Override
    public float getRailMaxSpeed(net.minecraft.world.World world, net.minecraft.entity.item.EntityMinecart cart, net.minecraft.util.math.BlockPos pos) {
        return getRailMaxSpeed(world.getBlockState(pos), world, pos, cart);
    }

    /** Forge 1.16's {@code getRailMaxSpeed(state, world, pos, cart)}. */
    public float getRailMaxSpeed(IBlockState state, net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, net.minecraft.entity.item.EntityMinecart cart) {
        return 0.4F;
    }

    /** Forge 1.13's tile entity factory, taking a block reader, which newer blocks override. */
    @javax.annotation.Nullable
    public net.minecraft.tileentity.TileEntity createTileEntity(IBlockState state, net.minecraft.world.IBlockAccess world) {
        return null;
    }

    @javax.annotation.Nullable
    @Override
    public net.minecraft.tileentity.TileEntity createTileEntity(net.minecraft.world.World world, IBlockState state) {
        return createTileEntity(state, (net.minecraft.world.IBlockAccess) world);
    }
}
