package com.michaelsebero.portium.merchant;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;

/** 1.14's {@code MerchantContainer}: the 1.14 layout, and choosing a trade fills the payment slots from the inventory. */
public class ContainerMerchant116 extends Container {

    private final IMerchant116 merchant;
    private final InventoryMerchant116 merchantInventory;
    private int merchantLevel;
    private boolean showProgressBar;
    private boolean canRestock;

    public ContainerMerchant116(InventoryPlayer playerInventory, IMerchant116 merchant) {
        this.merchant = merchant;
        this.merchantInventory = new InventoryMerchant116(merchant);
        this.addSlotToContainer(new Slot(this.merchantInventory, 0, 136, 37));
        this.addSlotToContainer(new Slot(this.merchantInventory, 1, 162, 37));
        this.addSlotToContainer(new SlotMerchantResult116(playerInventory.player, merchant, this.merchantInventory, 2, 220, 37));
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlotToContainer(new Slot(playerInventory, column + row * 9 + 9, 108 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; ++column) this.addSlotToContainer(new Slot(playerInventory, column, 108 + column * 18, 142));
    }

    public void setShowProgressBar(boolean show) {
        this.showProgressBar = show;
    }

    public boolean showProgressBar() {
        return this.showProgressBar;
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventory) {
        this.merchantInventory.resetRecipeAndSlots();
        super.onCraftMatrixChanged(inventory);
    }

    public void setCurrentRecipeIndex(int index) {
        this.merchantInventory.setCurrentRecipeIndex(index);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.merchant.getCustomer() == player;
    }

    public int getXp() {
        return this.merchant.getXp();
    }

    public int getPendingExp() {
        return this.merchantInventory.getClientSideExp();
    }

    public void setXp(int xp) {
        this.merchant.setXP(xp);
    }

    public int getMerchantLevel() {
        return this.merchantLevel;
    }

    public void setMerchantLevel(int level) {
        this.merchantLevel = level;
    }

    public void setCanRestock(boolean canRestock) {
        this.canRestock = canRestock;
    }

    public boolean canRestock() {
        return this.canRestock;
    }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slot) {
        return false;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            if (index == 2) {
                if (!this.mergeItemStack(stack, 3, 39, true)) return ItemStack.EMPTY;
                slot.onSlotChange(stack, result);
                this.playMerchantYesSound();
            } else if (index != 0 && index != 1) {
                if (index >= 3 && index < 30) {
                    if (!this.mergeItemStack(stack, 30, 39, false)) return ItemStack.EMPTY;
                } else if (index >= 30 && index < 39 && !this.mergeItemStack(stack, 3, 30, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(stack, 3, 39, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();
            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return result;
    }

    private void playMerchantYesSound() {
        if (!this.merchant.getWorld().isRemote && this.merchant instanceof Entity) {
            Entity entity = (Entity) this.merchant;
            this.merchant.getWorld().playSound(null, entity.posX, entity.posY, entity.posZ, this.merchant.getYesSound(), SoundCategory.NEUTRAL, 1.0F, 1.0F);
        }
    }

    /** 1.14 puts the payment back in the inventory (dropping what doesn't fit), where 1.12 dropped it. */
    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        this.merchant.setCustomer(null);
        if (this.merchant.getWorld().isRemote) return;
        boolean present = player.isEntityAlive() && !(player instanceof EntityPlayerMP && ((EntityPlayerMP) player).hasDisconnected());
        for (int slot = 0; slot < 2; ++slot) {
            ItemStack stack = this.merchantInventory.removeStackFromSlot(slot);
            if (stack.isEmpty()) continue;
            if (!present || !player.inventory.addItemStackToInventory(stack)) player.dropItem(stack, false);
        }
    }

    /** Choosing a trade: payment goes back to the inventory, then the inventory's matching items fill the slots. */
    public void fillPaymentFor(int index) {
        if (this.getOffers().size() <= index) return;
        ItemStack first = this.merchantInventory.getStackInSlot(0);
        if (!first.isEmpty()) {
            if (!this.mergeItemStack(first, 3, 39, true)) return;
            this.merchantInventory.setInventorySlotContents(0, first);
        }
        ItemStack second = this.merchantInventory.getStackInSlot(1);
        if (!second.isEmpty()) {
            if (!this.mergeItemStack(second, 3, 39, true)) return;
            this.merchantInventory.setInventorySlotContents(1, second);
        }
        if (this.merchantInventory.getStackInSlot(0).isEmpty() && this.merchantInventory.getStackInSlot(1).isEmpty()) {
            this.moveFromInventory(0, this.getOffers().get(index).getDiscountedBuyingStackFirst());
            this.moveFromInventory(1, this.getOffers().get(index).getBuyingStackSecond());
        }
    }

    private void moveFromInventory(int slot, ItemStack wanted) {
        if (wanted.isEmpty()) return;
        for (int i = 3; i < 39; ++i) {
            ItemStack stack = this.inventorySlots.get(i).getStack();
            if (stack.isEmpty() || !sameItem(wanted, stack)) continue;
            ItemStack current = this.merchantInventory.getStackInSlot(slot);
            int have = current.isEmpty() ? 0 : current.getCount();
            int move = Math.min(wanted.getMaxStackSize() - have, stack.getCount());
            ItemStack moved = stack.copy();
            int total = have + move;
            stack.shrink(move);
            moved.setCount(total);
            this.merchantInventory.setInventorySlotContents(slot, moved);
            if (total >= wanted.getMaxStackSize()) break;
        }
    }

    /** 1.14 compares item and tags (1.12's metadata stands in for the item where it tells items apart). */
    private static boolean sameItem(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem() && (a.isItemStackDamageable() || a.getMetadata() == b.getMetadata()) && ItemStack.areItemStackTagsEqual(a, b);
    }

    public void setClientSideOffers(MerchantOffers offers) {
        this.merchant.setClientSideOffers(offers);
    }

    public MerchantOffers getOffers() {
        return this.merchant.getOffers();
    }
}
