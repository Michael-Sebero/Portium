package com.michaelsebero.portium.block.bee;

import com.michaelsebero.portium.api.ISpeedFactorBlock;
import com.michaelsebero.portium.init.PortiumSoundTypes;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * The honey block (1.15): slows what crosses it and halves jumps off it, takes 80% off falls onto it, and holds mobs
 * that slide down its sides to a slow descent. Pistons pull blocks stuck to it like slime, but honey and slime don't
 * stick to each other, and a honey block moved sideways carries what stands on it.
 */
public class BlockHoney extends Block implements ISpeedFactorBlock {

    /** The status a sliding mob (or minecart, boat, TNT) sends for its particles now and then; landing sends 54. */
    public static final byte SLIDE_STATUS = 53;
    public static final byte LAND_STATUS = 54;
    private static final AxisAlignedBB COLLISION = new AxisAlignedBB(1.0D / 16.0D, 0.0D, 1.0D / 16.0D, 15.0D / 16.0D, 15.0D / 16.0D, 15.0D / 16.0D);

    public BlockHoney() {
        super(Material.CLAY, MapColor.ADOBE);
        this.setSoundType(PortiumSoundTypes.HONEY);
        this.setLightOpacity(1);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
    }

    @Override
    public float getSpeedFactor() {
        return 0.4F;
    }

    @Override
    public float getJumpFactor() {
        return 0.5F;
    }

    @Override
    public boolean isStickyBlock(IBlockState state) {
        return true;
    }

    // ---- Shape and looks ----

    @Nullable
    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return COLLISION;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.SOLID;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.TRANSLUCENT;
    }

    /** Like glass and slime, honey hides the faces it shares with honey. */
    @SideOnly(Side.CLIENT)
    @Override
    @SuppressWarnings("deprecation")
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return world.getBlockState(pos.offset(side)).getBlock() != this && super.shouldSideBeRendered(state, world, pos, side);
    }

    // ---- Falling and sliding ----

    @Override
    public void onFallenUpon(World world, BlockPos pos, Entity entity, float fallDistance) {
        entity.playSound(PortiumSounds.BLOCK_HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
        if (!world.isRemote) world.setEntityState(entity, LAND_STATUS);
        entity.fall(fallDistance, 0.2F);
    }

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (isSliding(pos, entity)) {
            slide(entity);
            if (entity instanceof net.minecraft.entity.player.EntityPlayerMP && world.getTotalWorldTime() % 20L == 0L) {
                com.michaelsebero.portium.advancements.ModernTriggers116.SLIDE_DOWN_BLOCK.trigger((net.minecraft.entity.player.EntityPlayerMP) entity, null,
                        null, null, state.getBlock(), 0.0F, null);
            }
            if (hasSlideEffects(entity)) {
                if (world.rand.nextInt(5) == 0) entity.playSound(PortiumSounds.BLOCK_HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
                if (!world.isRemote && world.rand.nextInt(5) == 0) world.setEntityState(entity, SLIDE_STATUS);
            }
        }
    }

    /** Falling past a side it's pressed against, below the top. */
    private static boolean isSliding(BlockPos pos, Entity entity) {
        if (entity.onGround) return false;
        if (entity.posY > pos.getY() + 0.9375D - 1.0E-7D) return false;
        if (entity.motionY >= -0.08D) return false;
        double dx = Math.abs(pos.getX() + 0.5D - entity.posX);
        double dz = Math.abs(pos.getZ() + 0.5D - entity.posZ);
        double reach = 0.4375D + entity.width / 2.0F;
        return dx + 1.0E-7D > reach || dz + 1.0E-7D > reach;
    }

    private static void slide(Entity entity) {
        if (entity.motionY < -0.13D) {
            double scale = -0.05D / entity.motionY;
            entity.motionX *= scale;
            entity.motionZ *= scale;
        }
        entity.motionY = -0.05D;
        entity.fallDistance = 0.0F;
    }

    private static boolean hasSlideEffects(Entity entity) {
        return entity instanceof EntityLivingBase || entity instanceof EntityMinecart || entity instanceof EntityTNTPrimed || entity instanceof EntityBoat;
    }

    /** Honey flecks around a mob sliding (5) or landing (10), from the statuses above (client). */
    public static void slideParticles(Entity entity, int count) {
        if (!entity.world.isRemote) return;
        IBlockState honey = com.michaelsebero.portium.init.PortiumBlocks.HONEY_BLOCK == null ? null
                : com.michaelsebero.portium.init.PortiumBlocks.HONEY_BLOCK.getDefaultState();
        if (honey == null) return;
        for (int i = 0; i < count; i++) {
            entity.world.spawnParticle(EnumParticleTypes.BLOCK_DUST, entity.posX, entity.posY, entity.posZ, 0.0D, 0.0D, 0.0D, Block.getStateId(honey));
        }
    }
}
