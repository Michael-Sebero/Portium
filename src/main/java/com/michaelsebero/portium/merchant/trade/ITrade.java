package com.michaelsebero.portium.merchant.trade;

import com.michaelsebero.portium.merchant.MerchantOffer;
import net.minecraft.entity.Entity;

import javax.annotation.Nullable;
import java.util.Random;

/** 1.14's {@code VillagerTrades.ITrade}: makes one offer for a merchant. */
public interface ITrade {

    @Nullable
    MerchantOffer getOffer(Entity merchant, Random random);
}
