package com.michaelsebero.portium.block.plant;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * The wither rose (1.14): grows on the Nether's ground too, withers what walks into it (not on Peaceful), and smokes.
 * Mobs the Wither kills leave one where they die (see {@code handler.WitherRoses}).
 */
public class BlockWitherRose extends BlockPortiumFlower {

    public BlockWitherRose() {
        super(MobEffects.WITHER, 8);
    }

    @Override
    protected boolean canSustainBush(IBlockState state) {
        Block block = state.getBlock();
        return super.canSustainBush(state) || block == Blocks.NETHERRACK || block == Blocks.SOUL_SAND
                || block == com.michaelsebero.portium.init.PortiumBlocks.SOUL_SOIL;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        IBlockState below = world.getBlockState(pos.down());
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos) && (this.canSustainBush(below)
                || below.getBlock().canSustainPlant(below, world, pos.down(), net.minecraft.util.EnumFacing.UP, this));
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        IBlockState below = world.getBlockState(pos.down());
        return this.canSustainBush(below) || super.canBlockStay(world, pos, state);
    }

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (world.isRemote || world.getDifficulty() == EnumDifficulty.PEACEFUL || !(entity instanceof EntityLivingBase)) return;
        EntityLivingBase living = (EntityLivingBase) entity;
        if (!living.isEntityInvulnerable(DamageSource.WITHER)) living.addPotionEffect(new PotionEffect(MobEffects.WITHER, 40));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        Vec3d center = this.getBoundingBox(state, world, pos).getCenter();
        double x = (double) pos.getX() + center.x;
        double z = (double) pos.getZ() + center.z;
        for (int i = 0; i < 3; i++) {
            if (rand.nextBoolean()) {
                world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x + rand.nextDouble() / 5.0D, (double) pos.getY() + (0.5D - rand.nextDouble()),
                        z + rand.nextDouble() / 5.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }
}
