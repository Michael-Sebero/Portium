package com.michaelsebero.portium.modern.adapter;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.mc.item.BlockItemUseContext;
import com.michaelsebero.portium.modern.mc.item.Item$Properties;
import com.michaelsebero.portium.modern.mc.item.ItemUseContext;
import com.michaelsebero.portium.modern.shim.BlockShims;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * What a 1.13 mod's {@code ItemBlock} (and {@code ItemBed}) subclasses extend, and what {@code new ItemBlock(...)} in
 * 1.13 code makes: placement as 1.13 does it, through {@code onItemUse(ItemUseContext)} and {@code tryPlace}, asking
 * a 1.13 block's {@code getStateForPlacement(BlockItemUseContext)} for its state. Its creative tab is its own, as in
 * 1.13, not its block's.
 */
public class ModernItemBlock extends ItemBlock {

    @Nullable
    private final CreativeTabs group;

    public ModernItemBlock(Block block, Item$Properties properties) {
        super(block);
        this.group = properties.getGroup();
        ((IItemPropertyHolder) this).portium$applyItemProperties(properties);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY,
                                      float hitZ) {
        return onItemUse(ModernItems.context(player, world, pos, hand, facing, hitX, hitY, hitZ));
    }

    @Srg("func_195939_a")
    public EnumActionResult onItemUse(ItemUseContext context) {
        return tryPlace(new BlockItemUseContext(context));
    }

    @Srg("func_195942_a")
    public EnumActionResult tryPlace(BlockItemUseContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getPos();
        EntityPlayer player = context.getPlayer();
        ItemStack stack = context.getItem();
        if (stack.isEmpty() || !context.canPlace() || player != null && !player.canPlayerEdit(pos, context.getFace(), stack)) return EnumActionResult.FAIL;
        IBlockState state = getStateForPlacement(context);
        if (state == null || !canPlace(context, state)) return EnumActionResult.FAIL;
        if (!placeBlock(context, state)) return EnumActionResult.FAIL;
        IBlockState placed = world.getBlockState(pos);
        if (placed.getBlock() == state.getBlock()) {
            onBlockPlaced(pos, world, player, stack, placed);
            placed.getBlock().onBlockPlacedBy(world, pos, placed, player, stack);
            if (player instanceof EntityPlayerMP) CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, pos, stack);
        }
        SoundType sound = placed.getBlock().getSoundType(placed, world, pos, player);
        world.playSound(player, pos, sound.getPlaceSound(), SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        stack.shrink(1);
        return EnumActionResult.SUCCESS;
    }

    @Nullable
    @Srg("func_195945_b")
    protected IBlockState getStateForPlacement(BlockItemUseContext context) {
        if (this.block instanceof IModernBlock) return ((IModernBlock) this.block).getStateForPlacement(context);
        EntityPlayer player = context.getPlayer();
        return this.block.getStateForPlacement(context.getWorld(), context.getPos(), context.getFace(), context.getHitX(), context.getHitY(),
                context.getHitZ(), getMetadata(context.getItem().getMetadata()), player, player == null ? EnumHand.MAIN_HAND : player.getActiveHand());
    }

    /**
     * 1.12's client asks this before using the item, of the block's default state; 1.13 has no such check, and
     * {@link #tryPlace} checks the state it places.
     */
    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side, EntityPlayer player, ItemStack stack) {
        return true;
    }

    /** 1.13's check, on the state being placed: it can stand there, and no entity is in its way. */
    @Srg("func_195944_a")
    protected boolean canPlace(BlockItemUseContext context, IBlockState state) {
        World world = context.getWorld();
        BlockPos pos = context.getPos();
        if (!BlockShims.isValidPosition(state, world, pos)) return false;
        AxisAlignedBB box = state.getCollisionBoundingBox(world, pos);
        return box == null || box == Block.NULL_AABB || world.checkNoEntityCollision(box.offset(pos), null);
    }

    /** 1.13's hook after the block is placed: the item's tile entity data by default (a secret sign opens its editor). */
    @Srg("func_195943_a")
    protected boolean onBlockPlaced(BlockPos pos, World world, @Nullable EntityPlayer player, ItemStack stack, IBlockState state) {
        return setTileEntityNBT(world, player, pos, stack);
    }

    @Srg("func_195941_b")
    protected boolean placeBlock(BlockItemUseContext context, IBlockState state) {
        return context.getWorld().setBlockState(context.getPos(), state, 11);
    }

    @Override
    @Nullable
    public CreativeTabs getCreativeTab() {
        return this.group;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return I18n.translateToLocal(getTranslationKey(stack)).trim();
    }
}
