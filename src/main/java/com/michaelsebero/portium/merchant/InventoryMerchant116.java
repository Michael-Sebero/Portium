package com.michaelsebero.portium.merchant;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;

/** 1.14's {@code MerchantInventory}: two payment slots and the result, which follows the payment and chosen trade. */
public class InventoryMerchant116 implements IInventory {

    private final IMerchant116 merchant;
    private final NonNullList<ItemStack> slots = NonNullList.withSize(3, ItemStack.EMPTY);
    @Nullable
    private MerchantOffer currentOffer;
    private int currentRecipeIndex;
    private int exp;

    public InventoryMerchant116(IMerchant116 merchant) {
        this.merchant = merchant;
    }

    @Override
    public int getSizeInventory() {
        return this.slots.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.slots) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return this.slots.get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack stack = this.slots.get(index);
        if (index == 2 && !stack.isEmpty()) return ItemStackHelper.getAndSplit(this.slots, index, stack.getCount());
        ItemStack taken = ItemStackHelper.getAndSplit(this.slots, index, count);
        if (!taken.isEmpty() && isPayment(index)) this.resetRecipeAndSlots();
        return taken;
    }

    private static boolean isPayment(int index) {
        return index == 0 || index == 1;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return ItemStackHelper.getAndRemove(this.slots, index);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        this.slots.set(index, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getInventoryStackLimit()) stack.setCount(this.getInventoryStackLimit());
        if (isPayment(index)) this.resetRecipeAndSlots();
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public void markDirty() {
        this.resetRecipeAndSlots();
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.merchant.getCustomer() == player;
    }

    @Override
    public void openInventory(EntityPlayer player) {}

    @Override
    public void closeInventory(EntityPlayer player) {}

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {}

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        this.slots.clear();
    }

    @Override
    public String getName() {
        return "mob.villager";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString(this.getName());
    }

    /** Finds the trade the payment (either way round) buys, and shows its result. */
    public void resetRecipeAndSlots() {
        this.currentOffer = null;
        ItemStack first;
        ItemStack second;
        if (this.slots.get(0).isEmpty()) {
            first = this.slots.get(1);
            second = ItemStack.EMPTY;
        } else {
            first = this.slots.get(0);
            second = this.slots.get(1);
        }
        if (first.isEmpty()) {
            this.setInventorySlotContents(2, ItemStack.EMPTY);
            this.exp = 0;
            return;
        }
        MerchantOffers offers = this.merchant.getOffers();
        if (!offers.isEmpty()) {
            MerchantOffer offer = offers.getRecipeFor(first, second, this.currentRecipeIndex);
            if (offer == null || offer.hasNoUsesLeft()) {
                this.currentOffer = offer;
                offer = offers.getRecipeFor(second, first, this.currentRecipeIndex);
            }
            if (offer != null && !offer.hasNoUsesLeft()) {
                this.currentOffer = offer;
                this.setInventorySlotContents(2, offer.getCopyOfSellingStack());
                this.exp = offer.getGivenExp();
            } else {
                this.setInventorySlotContents(2, ItemStack.EMPTY);
                this.exp = 0;
            }
        }
        this.merchant.verifySellingItem(this.getStackInSlot(2));
    }

    @Nullable
    public MerchantOffer getCurrentOffer() {
        return this.currentOffer;
    }

    public void setCurrentRecipeIndex(int index) {
        this.currentRecipeIndex = index;
        this.resetRecipeAndSlots();
    }

    public int getClientSideExp() {
        return this.exp;
    }
}
