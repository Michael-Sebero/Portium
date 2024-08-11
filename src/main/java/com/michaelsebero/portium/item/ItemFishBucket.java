package com.michaelsebero.portium.item;

import com.michaelsebero.portium.block.water.IWaterloggable;
import com.michaelsebero.portium.entity.fish.EntityAbstractFish;
import com.michaelsebero.portium.entity.fish.EntityTropicalFish;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

/**
 * A bucket of water with a fish in it (1.13). Emptying it places the water and releases the fish, which then never
 * despawns. Dispensers empty fish buckets too.
 */
public class ItemFishBucket extends Item {

    private final Function<World, ? extends EntityAbstractFish> fish;

    public ItemFishBucket(Function<World, ? extends EntityAbstractFish> fish) {
        this.fish = fish;
        this.setMaxStackSize(1);
        this.setContainerItem(Items.BUCKET);
        this.setCreativeTab(CreativeTabs.MISC);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(this, new DispenseBehavior());
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        RayTraceResult hit = this.rayTrace(world, player, false);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) return new ActionResult<>(EnumActionResult.PASS, stack);
        BlockPos clicked = hit.getBlockPos();
        if (!world.isBlockModifiable(player, clicked)) return new ActionResult<>(EnumActionResult.FAIL, stack);
        IBlockState state = world.getBlockState(clicked);
        // Like 1.13's buckets: a block that can hold water is filled itself, falling back to the space in front of it.
        boolean container = state.getBlock() instanceof IWaterloggable || Waterlogging.canWaterlog(state);
        BlockPos target = container || state.getBlock().isReplaceable(world, clicked) && hit.sideHit == EnumFacing.UP ? clicked : clicked.offset(hit.sideHit);
        boolean emptied = player.canPlayerEdit(target, hit.sideHit, stack) && this.empty(player, world, target, stack);
        if (!emptied && container) {
            target = clicked.offset(hit.sideHit);
            emptied = player.canPlayerEdit(target, hit.sideHit, stack) && this.empty(player, world, target, stack);
        }
        if (!emptied) return new ActionResult<>(EnumActionResult.FAIL, stack);
        if (player instanceof EntityPlayerMP) CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, target, stack);
        player.addStat(StatList.getObjectUseStats(this));
        return new ActionResult<>(EnumActionResult.SUCCESS, player.capabilities.isCreativeMode ? stack : new ItemStack(Items.BUCKET));
    }

    /** Pours out the water (waterlogging a waterloggable block) and releases the fish. */
    private boolean empty(@Nullable EntityPlayer player, World world, BlockPos pos, ItemStack stack) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof IWaterloggable && !world.provider.doesWaterVaporize()) {
            IWaterloggable block = (IWaterloggable) state.getBlock();
            IBlockState wet = block.isWaterlogged(state) ? null : block.withWaterlogged(state, true);
            if (wet != null) {
                world.setBlockState(pos, wet, 11);
                this.releaseFish(player, world, pos, stack);
                return true;
            }
        }
        if (!world.provider.doesWaterVaporize() && Waterlogging.canWaterlog(state) && Waterlogging.setWaterlogged(world, pos, true)) {
            this.releaseFish(player, world, pos, stack);
            return true;
        }
        Material material = state.getMaterial();
        boolean replaceable = state.getBlock().isReplaceable(world, pos);
        if (!world.isAirBlock(pos) && material.isSolid() && !replaceable) return false;
        if (world.provider.doesWaterVaporize()) {
            world.playSound(player, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
            for (int i = 0; i < 8; ++i) {
                world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, pos.getX() + Math.random(), pos.getY() + Math.random(), pos.getZ() + Math.random(), 0.0D, 0.0D, 0.0D);
            }
            if (!world.isRemote) this.spawnFish(world, pos, stack);
            return true;
        }
        if (!world.isRemote && (!material.isSolid() || replaceable) && !material.isLiquid()) world.destroyBlock(pos, true);
        world.setBlockState(pos, Blocks.FLOWING_WATER.getDefaultState(), 11);
        this.releaseFish(player, world, pos, stack);
        return true;
    }

    private void releaseFish(@Nullable EntityPlayer player, World world, BlockPos pos, ItemStack stack) {
        if (!world.isRemote) this.spawnFish(world, pos, stack);
        world.playSound(player, pos, PortiumSounds.ITEM_BUCKET_EMPTY_FISH, SoundCategory.NEUTRAL, 1.0F, 1.0F);
    }

    private void spawnFish(World world, BlockPos pos, ItemStack stack) {
        EntityAbstractFish fish = this.fish.apply(world);
        fish.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, MathHelper.wrapDegrees(world.rand.nextFloat() * 360.0F), 0.0F);
        fish.rotationYawHead = fish.rotationYaw;
        fish.renderYawOffset = fish.rotationYaw;
        fish.onInitialSpawn(world.getDifficultyForLocation(pos), null);
        NBTTagCompound tag = stack.getTagCompound();
        if (fish instanceof EntityTropicalFish && tag != null && tag.hasKey(EntityTropicalFish.BUCKET_VARIANT_TAG, 3)) {
            ((EntityTropicalFish) fish).setVariant(tag.getInteger(EntityTropicalFish.BUCKET_VARIANT_TAG));
        }
        if (stack.hasDisplayName()) fish.setCustomNameTag(stack.getDisplayName());
        fish.setFromBucket(true);
        world.spawnEntity(fish);
    }

    /** Tropical fish buckets name the species, or the pattern and colours of an unnamed variety (1.13). */
    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(EntityTropicalFish.BUCKET_VARIANT_TAG, 3)) return;
        String style = TextFormatting.GRAY.toString() + TextFormatting.ITALIC;
        int variant = tag.getInteger(EntityTropicalFish.BUCKET_VARIANT_TAG);
        int species = EntityTropicalFish.predefinedIndex(variant);
        if (species >= 0) {
            tooltip.add(style + I18n.format("entity.portium.tropical_fish.predefined." + species));
            return;
        }
        tooltip.add(style + I18n.format(EntityTropicalFish.pattern(variant).translationKey()));
        String body = I18n.format("item.fireworksCharge." + EntityTropicalFish.bodyColor(variant).getTranslationKey());
        String pattern = I18n.format("item.fireworksCharge." + EntityTropicalFish.patternColor(variant).getTranslationKey());
        tooltip.add(style + (body.equals(pattern) ? body : body + ", " + pattern));
    }

    /** Dispensers pour the water out in front of them and release the fish. */
    private static class DispenseBehavior extends BehaviorDefaultDispenseItem {

        private final BehaviorDefaultDispenseItem fallback = new BehaviorDefaultDispenseItem();

        @Override
        protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
            ItemFishBucket bucket = (ItemFishBucket) stack.getItem();
            BlockPos pos = source.getBlockPos().offset(source.getBlockState().getValue(BlockDispenser.FACING));
            if (bucket.empty(null, source.getWorld(), pos, stack)) return new ItemStack(Items.BUCKET);
            return this.fallback.dispense(source, stack);
        }
    }
}
