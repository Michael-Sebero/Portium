package com.michaelsebero.portium.block.bee;

import com.michaelsebero.portium.api.IManyStateBlock;
import com.michaelsebero.portium.block.light.BlockCampfire;
import com.michaelsebero.portium.entity.passive.bee.EntityBee;
import com.michaelsebero.portium.init.PortiumItems;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.tile.TileEntityBeehive;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 1.15's bee nest and beehive. Bees fill the honey level (0-5); at 5, shears take three honeycombs and a glass bottle
 * takes a honey bottle, which angers the bees inside and around unless a lit campfire smokes it from below. Broken
 * without silk touch it lets the bees out angry (a nest then drops nothing); with silk touch it keeps them. Facing and
 * honey level make 24 states ({@link IManyStateBlock}).
 */
public class BlockBeehive extends BlockContainer implements IManyStateBlock {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyInteger HONEY_LEVEL = PropertyInteger.create("honey_level", 0, 5);
    private static final EnumFacing[] GENERATE_DIRECTIONS = {EnumFacing.WEST, EnumFacing.EAST, EnumFacing.SOUTH};

    private final boolean nest;

    public BlockBeehive(boolean nest) {
        super(Material.WOOD, nest ? MapColor.YELLOW : MapColor.WOOD);
        this.nest = nest;
        this.setHardness(nest ? 0.3F : 0.6F);
        this.setSoundType(SoundType.WOOD);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
        this.setDefaultState(this.blockState.getBaseState().withProperty(HONEY_LEVEL, 0).withProperty(FACING, EnumFacing.NORTH));
    }

    public boolean isNest() {
        return this.nest;
    }

