package com.michaelsebero.portium.merchant;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** 1.14's {@code NPCMerchant}: the client's stand-in for the merchant behind an open trade screen. */
public class ClientMerchant implements IMerchant116 {

    private final EntityPlayer customer;
    private MerchantOffers offers = new MerchantOffers();
    private int xp;

    public ClientMerchant(EntityPlayer customer) {
        this.customer = customer;
    }

    @Override
    public EntityPlayer getCustomer() {
        return this.customer;
    }

    @Override
    public void setCustomer(@Nullable EntityPlayer player) {}

    @Override
    public MerchantOffers getOffers() {
        return this.offers;
    }

    @Override
    public void setClientSideOffers(@Nullable MerchantOffers offers) {
        this.offers = offers == null ? new MerchantOffers() : offers;
    }

    @Override
    public void onTrade(MerchantOffer offer) {
        offer.increaseUses();
    }

    @Override
    public void verifySellingItem(ItemStack stack) {}

    @Override
    public World getWorld() {
        return this.customer.world;
    }

    @Override
    public int getXp() {
        return this.xp;
    }

    @Override
    public void setXP(int xp) {
        this.xp = xp;
    }

    @Override
    public boolean hasXPBar() {
        return true;
    }

    @Override
    public SoundEvent getYesSound() {
        return SoundEvents.ENTITY_VILLAGER_YES;
    }
}
