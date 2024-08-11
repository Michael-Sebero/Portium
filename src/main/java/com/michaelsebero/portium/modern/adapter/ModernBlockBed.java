package com.michaelsebero.portium.modern.adapter;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.mc.block.Block$Properties;
import com.michaelsebero.portium.modern.mc.item.BlockItemUseContext;
import com.michaelsebero.portium.modern.mc.util.IItemProvider;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShape;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShapes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBed;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.List;

/**
 * What a 1.13 mod's bed blocks extend instead of 1.12's {@code BlockBed}: 1.12's bed (sleeping, its state
 * properties, its tile entity renderer) as 1.13 has it. 1.13 beds are one block per colour, are placed as ordinary
 * block items (the foot first, then the head in {@code onBlockPlacedBy}), break as a pair through shape updates and
 * drop from loot tables, where 1.12's single bed block leaves all that to its bed item and tile entity. Otherwise as
 * {@link ModernBlock}.
 */
public class ModernBlockBed extends BlockBed implements IModernBlock {

    private static final VoxelShape BED_SHAPE = VoxelShapes.create(0.0D, 0.0D, 0.0D, 1.0D, 0.5625D, 1.0D);

    private final EnumDyeColor color;
    @Nullable
    private Block$Properties properties;

    public ModernBlockBed(EnumDyeColor color, Block$Properties properties) {
        this.color = color;
        this.properties = properties;
        ((IBlockPropertyHolder) this).portium$applyBlockProperties(properties);
    }

    @Srg("func_196350_d")
    public EnumDyeColor getColor() {
        return this.color;
    }

    /** Forge 1.13 takes every bed block for a bed; Forge 1.12 only its own, and wakes players in any other. */
    @Override
    public boolean isBed(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable Entity player) {
        return true;
    }

    // Shapes

    @Override
    @Srg("func_196244_b")
    public VoxelShape getShape(IBlockState state, IBlockAccess world, BlockPos pos) {
        return BED_SHAPE;
    }

    @Override
    @Srg("func_196268_f")
    public VoxelShape getCollisionShape(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.properties == null || this.properties.blocksMovement() ? getShape(state, world, pos) : VoxelShapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return ModernBlocks.bounds(getShape(state, world, pos));
    }