    public static EnumFacing getGenerationDirection(java.util.Random random) {
        return GENERATE_DIRECTIONS[random.nextInt(GENERATE_DIRECTIONS.length)];
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, HONEY_LEVEL, FACING);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return IManyStateBlock.metaOf(state);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return IManyStateBlock.stateOf(this, meta);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer) {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    /** 1.15's fire info: nests 30/20, hives 5/20. */
    @Override
    public int getFlammability(net.minecraft.world.IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 20;
    }

    @Override
    public int getFireSpreadSpeed(net.minecraft.world.IBlockAccess world, BlockPos pos, EnumFacing face) {
        return this.nest ? 30 : 5;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityBeehive();
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        return state.getValue(HONEY_LEVEL);
    }

    // ---- Harvesting honey ----

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side,
                                    float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        if (state.getValue(HONEY_LEVEL) < 5) return false;
        boolean harvested = false;
        if (stack.getItem() == Items.SHEARS) {
            world.playSound(player, player.posX, player.posY, player.posZ, PortiumSounds.BLOCK_BEEHIVE_SHEAR, SoundCategory.NEUTRAL, 1.0F, 1.0F);
            if (!world.isRemote) {
                if (PortiumItems.HONEYCOMB != null) spawnAsEntity(world, pos, new ItemStack(PortiumItems.HONEYCOMB, 3));
                stack.damageItem(1, player);
            }
            harvested = true;
        } else if (stack.getItem() == Items.GLASS_BOTTLE && PortiumItems.HONEY_BOTTLE != null) {
            world.playSound(player, player.posX, player.posY, player.posZ, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.NEUTRAL, 1.0F, 1.0F);
            if (!world.isRemote) {
                if (!player.capabilities.isCreativeMode) stack.shrink(1);
                ItemStack honey = new ItemStack(PortiumItems.HONEY_BOTTLE);
                if (stack.isEmpty()) {
                    player.setHeldItem(hand, honey);
                } else if (!player.inventory.addItemStackToInventory(honey)) {
                    player.dropItem(honey, false);
                }
            }
            harvested = true;
        }
        if (!harvested) return false;
        if (!world.isRemote) {
            if (!isSmokingBlockAt(world, pos)) {
                if (this.hasBees(world, pos)) angerNearbyBees(world, pos);
                this.takeHoney(world, state, pos, player, TileEntityBeehive.State.EMERGENCY);
            } else {
                world.setBlockState(pos, state.withProperty(HONEY_LEVEL, 0), 3);
            }
        }
        return true;
    }

    private boolean hasBees(World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof TileEntityBeehive && !((TileEntityBeehive) tile).hasNoBees();
    }

    public void takeHoney(World world, IBlockState state, BlockPos pos, @Nullable EntityPlayer player, TileEntityBeehive.State reason) {
        world.setBlockState(pos, state.withProperty(HONEY_LEVEL, 0), 3);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityBeehive) ((TileEntityBeehive) tile).angerBees(player, state, reason);
    }

    /** Bees about the hive with no target go for a random player near it. */
    private static void angerNearbyBees(World world, BlockPos pos) {
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(8.0D, 6.0D, 8.0D);
        List<EntityBee> bees = world.getEntitiesWithinAABB(EntityBee.class, area);
        if (bees.isEmpty()) return;
        List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, area);
        if (players.isEmpty()) return;
        for (EntityBee bee : bees) {
            if (bee.getAttackTarget() == null) bee.setAttackTarget(players.get(world.rand.nextInt(players.size())));
        }
    }

    /**
     * 1.15's {@code CampfireBlock.isSmokingBlockAt}: a lit campfire up to five blocks below, smoke rising through
     * anything that doesn't block a fence post's footprint.
     */
    public static boolean isSmokingBlockAt(World world, BlockPos pos) {
        AxisAlignedBB post = new AxisAlignedBB(6.0D / 16.0D, 0.0D, 6.0D / 16.0D, 10.0D / 16.0D, 1.0D, 10.0D / 16.0D);
        for (int i = 1; i <= 5; ++i) {
            BlockPos below = pos.down(i);
            IBlockState state = world.getBlockState(below);
            if (BlockCampfire.isLit(state)) return true;
            AxisAlignedBB box = state.getCollisionBoundingBox(world, below);
            if (box != null && box.intersects(post)) return BlockCampfire.isLit(world.getBlockState(below.down()));
        }
        return false;
    }

    // ---- Breaking ----

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity tile, ItemStack tool) {
        player.addStat(StatList.getBlockStats(this));
        player.addExhaustion(0.005F);
        boolean silk = EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0;
        if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            int bees = tile instanceof TileEntityBeehive ? ((TileEntityBeehive) tile).getBees().tagCount() : 0;
            com.michaelsebero.portium.advancements.ModernTriggers116.BEE_NEST_DESTROYED.trigger((net.minecraft.entity.player.EntityPlayerMP) player, tool, null,
                    null, this, bees, null);
        }
        if (silk) {
            spawnAsEntity(world, pos, withContents(new ItemStack(this), state, tile, true));
        } else if (!this.nest) {
            spawnAsEntity(world, pos, new ItemStack(this));
        }
        if (!world.isRemote && tile instanceof TileEntityBeehive && !silk) {
            ((TileEntityBeehive) tile).angerBees(player, state, TileEntityBeehive.State.EMERGENCY);
            world.updateComparatorOutputLevel(pos, this);
            angerNearbyBees(world, pos);
        }
    }

    /** The item with the bees and honey level, as 1.15 copies them ({@code BlockEntityTag} and {@code BlockStateTag}). */
    private static ItemStack withContents(ItemStack stack, IBlockState state, @Nullable TileEntity tile, boolean always) {
        boolean bees = tile instanceof TileEntityBeehive && !((TileEntityBeehive) tile).hasNoBees();
        if (bees) {
            NBTTagCompound entityTag = new NBTTagCompound();
            entityTag.setTag("Bees", ((TileEntityBeehive) tile).getBees());
            stack.setTagInfo("BlockEntityTag", entityTag);
        }
        if (bees || always || state.getValue(HONEY_LEVEL) > 0) {
            NBTTagCompound stateTag = new NBTTagCompound();
            stateTag.setString("honey_level", String.valueOf(state.getValue(HONEY_LEVEL)));
            stack.setTagInfo("BlockStateTag", stateTag);
        }
        return stack;
    }

    /** In creative, a hive with bees or honey drops itself with them (when blocks drop). */
    @Override
    public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        if (!world.isRemote && player.capabilities.isCreativeMode && world.getGameRules().getBoolean("doTileDrops")) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityBeehive && (!((TileEntityBeehive) tile).hasNoBees() || state.getValue(HONEY_LEVEL) > 0)) {
                EntityItem item = new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), withContents(new ItemStack(this), state, tile, false));
                item.setDefaultPickupDelay();
                world.spawnEntity(item);
            }
        }
        super.onBlockHarvested(world, pos, state, player);
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof TileEntityBeehive) {
            ((TileEntityBeehive) tile).angerBees(null, world.getBlockState(pos), TileEntityBeehive.State.EMERGENCY);
        }
        super.onBlockExploded(world, pos, explosion);
    }

    @Override
    public void getDrops(net.minecraft.util.NonNullList<ItemStack> drops, net.minecraft.world.IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        if (!this.nest) drops.add(new ItemStack(this));
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (world.getBlockState(fromPos).getBlock() instanceof BlockFire) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityBeehive) ((TileEntityBeehive) tile).angerBees(null, state, TileEntityBeehive.State.EMERGENCY);
        }
    }

    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, java.util.Random random) {
        if (state.getValue(HONEY_LEVEL) < 5 || random.nextFloat() < 0.3F) return;
        IBlockState below = world.getBlockState(pos.down());
        if (below.isFullCube() || below.getMaterial().isLiquid()) return;
        com.michaelsebero.portium.Portium.proxy.spawnParticle(com.michaelsebero.portium.proxy.ParticleKind.DRIPPING_HONEY, world,
                pos.getX() + random.nextDouble(), pos.getY() - 0.05D, pos.getZ() + random.nextDouble(), 0.0D, 0.0D, 0.0D);
    }
}
