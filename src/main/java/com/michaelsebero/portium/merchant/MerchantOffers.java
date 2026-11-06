package com.michaelsebero.portium.merchant;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;

/** 1.14's {@code MerchantOffers}: a merchant's trades, saved as {@code {Recipes: [...]}} and sent as 1.14 sends them. */
public class MerchantOffers extends ArrayList<MerchantOffer> {

    public MerchantOffers() {}

    public MerchantOffers(NBTTagCompound tag) {
        NBTTagList recipes = tag.getTagList("Recipes", 10);
        for (int i = 0; i < recipes.tagCount(); ++i) this.add(new MerchantOffer(recipes.getCompoundTagAt(i)));
    }

    /** The trade the payment matches: the chosen one if any, else the first that matches. */
    @Nullable
    public MerchantOffer getRecipeFor(ItemStack first, ItemStack second, int index) {
        if (index > 0 && index < this.size()) {
            MerchantOffer offer = this.get(index);
            return offer.matches(first, second) ? offer : null;
        }
        for (MerchantOffer offer : this) {
            if (offer.matches(first, second)) return offer;
        }
        return null;
    }

    public void write(PacketBuffer buffer) {
        buffer.writeByte((byte) (this.size() & 255));
        for (MerchantOffer offer : this) {
            buffer.writeItemStack(offer.getBuyingStackFirst());
            buffer.writeItemStack(offer.getSellingStack());
            ItemStack second = offer.getBuyingStackSecond();
            buffer.writeBoolean(!second.isEmpty());
            if (!second.isEmpty()) buffer.writeItemStack(second);
            buffer.writeBoolean(offer.hasNoUsesLeft());
            buffer.writeInt(offer.getUses());
            buffer.writeInt(offer.getMaxUses());
            buffer.writeInt(offer.getGivenExp());
            buffer.writeInt(offer.getSpecialPrice());
            buffer.writeFloat(offer.getPriceMultiplier());
            buffer.writeInt(offer.getDemand());
        }
    }

    public static MerchantOffers read(PacketBuffer buffer) throws IOException {
        MerchantOffers offers = new MerchantOffers();
        int size = buffer.readByte() & 255;
        for (int i = 0; i < size; ++i) {
            ItemStack first = buffer.readItemStack();
            ItemStack selling = buffer.readItemStack();
            ItemStack second = buffer.readBoolean() ? buffer.readItemStack() : ItemStack.EMPTY;
            boolean outOfStock = buffer.readBoolean();
            int uses = buffer.readInt();
            int maxUses = buffer.readInt();
            int xp = buffer.readInt();
            int specialPrice = buffer.readInt();
            float priceMultiplier = buffer.readFloat();
            int demand = buffer.readInt();
            MerchantOffer offer = new MerchantOffer(first, second, selling, uses, maxUses, xp, priceMultiplier, demand);
            if (outOfStock) offer.makeUnavailable();
            offer.setSpecialPrice(specialPrice);
            offers.add(offer);
        }
        return offers;
    }

    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList recipes = new NBTTagList();
        for (MerchantOffer offer : this) recipes.appendTag(offer.write());
        tag.setTag("Recipes", recipes);
        return tag;
    }
}
