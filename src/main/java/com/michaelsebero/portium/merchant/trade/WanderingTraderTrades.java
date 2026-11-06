package com.michaelsebero.portium.merchant.trade;

/** 1.16.5's wandering trader trades: five of the first list and one of the second. */
public final class WanderingTraderTrades {

    public static final ITrade[] GENERIC = {
            sell("sea_pickle", 2, 1, 5), sell("slime_ball", 4, 1, 5), sell("glowstone", 2, 1, 5), sell("nautilus_shell", 5, 1, 5),
            sell("fern", 1, 1, 12), sell("sugar_cane", 1, 1, 8), sell("pumpkin", 1, 1, 4), sell("kelp", 3, 1, 12), sell("cactus", 3, 1, 8),
            sell("dandelion", 1, 1, 12), sell("poppy", 1, 1, 12), sell("blue_orchid", 1, 1, 8), sell("allium", 1, 1, 12),
            sell("azure_bluet", 1, 1, 12), sell("red_tulip", 1, 1, 12), sell("orange_tulip", 1, 1, 12), sell("white_tulip", 1, 1, 12),
            sell("pink_tulip", 1, 1, 12), sell("oxeye_daisy", 1, 1, 12), sell("cornflower", 1, 1, 12), sell("lily_of_the_valley", 1, 1, 7),
            sell("wheat_seeds", 1, 1, 12), sell("beetroot_seeds", 1, 1, 12), sell("pumpkin_seeds", 1, 1, 12), sell("melon_seeds", 1, 1, 12),
            sell("acacia_sapling", 5, 1, 8), sell("birch_sapling", 5, 1, 8), sell("dark_oak_sapling", 5, 1, 8), sell("jungle_sapling", 5, 1, 8),
            sell("oak_sapling", 5, 1, 8), sell("spruce_sapling", 5, 1, 8), sell("red_dye", 1, 3, 12), sell("white_dye", 1, 3, 12),
            sell("blue_dye", 1, 3, 12), sell("pink_dye", 1, 3, 12), sell("black_dye", 1, 3, 12), sell("green_dye", 1, 3, 12),
            sell("light_gray_dye", 1, 3, 12), sell("magenta_dye", 1, 3, 12), sell("yellow_dye", 1, 3, 12), sell("gray_dye", 1, 3, 12),
            sell("purple_dye", 1, 3, 12), sell("light_blue_dye", 1, 3, 12), sell("lime_dye", 1, 3, 12), sell("orange_dye", 1, 3, 12),
            sell("brown_dye", 1, 3, 12), sell("cyan_dye", 1, 3, 12), sell("brain_coral_block", 3, 1, 8), sell("bubble_coral_block", 3, 1, 8),
            sell("fire_coral_block", 3, 1, 8), sell("horn_coral_block", 3, 1, 8), sell("tube_coral_block", 3, 1, 8), sell("vine", 1, 1, 12),
            sell("brown_mushroom", 1, 1, 12), sell("red_mushroom", 1, 1, 12), sell("lily_pad", 1, 2, 5), sell("sand", 1, 8, 8),
            sell("red_sand", 1, 4, 6)};

    public static final ITrade[] RARE = {
            sell("tropical_fish_bucket", 5, 1, 4), sell("pufferfish_bucket", 5, 1, 4), sell("packed_ice", 3, 1, 6), sell("blue_ice", 6, 1, 6),
            sell("gunpowder", 1, 1, 8), sell("podzol", 3, 3, 6)};

    private static ITrade sell(String item, int emeralds, int count, int maxUses) {
        return new ItemsForEmeraldsTrade("minecraft:" + item, emeralds, count, maxUses, 1);
    }

    private WanderingTraderTrades() {}
}