    @Nullable
    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return ModernBlocks.collisionBounds(getCollisionShape(state, world, pos));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> boxes,
                                      @Nullable Entity entity, boolean isActualState) {
        ModernBlocks.addCollisionBoxes(getCollisionShape(state, world, pos), pos, entityBox, boxes);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return ModernBlocks.faceShape(getShape(state, world, pos));
    }

    // Placement and updates

    /** 1.13's bed: the foot, facing away from the placer, if the head has room. */
    @Nullable
    @Override
    @Srg("func_196258_a")
    public IBlockState getStateForPlacement(BlockItemUseContext context) {
        EnumFacing facing = context.getPlacementHorizontalFacing();
        BlockPos head = context.getPos().offset(facing);
        World world = context.getWorld();
        return world.getBlockState(head).getBlock().isReplaceable(world, head) ? getDefaultState().withProperty(FACING, facing) : null;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        EntityPlayer player = placer instanceof EntityPlayer ? (EntityPlayer) placer : null;
        IBlockState state = getStateForPlacement(BlockItemUseContext.at(world, player, hand, placer.getHeldItem(hand), pos, facing, hitX, hitY, hitZ));
        return state == null ? getDefaultState() : state;
    }

    /** 1.13's bed puts its head down once the foot is placed. */
    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (world.isRemote) return;
        BlockPos head = pos.offset(state.getValue(FACING));
        world.setBlockState(head, state.withProperty(PART, EnumPartType.HEAD), 3);
        world.notifyNeighborsRespectDebug(pos, Blocks.AIR, false);
        world.notifyNeighborsRespectDebug(head, Blocks.AIR, false);
    }

    /** 1.13's bed: a half whose other half is gone becomes air. */
    @Override
    @Srg("func_196271_a")
    public IBlockState updatePostPlacement(IBlockState state, EnumFacing facing, IBlockState facingState, World world, BlockPos pos, BlockPos facingPos) {
        EnumPartType part = state.getValue(PART);
        EnumFacing towardsOther = part == EnumPartType.FOOT ? state.getValue(FACING) : state.getValue(FACING).getOpposite();
        if (facing != towardsOther) return state;
        if (facingState.getBlock() == this && facingState.getValue(PART) != part) return state.withProperty(OCCUPIED, facingState.getValue(OCCUPIED));
        return Blocks.AIR.getDefaultState();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, block, fromPos);
        ModernBlocks.applyShapeUpdate(this, world, pos, fromPos);
    }

    @Override
    @Srg("func_196260_a")
    public boolean isValidPosition(IBlockState state, IBlockAccess world, BlockPos pos) {
        return true;
    }

    /** 1.12's placement check has no state to go on; any of the block's states that can stand there will do. */
    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && ModernBlocks.anyValidPosition(this, world, pos);
    }

    @Override
    @Srg("func_196250_a")
    public boolean onBlockActivated(IBlockState state, World world, BlockPos pos, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX,
                                    float hitY, float hitZ) {
        return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX,
                                    float hitY, float hitZ) {
        return onBlockActivated(state, world, pos, player, hand, side, hitX, hitY, hitZ);
    }

    // Items, drops, tile entities and names: 1.13's, not 1.12's bed item and tile entity colour.

    @Override
    @Srg("func_185473_a")
    public ItemStack getItem(IBlockAccess world, BlockPos pos, IBlockState state) {
        return new ItemStack(this);
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return getItem((IBlockAccess) world, pos, state);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        if (world instanceof World) getDrops(state, drops, (World) world, pos, fortune);
    }

    /** A 1.13 bed drops its item from the head; 1.12's dropped it from the foot. */
    @Override
    @Srg("func_199769_a")
    public IItemProvider getItemDropped(IBlockState state, World world, BlockPos pos, int fortune) {
        return state.getValue(PART) == EnumPartType.FOOT ? (IItemProvider) Items.AIR : (IItemProvider) this;
    }

    /** Block's, without 1.12's bed dropping a bed item from its foot. */
    @Override
    public void dropBlockAsItemWithChance(World world, BlockPos pos, IBlockState state, float chance, int fortune) {
        if (world.isRemote || world.restoringBlockSnapshots) return;
        NonNullList<ItemStack> drops = NonNullList.create();
        getDrops(drops, world, pos, state, fortune);
        chance = ForgeEventFactory.fireBlockHarvesting(drops, world, pos, state, fortune, chance, false, this.harvesters.get());
        for (ItemStack drop : drops) {
            if (world.rand.nextFloat() <= chance) spawnAsEntity(world, pos, drop);
        }
    }

    /** Block's, without 1.12's bed dropping a bed item for a bed tile entity. */
    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity tileEntity, ItemStack tool) {
        StatBase mined = StatList.getBlockStats(this);
        if (mined != null) player.addStat(mined);
        player.addExhaustion(0.005F);
        this.harvesters.set(player);
        dropBlockAsItem(world, pos, state, EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, tool));
        this.harvesters.set(null);
    }

    /** 1.13's bed tile entity, which carries the colour for its renderer. */
    @Nullable
    @Override
    @Srg("func_196283_a_")
    public TileEntity createNewTileEntity(IBlockAccess world) {
        TileEntityBed bed = new TileEntityBed();
        bed.setColor(this.color);
        return bed;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return createNewTileEntity((IBlockAccess) world);
    }

    @Override
    public String getTranslationKey() {
        return ModernBlocks.translationKey(this, super.getTranslationKey());
    }

    @Override
    public String getLocalizedName() {
        return ModernBlocks.localize(getTranslationKey());
    }
}
