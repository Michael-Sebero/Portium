package com.michaelsebero.portium.block.light;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.api.IWaterloggableBlock;
import com.michaelsebero.portium.block.water.WaterUtil;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.init.PortiumStats;
import com.michaelsebero.portium.proxy.ParticleKind;
import com.michaelsebero.portium.recipe.CookingRecipes;
import com.michaelsebero.portium.tile.TileEntityCampfire;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
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
 * The campfire (1.14) and soul campfire (1.16). Lit, it gives light, smokes (a tall column on hay), burns what
 * stands in it and cooks food put on it (see {@link TileEntityCampfire}). Water, a shovel or a splash of water puts it
 * out; flint and steel, a fire charge or a burning arrow lights it again. Drops charcoal (soul soil) unless silk
 * touched.
 */
public class BlockCampfire extends Block implements IWaterloggableBlock {

    public static final PropertyBool LIT = PropertyBool.create("lit");
    public static final PropertyBool SIGNAL_FIRE = PropertyBool.create("signal_fire");
    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    private static final AxisAlignedBB SHAPE = BlockLantern.box(0, 0, 0, 16, 7, 16);

    /** Whether it gives off lava sparks (the soul campfire doesn't). */
    private final boolean smokey;
    private final int fireDamage;
    private final int light;

    public BlockCampfire(boolean smokey, int fireDamage, int light) {
        super(Material.WOOD, MapColor.OBSIDIAN);
        this.smokey = smokey;
        this.fireDamage = fireDamage;
        this.light = light;
        this.setHardness(2.0F);
        this.setSoundType(SoundType.WOOD);
        this.setHarvestLevel("axe", 0);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
        this.setDefaultState(this.blockState.getBaseState().withProperty(LIT, true).withProperty(SIGNAL_FIRE, false).withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, LIT, SIGNAL_FIRE, FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(LIT, (meta & 4) != 0)
                .withProperty(SIGNAL_FIRE, (meta & 8) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(LIT) ? 4 : 0) | (state.getValue(SIGNAL_FIRE) ? 8 : 0);
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    // ---- Tile entity ----

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityCampfire();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityCampfire) TileEntityCampfire.dropItems(world, pos, ((TileEntityCampfire) tile).getInventory());
        super.breakBlock(world, pos, state);
    }

