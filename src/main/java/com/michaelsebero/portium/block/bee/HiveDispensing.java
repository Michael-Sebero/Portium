package com.michaelsebero.portium.block.bee;

import com.michaelsebero.portium.init.PortiumItems;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.tile.TileEntityBeehive;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.15's dispensers at beehives and bee nests: shears take three honeycombs from a full one (sheep are still shorn as
 * before), and glass bottles take a honey bottle; failing that a bottle fills with water from water in front, as 1.16's
 * do. The bees inside come out calm, as for a dispenser in 1.15.
 */
public final class HiveDispensing {

    public static void register() {
        IBehaviorDispenseItem shears = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.SHEARS);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(Items.SHEARS, new Shears(shears));
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(Items.GLASS_BOTTLE, new Bottle());
    }

    /** A full hive or nest at {@code pos}, or null. */
    private static IBlockState fullHive(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof BlockBeehive && state.getValue(BlockBeehive.HONEY_LEVEL) >= 5 ? state : null;
    }

    private static final class Shears implements IBehaviorDispenseItem {

        private final IBehaviorDispenseItem previous;

        Shears(IBehaviorDispenseItem previous) {
            this.previous = previous;
        }

        @Override
        public ItemStack dispense(IBlockSource source, ItemStack stack) {
            World world = source.getWorld();
            BlockPos pos = source.getBlockPos().offset(source.getBlockState().getValue(BlockDispenser.FACING));
            IBlockState hive = fullHive(world, pos);
            if (hive == null || world.isRemote) return this.previous.dispense(source, stack);
            world.playSound(null, pos, PortiumSounds.BLOCK_BEEHIVE_SHEAR, SoundCategory.BLOCKS, 1.0F, 1.0F);
            if (PortiumItems.HONEYCOMB != null) net.minecraft.block.Block.spawnAsEntity(world, pos, new ItemStack(PortiumItems.HONEYCOMB, 3));
            ((BlockBeehive) hive.getBlock()).takeHoney(world, hive, pos, null, TileEntityBeehive.State.BEE_RELEASED);
            if (stack.attemptDamageItem(1, world.rand, null)) stack.setCount(0);
            // The dispenser's click and smoke, as for any item it uses.
            world.playEvent(1000, source.getBlockPos(), 0);
            world.playEvent(2000, source.getBlockPos(), source.getBlockState().getValue(BlockDispenser.FACING).getIndex());
            return stack;
        }
    }

    private static final class Bottle extends BehaviorDefaultDispenseItem {

        private final BehaviorDefaultDispenseItem drop = new BehaviorDefaultDispenseItem();

        @Override
        protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
            World world = source.getWorld();
            EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
            BlockPos pos = source.getBlockPos().offset(facing);
            IBlockState hive = fullHive(world, pos);
            if (hive != null && PortiumItems.HONEY_BOTTLE != null) {
                ((BlockBeehive) hive.getBlock()).takeHoney(world, hive, pos, null, TileEntityBeehive.State.BEE_RELEASED);
                world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                return this.fill(source, stack, new ItemStack(PortiumItems.HONEY_BOTTLE));
            }
            if (world.getBlockState(pos).getMaterial() == Material.WATER) {
                world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                return this.fill(source, stack, PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER));
            }
            return this.drop.dispense(source, stack);
        }

        /** 1.16's {@code glassBottleFill}: one bottle used, the filled one kept in the dispenser if there's room. */
        private ItemStack fill(IBlockSource source, ItemStack stack, ItemStack filled) {
            stack.shrink(1);
            if (stack.isEmpty()) return filled;
            TileEntityDispenser dispenser = source.getBlockTileEntity();
            if (dispenser.addItemStack(filled.copy()) < 0) this.drop.dispense(source, filled.copy());
            return stack;
        }
    }

    private HiveDispensing() {}
}
