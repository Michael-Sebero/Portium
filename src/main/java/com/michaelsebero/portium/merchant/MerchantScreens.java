package com.michaelsebero.portium.merchant;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.inventory.PortiumGuis;
import com.michaelsebero.portium.network.MerchantOffersMessage;
import com.michaelsebero.portium.network.PortiumNetwork;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

/** 1.14's {@code IMerchant.openMerchantContainer}: opens the trade screen, then sends the offers and the merchant's level. */
public final class MerchantScreens {

    public static <M extends Entity & IMerchant116> void open(EntityPlayer player, M merchant, int level) {
        player.openGui(Portium.instance, PortiumGuis.MERCHANT, merchant.world, merchant.getEntityId(), 0, 0);
        if (!(player instanceof EntityPlayerMP) || !(player.openContainer instanceof ContainerMerchant116)) return;
        MerchantOffers offers = merchant.getOffers();
        if (!offers.isEmpty()) {
            PortiumNetwork.CHANNEL.sendTo(new MerchantOffersMessage(player.openContainer.windowId, offers, level, merchant.getXp(), merchant.hasXPBar(),
                    merchant.canRestockTrades()), (EntityPlayerMP) player);
        }
    }

    private MerchantScreens() {}
}
