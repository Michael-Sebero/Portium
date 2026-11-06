package com.michaelsebero.portium.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * The target block (1.16): a projectile hitting it gives off redstone power, stronger the nearer the hit is to the
 * middle of the face, for a second (arrows and tridents) or 8 ticks (anything else thrown).
 */
public class BlockTarget extends Block implements IProjectileHitBlock {

    public static final PropertyInteger POWER = PropertyInteger.create("power", 0, 15);

    public BlockTarget() {
        super(Material.GRASS, MapColor.QUARTZ);
        this.setHardness(0.5F);
        this.setSoundType(SoundType.PLANT);
        this.setCreativeTab(CreativeTabs.REDSTONE);
        this.setDefaultState(this.blockState.getBaseState().withProperty(POWER, 0));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, POWER);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(POWER, meta & 15);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(POWER);
    }

    /** A projectile hit it (server): power the block and count the hit for the shooter. */
    @Override
    public void onProjectileHit(World world, IBlockState state, RayTraceResult hit, Entity projectile, Entity shooter) {
        int power = this.powerFromHit(world, state, hit, projectile);
        if (shooter instanceof net.minecraft.entity.player.EntityPlayerMP) {
            com.michaelsebero.portium.advancements.ModernTriggers116.TARGET_HIT.trigger((net.minecraft.entity.player.EntityPlayerMP) shooter, null, projectile,
                    null, null, power, null);
        }
        if (shooter instanceof EntityPlayerMP) ((EntityPlayerMP) shooter).addStat(com.michaelsebero.portium.init.PortiumStats.TARGET_HIT);
    }

    private int powerFromHit(World world, IBlockState state, RayTraceResult hit, Entity projectile) {
        int power = powerFromHitVec(hit.sideHit, hit.hitVec);
        int duration = projectile instanceof EntityArrow ? 20 : 8;
        if (!world.isUpdateScheduled(hit.getBlockPos(), this)) {
            world.setBlockState(hit.getBlockPos(), state.withProperty(POWER, power), 3);
            world.scheduleUpdate(hit.getBlockPos(), this, duration);
        }
        return power;
    }

    private static int powerFromHitVec(EnumFacing face, Vec3d hit) {
        double x = Math.abs(MathHelper.frac(hit.x) - 0.5D);
        double y = Math.abs(MathHelper.frac(hit.y) - 0.5D);
        double z = Math.abs(MathHelper.frac(hit.z) - 0.5D);
        EnumFacing.Axis axis = face.getAxis();
        double distance = axis == EnumFacing.Axis.Y ? Math.max(x, z) : axis == EnumFacing.Axis.Z ? Math.max(x, y) : Math.max(y, z);
        return Math.max(1, MathHelper.ceil(15.0D * MathHelper.clamp((0.5D - distance) / 0.5D, 0.0D, 1.0D)));
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (state.getValue(POWER) != 0) world.setBlockState(pos, state.withProperty(POWER, 0), 3);
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return state.getValue(POWER);
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return true;
    }

    /** Placed already powered (by a command), it switches off unless a tick will. */
    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote && state.getValue(POWER) > 0 && !world.isUpdateScheduled(pos, this)) {
            world.setBlockState(pos, state.withProperty(POWER, 0), 18);
        }
    }
}
