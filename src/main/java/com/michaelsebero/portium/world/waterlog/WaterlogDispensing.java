package com.michaelsebero.portium.world.waterlog;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.math.BlockPos;

/**
 * A dispenser's empty bucket takes the water out of a waterlogged block in front of it (1.13). Anything else is left
 * to the bucket's previous behaviour. Water buckets need nothing here: pouring one waterlogs through the bucket itself.
 */
public final class WaterlogDispensing implements IBehaviorDispenseItem {

    private final IBehaviorDispenseItem previous;
    private final IBehaviorDispenseItem drain = new Drain();

    private WaterlogDispensing(IBehaviorDispenseItem previous) {
        this.previous = previous;
    }

    public static void register() {
        IBehaviorDispenseItem previous = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.BUCKET);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(Items.BUCKET, new WaterlogDispensing(previous));
    }

    private static BlockPos front(IBlockSource source) {
        return source.getBlockPos().offset(source.getBlockState().getValue(BlockDispenser.FACING));
    }

    @Override
    public ItemStack dispense(IBlockSource source, ItemStack stack) {
        return (Waterlogging.isWaterlogged(source.getWorld(), front(source)) ? this.drain : this.previous).dispense(source, stack);
    }

    /** Fills one bucket, keeping the rest of the stack and making room for the water bucket like 1.13's dispensers. */
    private static final class Drain extends BehaviorDefaultDispenseItem {

        private final BehaviorDefaultDispenseItem drop = new BehaviorDefaultDispenseItem();

        @Override
        protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
            Waterlogging.setWaterlogged(source.getWorld(), front(source), false);
            stack.shrink(1);
            if (stack.isEmpty()) return new ItemStack(Items.WATER_BUCKET);
            if (((TileEntityDispenser) source.getBlockTileEntity()).addItemStack(new ItemStack(Items.WATER_BUCKET)) < 0) {
                this.drop.dispense(source, new ItemStack(Items.WATER_BUCKET));
            }
            return stack;
        }
    }
}
