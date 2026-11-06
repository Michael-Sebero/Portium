package com.michaelsebero.portium.merchant;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;

/** 1.14's {@code MerchantResultSlot}: taking the result pays for it and counts the trade. */
public class SlotMerchantResult116 extends Slot {

    private final InventoryMerchant116 merchantInventory;
    private final EntityPlayer player;
    private final IMerchant116 merchant;
    private int removeCount;

    public SlotMerchantResult116(EntityPlayer player, IMerchant116 merchant, InventoryMerchant116 inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.player = player;
        this.merchant = merchant;
        this.merchantInventory = inventory;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack decrStackSize(int amount) {
        if (this.getHasStack()) this.removeCount += Math.min(amount, this.getStack().getCount());
        return super.decrStackSize(amount);
    }

    @Override
    protected void onCrafting(ItemStack stack, int amount) {
        this.removeCount += amount;
        this.onCrafting(stack);
    }

    @Override
    protected void onCrafting(ItemStack stack) {
        stack.onCrafting(this.player.world, this.player, this.removeCount);
        this.removeCount = 0;
    }

    @Override
    public ItemStack onTake(EntityPlayer player, ItemStack stack) {
        this.onCrafting(stack);
        MerchantOffer offer = this.merchantInventory.getCurrentOffer();
        if (offer != null) {
            ItemStack first = this.merchantInventory.getStackInSlot(0);
            ItemStack second = this.merchantInventory.getStackInSlot(1);
            if (offer.doTransaction(first, second) || offer.doTransaction(second, first)) {
                this.merchant.onTrade(offer);
                player.addStat(StatList.TRADED_WITH_VILLAGER);
                this.merchantInventory.setInventorySlotContents(0, first);
                this.merchantInventory.setInventorySlotContents(1, second);
            }
            this.merchant.setXP(this.merchant.getXp() + offer.getGivenExp());
        }
        return stack;
    }
}
