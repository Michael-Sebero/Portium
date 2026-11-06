package com.michaelsebero.portium.merchant.trade;

import com.michaelsebero.portium.compat.LegacyItem;
import com.michaelsebero.portium.compat.LegacyItems;
import com.michaelsebero.portium.merchant.MerchantOffer;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 1.14's {@code ItemsForEmeraldsTrade}: sells an item for emeralds. The item is named as 1.16 names it and looked up
 * through Portium's 1.13 item names (1.12 keeps many items as metadata of one item); a trade for an item that isn't
 * there (its feature switched off) offers nothing.
 */
public class ItemsForEmeraldsTrade implements ITrade {

    private final String item;
    private final int emeraldCount;
    private final int sellingCount;
    private final int maxUses;
    private final int xp;
    private final float priceMultiplier;

    public ItemsForEmeraldsTrade(String item, int emeraldCount, int sellingCount, int maxUses, int xp) {
        this(item, emeraldCount, sellingCount, maxUses, xp, 0.05F);
    }

    public ItemsForEmeraldsTrade(String item, int emeraldCount, int sellingCount, int maxUses, int xp, float priceMultiplier) {
        this.item = item;
        this.emeraldCount = emeraldCount;
        this.sellingCount = sellingCount;
        this.maxUses = maxUses;
        this.xp = xp;
        this.priceMultiplier = priceMultiplier;
    }

    @Nullable
    @Override
    public MerchantOffer getOffer(Entity merchant, Random random) {
        LegacyItem selling = LegacyItems.resolve(this.item);
        if (selling == null) return null;
        return new MerchantOffer(new ItemStack(Items.EMERALD, this.emeraldCount), selling.createStack(this.sellingCount), this.maxUses, this.xp,
                this.priceMultiplier);
    }
}
