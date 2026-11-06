package com.michaelsebero.portium.block.workstation;

import com.michaelsebero.portium.compat.LegacyItem;
import com.michaelsebero.portium.compat.LegacyItems;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 1.14's composter chances, by 1.16's item names (resolved to 1.12 items, so carved pumpkins and the rest map right). */
public final class Compostables {

    private static final Object[][] TABLE = {
            {0.3F, "jungle_leaves", "oak_leaves", "spruce_leaves", "dark_oak_leaves", "acacia_leaves", "birch_leaves", "oak_sapling", "spruce_sapling",
                    "birch_sapling", "jungle_sapling", "acacia_sapling", "dark_oak_sapling", "beetroot_seeds", "dried_kelp", "grass", "kelp",
                    "melon_seeds", "pumpkin_seeds", "seagrass", "sweet_berries", "wheat_seeds"},
            {0.5F, "dried_kelp_block", "tall_grass", "cactus", "sugar_cane", "vine", "nether_sprouts", "weeping_vines", "twisting_vines", "melon_slice"},
            {0.65F, "sea_pickle", "lily_pad", "pumpkin", "carved_pumpkin", "melon", "apple", "beetroot", "carrot", "cocoa_beans", "potato", "wheat",
                    "brown_mushroom", "red_mushroom", "mushroom_stem", "crimson_fungus", "warped_fungus", "nether_wart", "crimson_roots",
                    "warped_roots", "shroomlight", "dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip",
                    "white_tulip", "pink_tulip", "oxeye_daisy", "cornflower", "lily_of_the_valley", "wither_rose", "fern", "sunflower", "lilac",
                    "rose_bush", "peony", "large_fern"},
            {0.85F, "hay_block", "brown_mushroom_block", "red_mushroom_block", "nether_wart_block", "warped_wart_block", "bread", "baked_potato", "cookie"},
            {1.0F, "cake", "pumpkin_pie"}};

    private static final class Entry {
        final LegacyItem item;
        final float chance;

        Entry(LegacyItem item, float chance) {
            this.item = item;
            this.chance = chance;
        }
    }

    private static volatile List<Entry> entries;

    /** The chance an item raises the composter's level, or -1 if it can't be composted. */
    public static float chance(ItemStack stack) {
        if (stack.isEmpty()) return -1.0F;
        for (Entry entry : entries()) {
            if (entry.item.test(stack)) return entry.chance;
        }
        return -1.0F;
    }

    private static List<Entry> entries() {
        List<Entry> list = entries;
        if (list == null) {
            list = new ArrayList<>();
            for (Object[] row : TABLE) {
                float chance = (Float) row[0];
                for (int i = 1; i < row.length; i++) {
                    LegacyItem item = LegacyItems.resolve("minecraft:" + row[i]);
                    // Items the 1.12 world names differently map through LegacyItems; ones Portium has switched off are left out.
                    if (item != null && item.getItem() != net.minecraft.init.Items.AIR) list.add(new Entry(item, chance));
                }
            }
            entries = list = Collections.unmodifiableList(list);
        }
        return list;
    }

    private Compostables() {}
}
