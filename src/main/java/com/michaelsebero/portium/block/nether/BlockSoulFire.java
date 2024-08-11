package com.michaelsebero.portium.block.nether;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Soul fire (1.16): the blue fire that burns on soul sand and soul soil. It never spreads or goes out, hurts twice as
 * much as fire, and gives light 10. It goes out once its soul block is gone.
 */
public class BlockSoulFire extends Block {

    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.0625D, 1.0D);

    public BlockSoulFire() {
        super(Material.FIRE, MapColor.LIGHT_BLUE);
        this.setLightLevel(10.0F / 15.0F);
        this.disableStats();
    }

    /** Soul sand and soul soil, which turn fire lit on them into soul fire (1.16's soul fire base blocks). */
    public static boolean isSoulFireBase(Block block) {
        return block == Blocks.SOUL_SAND || block == com.michaelsebero.portium.init.PortiumBlocks.SOUL_SOIL;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SHAPE;
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return Items.AIR;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return isSoulFireBase(world.getBlockState(pos.down()).getBlock());
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (!this.canPlaceBlockAt(world, pos)) world.setBlockToAir(pos);
    }

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (entity.isImmuneToFire()) return;
        // As 1.16 fire: burning keeps going while inside, and the fire itself hurts.
        if (!entity.isBurning()) entity.setFire(8);
        entity.attackEntityFrom(DamageSource.IN_FIRE, 2.0F);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
        if (random.nextInt(24) == 0) {
            world.playSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS,
                    1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F, false);
        }
        for (int i = 0; i < 3; i++) {
            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble() * 0.5D + 0.5D,
                    pos.getZ() + random.nextDouble(), 0.0D, 0.0D, 0.0D);
        }
    }
}
