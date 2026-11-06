package com.michaelsebero.portium.item;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.BannerPattern;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.oredict.DyeUtils;

import javax.annotation.Nullable;

/**
 * 1.14's banner patterns as the loom lists them: 1.16's order, its two new patterns (globe, piglin) added to 1.12's
 * pattern list so banners and shields draw them, and which item unlocks which.
 */
public final class BannerPatterns116 {

    /** 1.16's BannerPattern order, by hash name: the base, the 34 the loom offers, then the 6 that need an item. */
    public static final String[] ORDER = {"b", "bl", "br", "tl", "tr", "bs", "ts", "ls", "rs", "cs", "ms", "drs", "dls", "ss", "cr", "sc", "bt", "tt",
            "bts", "tts", "ld", "rd", "lud", "rud", "mc", "mr", "vh", "hh", "vhr", "hhb", "bo", "cbo", "gra", "gru", "bri", "glb", "cre", "sku", "flo",
            "moj", "pig"};
    /** Patterns that need an item in the loom. */
    public static final int WITH_ITEMS = 6;
    /** The last pattern the loom offers without an item. */
    public static final int PATTERN_ITEM_INDEX = ORDER.length - WITH_ITEMS - 1;
    /** Rows of the loom's pattern grid. */
    public static final int ROWS = (ORDER.length - WITH_ITEMS - 1 + 4 - 1) / 4;

    private static BannerPattern globe;
    private static BannerPattern piglin;

    /** Adds 1.16's globe and piglin patterns to 1.12's list. Before items register. */
    public static void register() {
        if (globe != null) return;
        globe = BannerPattern.byHash("glb") != null ? BannerPattern.byHash("glb")
                : EnumHelper.addEnum(BannerPattern.class, "GLOBE", new Class<?>[] {String.class, String.class}, "globe", "glb");
        piglin = BannerPattern.byHash("pig") != null ? BannerPattern.byHash("pig")
                : EnumHelper.addEnum(BannerPattern.class, "PIGLIN", new Class<?>[] {String.class, String.class}, "piglin", "pig");
    }

    @Nullable
    public static BannerPattern byIndex(int index) {
        return index >= 0 && index < ORDER.length ? BannerPattern.byHash(ORDER[index]) : null;
    }

    public static int indexOf(String hash) {
        for (int i = 0; i < ORDER.length; i++) if (ORDER[i].equals(hash)) return i;
        return -1;
    }

    /** A dye's colour: 1.12's dyes, 1.14's, and other mods' (by the ore dictionary, as Forge's banner recipes do). */
    @Nullable
    public static EnumDyeColor dyeColor(ItemStack stack) {
        if (stack.getItem() instanceof ItemDye114) return ((ItemDye114) stack.getItem()).getColor();
        return DyeUtils.colorFromStack(stack).orElse(null);
    }

    /** Forge 1.16's {@code BannerPattern.create(enumName, fileName, hashName, hasPatternItem)}. */
    public static BannerPattern create(String enumName, String fileName, String hashName, boolean hasPatternItem) {
        BannerPattern existing = BannerPattern.byHash(hashName);
        if (existing != null) return existing;
        return EnumHelper.addEnum(BannerPattern.class, enumName, new Class<?>[] {String.class, String.class}, fileName, hashName);
    }

    private BannerPatterns116() {}
}
