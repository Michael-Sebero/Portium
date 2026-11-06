package com.michaelsebero.portium.merchant;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** 1.14's {@code IMerchant}: what the 1.14 trade screen trades with (wandering traders now, villagers with their rework). */
public interface IMerchant116 {

    void setCustomer(@Nullable EntityPlayer player);

    @Nullable
    EntityPlayer getCustomer();

    MerchantOffers getOffers();

    void setClientSideOffers(@Nullable MerchantOffers offers);

    void onTrade(MerchantOffer offer);

    /** The yes/no grunt as the result slot fills or empties. */
    void verifySellingItem(ItemStack stack);

    World getWorld();

    int getXp();

    void setXP(int xp);

    boolean hasXPBar();

    SoundEvent getYesSound();

    default boolean canRestockTrades() {
        return false;
    }
}
