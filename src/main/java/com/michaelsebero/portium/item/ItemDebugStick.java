package com.michaelsebero.portium.item;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The debug stick (1.13). For operators in creative mode: left-click a block to choose one of its properties,
 * right-click to cycle that property's value. Sneak to go backwards.
 */
public class ItemDebugStick extends Item {

    private static final String TAG = "DebugProperty";

    public ItemDebugStick() {
        this.setMaxStackSize(1);
    }

    @Nonnull
    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(net.minecraft.client.resources.I18n.format("item.portium.debug_stick.tooltip"));
    }

    private static boolean canUse(EntityPlayer player) {
        return player.capabilities.isCreativeMode && player.canUseCommand(2, "");
    }

    /** Left click selects a property instead of breaking the block. */
    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, EntityPlayer player) {
        if (!player.world.isRemote && canUse(player)) this.handleClick(player, player.world, pos, false, stack);
        return true;
    }

    @Nonnull
    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote && canUse(player)) this.handleClick(player, world, pos, true, player.getHeldItem(hand));
        return EnumActionResult.SUCCESS;
    }

    private void handleClick(EntityPlayer player, World world, BlockPos pos, boolean cycleValue, ItemStack stack) {
        IBlockState state = world.getBlockState(pos);
        Collection<IProperty<?>> properties = state.getPropertyKeys();
        String blockId = String.valueOf(state.getBlock().getRegistryName());
        if (properties.isEmpty()) {
            player.sendStatusMessage(new TextComponentTranslation("item.portium.debug_stick.empty", blockId), true);
            return;
        }
        NBTTagCompound selections = stack.getOrCreateSubCompound(TAG);
        IProperty<?> property = findProperty(properties, selections.getString(blockId));
        if (cycleValue) {
            if (property == null) property = properties.iterator().next();
            IBlockState cycled = cycle(state, property, player.isSneaking());
            world.setBlockState(pos, cycled, 2);
            player.sendStatusMessage(new TextComponentTranslation("item.portium.debug_stick.update", property.getName(), valueName(cycled, property)), true);
        } else {
            property = adjacent(new ArrayList<>(properties), property, player.isSneaking());
            selections.setString(blockId, property.getName());
            player.sendStatusMessage(new TextComponentTranslation("item.portium.debug_stick.select", property.getName(), valueName(state, property)), true);
        }
    }

    @Nullable
    private static IProperty<?> findProperty(Collection<IProperty<?>> properties, String name) {
        for (IProperty<?> property : properties) {
            if (property.getName().equals(name)) return property;
        }
        return null;
    }

    private static <T> T adjacent(List<T> values, @Nullable T current, boolean backwards) {
        int index = current == null ? -1 : values.indexOf(current);
        int size = values.size();
        int next = backwards ? (index <= 0 ? size - 1 : index - 1) : (index + 1) % size;
        return values.get(next);
    }

    private static <T extends Comparable<T>> IBlockState cycle(IBlockState state, IProperty<T> property, boolean backwards) {
        List<T> values = new ArrayList<>(property.getAllowedValues());
        return state.withProperty(property, adjacent(values, state.getValue(property), backwards));
    }

    private static <T extends Comparable<T>> String valueName(IBlockState state, IProperty<T> property) {
        return property.getName(state.getValue(property));
    }
}