    // ---- Placement ----

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        boolean water = WaterUtil.isPlainWaterSource(world.getBlockState(pos));
        return this.getDefaultState().withProperty(SIGNAL_FIRE, isHayBlock(world.getBlockState(pos.down()))).withProperty(LIT, !water)
                .withProperty(FACING, placer.getHorizontalFacing());
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (fromPos.equals(pos.down())) {
            boolean signal = isHayBlock(world.getBlockState(fromPos));
            if (signal != state.getValue(SIGNAL_FIRE)) world.setBlockState(pos, state.withProperty(SIGNAL_FIRE, signal), 3);
        }
    }

    private static boolean isHayBlock(IBlockState state) {
        return state.getBlock() == Blocks.HAY_BLOCK;
    }

    // ---- Use ----

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX,
                                    float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityCampfire)) return false;
        TileEntityCampfire campfire = (TileEntityCampfire) tile;
        ItemStack stack = player.getHeldItem(hand);
        CookingRecipes.Recipe recipe = campfire.findMatchingRecipe(stack);
        if (recipe == null) return false;
        if (!world.isRemote && campfire.addItem(player.capabilities.isCreativeMode ? stack.copy() : stack, recipe.cookingTime)) {
            player.addStat(PortiumStats.INTERACT_WITH_CAMPFIRE);
        }
        return true;
    }

    /** 1.16's {@code CampfireBlock.canBeLit}: an unlit campfire out of water. */
    public static boolean canBeLit(World world, BlockPos pos, IBlockState state) {
        return state.getBlock() instanceof BlockCampfire && !state.getValue(LIT) && !Waterlogging.isWaterlogged(world, pos);
    }

    public static boolean isLit(IBlockState state) {
        return state.getBlock() instanceof BlockCampfire && state.getValue(LIT);
    }

    /** Puts a campfire out: smoke on clients, and whatever was cooking drops. The caller sets the unlit state. */
    public static void extinguish(World world, BlockPos pos, IBlockState state) {
        if (world.isRemote) {
            for (int i = 0; i < 20; i++) spawnSmokeParticles(world, pos, state.getValue(SIGNAL_FIRE), true);
        }
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityCampfire) ((TileEntityCampfire) tile).dropAllItems();
    }

    /** Water poured into it (a bucket, or placing it in water): it goes out. */
    @Override
    public void onWaterlogged(World world, BlockPos pos, IBlockState state) {
        if (!state.getValue(LIT)) return;
        if (!world.isRemote) world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        extinguish(world, pos, state);
        world.setBlockState(pos, state.withProperty(LIT, false), 3);
    }

    // ---- Fire ----

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (entity instanceof EntityArrow) {
            this.onArrowHit(world, pos, state, (EntityArrow) entity);
            return;
        }
        if (!entity.isImmuneToFire() && state.getValue(LIT) && entity instanceof EntityLivingBase
                && EnchantmentHelper.getMaxEnchantmentLevel(Enchantments.FROST_WALKER, (EntityLivingBase) entity) <= 0) {
            entity.attackEntityFrom(DamageSource.IN_FIRE, (float) this.fireDamage);
        }
    }

    /** A burning arrow lights it, if a player shot it or mobs may grief. */
    private void onArrowHit(World world, BlockPos pos, IBlockState state, EntityArrow arrow) {
        if (world.isRemote || !arrow.isBurning()) return;
        Entity shooter = arrow.shootingEntity;
        boolean allowed = shooter == null || shooter instanceof EntityPlayer || world.getGameRules().getBoolean("mobGriefing");
        if (allowed && !state.getValue(LIT) && !Waterlogging.isWaterlogged(world, pos)) world.setBlockState(pos, state.withProperty(LIT, true), 11);
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.getLightValue(state);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getLightValue(IBlockState state) {
        return state.getBlock() == this && state.getValue(LIT) ? this.light : 0;
    }

    @Nullable
    @Override
    public PathNodeType getAiPathNodeType(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(LIT) ? PathNodeType.DAMAGE_FIRE : null;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (!state.getValue(LIT)) return;
        if (rand.nextInt(10) == 0) {
            world.playSound((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, PortiumSounds.BLOCK_CAMPFIRE_CRACKLE,
                    SoundCategory.BLOCKS, 0.5F + rand.nextFloat(), rand.nextFloat() * 0.7F + 0.6F, false);
        }
        if (this.smokey && rand.nextInt(5) == 0) {
            for (int i = 0; i < rand.nextInt(1) + 1; i++) {
                world.spawnParticle(EnumParticleTypes.LAVA, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D,
                        (double) (rand.nextFloat() / 2.0F), 5.0E-5D, (double) (rand.nextFloat() / 2.0F));
            }
        }
    }

    /** A puff of campfire smoke rising from it, and with {@code extinguished} the smoke of it going out. */
    public static void spawnSmokeParticles(World world, BlockPos pos, boolean signal, boolean extinguished) {
        Random random = world.rand;
        Portium.proxy.spawnParticle(signal ? ParticleKind.CAMPFIRE_SIGNAL_SMOKE : ParticleKind.CAMPFIRE_COSY_SMOKE, world,
                (double) pos.getX() + 0.5D + random.nextDouble() / 3.0D * (double) (random.nextBoolean() ? 1 : -1),
                (double) pos.getY() + random.nextDouble() + random.nextDouble(),
                (double) pos.getZ() + 0.5D + random.nextDouble() / 3.0D * (double) (random.nextBoolean() ? 1 : -1), 0.0D, 0.07D, 0.0D);
        if (extinguished) {
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, (double) pos.getX() + 0.25D + random.nextDouble() / 2.0D * (double) (random.nextBoolean() ? 1 : -1),
                    (double) pos.getY() + 0.4D, (double) pos.getZ() + 0.25D + random.nextDouble() / 2.0D * (double) (random.nextBoolean() ? 1 : -1),
                    0.0D, 0.005D, 0.0D);
        }
    }

    // ---- Drops ----

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return this.smokey ? Items.COAL : Item.getItemFromBlock(soulSoil());
    }

    @Override
    public int damageDropped(IBlockState state) {
        // Charcoal.
        return this.smokey ? 1 : 0;
    }

    @Override
    public int quantityDropped(Random random) {
        return this.smokey ? 2 : 1;
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(this);
    }

    /** The soul campfire drops soul soil, or soul sand while soul soil is switched off. */
    private static Block soulSoil() {
        Block soil = com.michaelsebero.portium.init.PortiumBlocks.SOUL_SOIL;
        return soil != null ? soil : Blocks.SOUL_SAND;
    }

    // ---- Shape ----

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SHAPE;
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
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
