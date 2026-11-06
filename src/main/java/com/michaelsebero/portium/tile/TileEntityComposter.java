package com.michaelsebero.portium.tile;

import com.michaelsebero.portium.block.workstation.BlockComposter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * What hoppers see of a composter (1.14 makes the block itself an inventory): from above it takes compostable items
 * one by one, from below a full one gives its bone meal. The composter's level stays in its block state.
 */
public class TileEntityComposter extends TileEntity {

    private final IItemHandler top = new Top();
    private final IItemHandler bottom = new Bottom();

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && (facing == EnumFacing.UP || facing == EnumFacing.DOWN)
                || super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (facing == EnumFacing.UP) return (T) this.top;
            if (facing == EnumFacing.DOWN) return (T) this.bottom;
        }
        return super.getCapability(capability, facing);
    }

    private IBlockState state() {
        return this.world.getBlockState(this.pos);
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    private final class Top implements IItemHandler {

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            IBlockState state = TileEntityComposter.this.state();
            if (!(state.getBlock() instanceof BlockComposter) || state.getValue(BlockComposter.LEVEL) >= 7
                    || com.michaelsebero.portium.block.workstation.Compostables.chance(stack) < 0.0F) {
                return stack;
            }
            if (!simulate) {
                IBlockState after = BlockComposter.attemptCompost(state, TileEntityComposter.this.world, TileEntityComposter.this.pos, stack);
                TileEntityComposter.this.world.playEvent(BlockComposter.FILL_EVENT, TileEntityComposter.this.pos, after != state ? 1 : 0);
            }
            ItemStack rest = stack.copy();
            rest.shrink(1);
            return rest;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    }

    private final class Bottom implements IItemHandler {

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            IBlockState state = TileEntityComposter.this.state();
            return state.getBlock() instanceof BlockComposter && state.getValue(BlockComposter.LEVEL) == 8 ? BlockComposter.boneMeal() : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack bonemeal = this.getStackInSlot(slot);
            if (amount <= 0 || bonemeal.isEmpty()) return ItemStack.EMPTY;
            if (!simulate) {
                IBlockState state = TileEntityComposter.this.state();
                TileEntityComposter.this.world.setBlockState(TileEntityComposter.this.pos, state.withProperty(BlockComposter.LEVEL, 0), 3);
            }
            return bonemeal;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    }
}
