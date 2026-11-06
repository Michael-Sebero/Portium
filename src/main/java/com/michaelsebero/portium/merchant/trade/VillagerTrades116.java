package com.michaelsebero.portium.merchant.trade;

import com.michaelsebero.portium.compat.LegacyItem;
import com.michaelsebero.portium.compat.LegacyItems;
import com.michaelsebero.portium.entity.villager.VillagerProfession116;
import com.michaelsebero.portium.entity.villager.VillagerType116;
import com.michaelsebero.portium.entity.villager.IVillagerDataHolder;
import com.michaelsebero.portium.merchant.MerchantOffer;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.MapDecoration;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 1.16.5's villager trades by profession and level (1 novice to 5 master). A villager takes two random trades of its
 * level's list each time it levels up. Items are named as 1.16 names them, found through Portium's 1.13 item names;
 * a trade whose item isn't there (its feature switched off) offers nothing.
 */
public final class VillagerTrades116 {

    private static final Map<VillagerProfession116, ITrade[][]> TRADES = new EnumMap<>(VillagerProfession116.class);

    private static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
            "purple", "blue", "brown", "green", "red", "black"};

    static {
        TRADES.put(VillagerProfession116.FARMER, new ITrade[][] {
                {buy("wheat", 20, 16, 2), buy("potato", 26, 16, 2), buy("carrot", 22, 16, 2), buy("beetroot", 15, 16, 2), sell("bread", 1, 6, 16, 1)},
                {buy("pumpkin", 6, 12, 10), sell("pumpkin_pie", 1, 4, 5), sell("apple", 1, 4, 16, 5)},
                {sell("cookie", 3, 18, 10), buy("melon", 4, 12, 20)},
                {sell("cake", 1, 1, 12, 15), stew(MobEffects.NIGHT_VISION, 100), stew(MobEffects.JUMP_BOOST, 160), stew(MobEffects.WEAKNESS, 140),
                        stew(MobEffects.BLINDNESS, 120), stew(MobEffects.POISON, 280), stew(MobEffects.SATURATION, 7)},
                {sell("golden_carrot", 3, 3, 30), sell("glistering_melon_slice", 4, 3, 30)}});
        TRADES.put(VillagerProfession116.FISHERMAN, new ITrade[][] {
                {buy("string", 20, 16, 2), buy("coal", 10, 16, 2), swap("cod", 6, 1, "cooked_cod", 6, 16, 1), sell("cod_bucket", 3, 1, 16, 1)},
                {buy("cod", 15, 16, 10), swap("salmon", 6, 1, "cooked_salmon", 6, 16, 5), sell("campfire", 2, 1, 5)},
                {buy("salmon", 13, 16, 20), enchanted("fishing_rod", 3, 3, 10, 0.2F)},
                {buy("tropical_fish", 6, 12, 30)},
                {buy("pufferfish", 4, 12, 30), new EmeraldForVillagerTypeItem(1, 12, 30)}});
        TRADES.put(VillagerProfession116.SHEPHERD, new ITrade[][] {
                {buy("white_wool", 18, 16, 2), buy("brown_wool", 18, 16, 2), buy("black_wool", 18, 16, 2), buy("gray_wool", 18, 16, 2),
                        sell("shears", 2, 1, 1)},
                concat(new ITrade[] {buy("white_dye", 12, 16, 10), buy("gray_dye", 12, 16, 10), buy("black_dye", 12, 16, 10),
                        buy("light_blue_dye", 12, 16, 10), buy("lime_dye", 12, 16, 10)}, colored("_wool", 1, 1, 16, 5), colored("_carpet", 1, 4, 16, 5)),
                concat(new ITrade[] {buy("yellow_dye", 12, 16, 20), buy("light_gray_dye", 12, 16, 20), buy("orange_dye", 12, 16, 20),
                        buy("red_dye", 12, 16, 20), buy("pink_dye", 12, 16, 20)}, colored("_bed", 3, 1, 12, 10)),
                concat(new ITrade[] {buy("brown_dye", 12, 16, 30), buy("purple_dye", 12, 16, 30), buy("blue_dye", 12, 16, 30),
                        buy("green_dye", 12, 16, 30), buy("magenta_dye", 12, 16, 30), buy("cyan_dye", 12, 16, 30)}, colored("_banner", 3, 1, 12, 15)),
                {sell("painting", 2, 3, 30)}});
        TRADES.put(VillagerProfession116.FLETCHER, new ITrade[][] {
                {buy("stick", 32, 16, 2), sell("arrow", 1, 16, 1), swap("gravel", 10, 1, "flint", 10, 12, 1)},
                {buy("flint", 26, 12, 10), sell("bow", 2, 1, 5)},
                {buy("string", 14, 16, 20), sell("crossbow", 3, 1, 10)},
                {buy("feather", 24, 16, 30), enchanted("bow", 2, 3, 15, 0.05F)},
                {buy("tripwire_hook", 8, 12, 30), enchanted("crossbow", 3, 3, 15, 0.05F), new TippedArrowForItemsAndEmeralds(2, 5, 5, 12, 30)}});
        TRADES.put(VillagerProfession116.LIBRARIAN, new ITrade[][] {
                {buy("paper", 24, 16, 2), new EnchantedBookForEmeralds(1), sell("bookshelf", 9, 1, 12, 1)},
                {buy("book", 4, 12, 10), new EnchantedBookForEmeralds(5), sell("lantern", 1, 1, 5)},
                {buy("ink_sac", 5, 12, 20), new EnchantedBookForEmeralds(10), sell("glass", 1, 4, 10)},
                {buy("writable_book", 2, 12, 30), new EnchantedBookForEmeralds(15), sell("clock", 5, 1, 15), sell("compass", 4, 1, 15)},
                {sell("name_tag", 20, 1, 30)}});
        TRADES.put(VillagerProfession116.CARTOGRAPHER, new ITrade[][] {
                {buy("paper", 24, 16, 2), sell("map", 7, 1, 1)},
                {buy("glass_pane", 11, 16, 10), new EmeraldForMap(13, "Monument", MapDecoration.Type.MONUMENT, "filled_map.monument", 12, 5)},
                {buy("compass", 1, 12, 20), new EmeraldForMap(14, "Mansion", MapDecoration.Type.MANSION, "filled_map.mansion", 12, 10)},
                concat(new ITrade[] {sell("item_frame", 7, 1, 15)}, colored("_banner", 3, 1, 12, 15)),
                {sell("globe_banner_pattern", 8, 1, 30)}});
        TRADES.put(VillagerProfession116.CLERIC, new ITrade[][] {
                {buy("rotten_flesh", 32, 16, 2), sell("redstone", 1, 2, 1)},
                {buy("gold_ingot", 3, 12, 10), sell("lapis_lazuli", 1, 1, 5)},
                {buy("rabbit_foot", 2, 12, 20), sell("glowstone", 4, 1, 12, 10)},
                {buy("scute", 4, 12, 30), buy("glass_bottle", 9, 12, 30), sell("ender_pearl", 5, 1, 15)},
                {buy("nether_wart", 22, 12, 30), sell("experience_bottle", 3, 1, 30)}});
        TRADES.put(VillagerProfession116.ARMORER, new ITrade[][] {
                {buy("coal", 15, 16, 2), sell("iron_leggings", 7, 1, 12, 1, 0.2F), sell("iron_boots", 4, 1, 12, 1, 0.2F),
                        sell("iron_helmet", 5, 1, 12, 1, 0.2F), sell("iron_chestplate", 9, 1, 12, 1, 0.2F)},
                {buy("iron_ingot", 4, 12, 10), sell("bell", 36, 1, 12, 5, 0.2F), sell("chainmail_boots", 1, 1, 12, 5, 0.2F),
                        sell("chainmail_leggings", 3, 1, 12, 5, 0.2F)},
                {buy("lava_bucket", 1, 12, 20), buy("diamond", 1, 12, 20), sell("chainmail_helmet", 1, 1, 12, 10, 0.2F),
                        sell("chainmail_chestplate", 4, 1, 12, 10, 0.2F), sell("shield", 5, 1, 12, 10, 0.2F)},
                {enchanted("diamond_leggings", 14, 3, 15, 0.2F), enchanted("diamond_boots", 8, 3, 15, 0.2F)},
                {enchanted("diamond_helmet", 8, 3, 30, 0.2F), enchanted("diamond_chestplate", 16, 3, 30, 0.2F)}});
        TRADES.put(VillagerProfession116.WEAPONSMITH, new ITrade[][] {
                {buy("coal", 15, 16, 2), sell("iron_axe", 3, 1, 12, 1, 0.2F), enchanted("iron_sword", 2, 3, 1, 0.05F)},
                {buy("iron_ingot", 4, 12, 10), sell("bell", 36, 1, 12, 5, 0.2F)},
                {buy("flint", 24, 12, 20)},
                {buy("diamond", 1, 12, 30), enchanted("diamond_axe", 12, 3, 15, 0.2F)},
                {enchanted("diamond_sword", 8, 3, 30, 0.2F)}});
        TRADES.put(VillagerProfession116.TOOLSMITH, new ITrade[][] {
                {buy("coal", 15, 16, 2), sell("stone_axe", 1, 1, 12, 1, 0.2F), sell("stone_shovel", 1, 1, 12, 1, 0.2F),
                        sell("stone_pickaxe", 1, 1, 12, 1, 0.2F), sell("stone_hoe", 1, 1, 12, 1, 0.2F)},
                {buy("iron_ingot", 4, 12, 10), sell("bell", 36, 1, 12, 5, 0.2F)},
                {buy("flint", 30, 12, 20), enchanted("iron_axe", 1, 3, 10, 0.2F), enchanted("iron_shovel", 2, 3, 10, 0.2F),
                        enchanted("iron_pickaxe", 3, 3, 10, 0.2F), sell("diamond_hoe", 4, 1, 3, 10, 0.2F)},
                {buy("diamond", 1, 12, 30), enchanted("diamond_axe", 12, 3, 15, 0.2F), enchanted("diamond_shovel", 5, 3, 15, 0.2F)},
                {enchanted("diamond_pickaxe", 13, 3, 30, 0.2F)}});
        TRADES.put(VillagerProfession116.BUTCHER, new ITrade[][] {
                {buy("chicken", 14, 16, 2), buy("porkchop", 7, 16, 2), buy("rabbit", 4, 16, 2), sell("rabbit_stew", 1, 1, 1)},
                {buy("coal", 15, 16, 2), sell("cooked_porkchop", 1, 5, 16, 5), sell("cooked_chicken", 1, 8, 16, 5)},
                {buy("mutton", 7, 16, 20), buy("beef", 10, 16, 20)},
                {buy("dried_kelp_block", 10, 12, 30)},
                {buy("sweet_berries", 10, 12, 30)}});
        TRADES.put(VillagerProfession116.LEATHERWORKER, new ITrade[][] {
                {buy("leather", 6, 16, 2), dyed("leather_leggings", 3, 12, 1), dyed("leather_chestplate", 7, 12, 1)},
                {buy("flint", 26, 12, 10), dyed("leather_helmet", 5, 12, 5), dyed("leather_boots", 4, 12, 5)},
                {buy("rabbit_hide", 9, 12, 20), dyed("leather_chestplate", 7, 12, 10)},
                {buy("scute", 4, 12, 30), dyed("leather_horse_armor", 6, 12, 15)},
                {sell("saddle", 6, 1, 12, 30, 0.2F), dyed("leather_helmet", 5, 12, 30)}});
        TRADES.put(VillagerProfession116.MASON, new ITrade[][] {
                {buy("clay_ball", 10, 16, 2), sell("brick", 1, 10, 16, 1)},
                {buy("stone", 20, 16, 10), sell("chiseled_stone_bricks", 1, 4, 16, 5)},
                {buy("granite", 16, 16, 20), buy("andesite", 16, 16, 20), buy("diorite", 16, 16, 20), sell("polished_andesite", 1, 4, 16, 10),
                        sell("polished_diorite", 1, 4, 16, 10), sell("polished_granite", 1, 4, 16, 10)},
                concat(new ITrade[] {buy("quartz", 12, 12, 30)}, colored("_terracotta", 1, 1, 12, 15), colored("_glazed_terracotta", 1, 1, 12, 15)),
                {sell("quartz_pillar", 1, 1, 12, 30), sell("quartz_block", 1, 1, 12, 30)}});
    }

    /** The trades a villager of this profession can be offered at this level (1 to 5); none for the jobless. */
    public static ITrade[] forLevel(VillagerProfession116 profession, int level) {
        ITrade[][] levels = TRADES.get(profession);
        if (levels == null || level < 1 || level > levels.length) return new ITrade[0];
        return levels[level - 1];
    }

    // ---- Shorthands ----

    private static ITrade buy(String item, int count, int maxUses, int xp) {
        return new EmeraldForItems("minecraft:" + item, count, maxUses, xp);
    }

    private static ITrade sell(String item, int emeralds, int count, int xp) {
        return new ItemsForEmeraldsTrade("minecraft:" + item, emeralds, count, 12, xp);
    }

    private static ITrade sell(String item, int emeralds, int count, int maxUses, int xp) {
        return new ItemsForEmeraldsTrade("minecraft:" + item, emeralds, count, maxUses, xp);
    }

    private static ITrade sell(String item, int emeralds, int count, int maxUses, int xp, float multiplier) {
        return new ItemsForEmeraldsTrade("minecraft:" + item, emeralds, count, maxUses, xp, multiplier);
    }

    private static ITrade swap(String bought, int boughtCount, int emeralds, String sold, int soldCount, int maxUses, int xp) {
        return new ItemWithEmeraldsForItems("minecraft:" + bought, boughtCount, emeralds, "minecraft:" + sold, soldCount, maxUses, xp);
    }

    private static ITrade enchanted(String item, int emeralds, int maxUses, int xp, float multiplier) {
        return new EnchantedItemForEmeralds("minecraft:" + item, emeralds, maxUses, xp, multiplier);
    }

    private static ITrade dyed(String item, int emeralds, int maxUses, int xp) {
        return new DyedArmorForEmeralds("minecraft:" + item, emeralds, maxUses, xp);
    }

    private static ITrade stew(Potion effect, int duration) {
        return new SuspiciousStewForEmerald(effect, duration);
    }

    private static ITrade[] colored(String suffix, int emeralds, int count, int maxUses, int xp) {
        ITrade[] trades = new ITrade[COLORS.length];
        for (int i = 0; i < COLORS.length; i++) trades[i] = new ItemsForEmeraldsTrade("minecraft:" + COLORS[i] + suffix, emeralds, count, maxUses, xp);
        return trades;
    }

    private static ITrade[] concat(ITrade[]... parts) {
        List<ITrade> all = new ArrayList<>();
        for (ITrade[] part : parts) java.util.Collections.addAll(all, part);
        return all.toArray(new ITrade[0]);
    }

    @Nullable
    private static ItemStack stack(String name, int count) {
        LegacyItem item = LegacyItems.resolve(name);
        return item == null ? null : item.createStack(count);
    }

    // ---- Trade kinds ----

    /** Buys a stack of an item for an emerald. */
    static final class EmeraldForItems implements ITrade {
        private final String item;
        private final int count, maxUses, xp;

        EmeraldForItems(String item, int count, int maxUses, int xp) {
            this.item = item;
            this.count = count;
            this.maxUses = maxUses;
            this.xp = xp;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(Entity merchant, Random random) {
            ItemStack bought = stack(this.item, this.count);
            return bought == null ? null : new MerchantOffer(bought, new ItemStack(Items.EMERALD), this.maxUses, this.xp, 0.05F);
        }
    }

    /** Takes an item and emeralds for another (the fisherman's cooking). */
    static final class ItemWithEmeraldsForItems implements ITrade {
        private final String bought, sold;
        private final int boughtCount, emeralds, soldCount, maxUses, xp;

        ItemWithEmeraldsForItems(String bought, int boughtCount, int emeralds, String sold, int soldCount, int maxUses, int xp) {
            this.bought = bought;
            this.boughtCount = boughtCount;
            this.emeralds = emeralds;
            this.sold = sold;
            this.soldCount = soldCount;
            this.maxUses = maxUses;
            this.xp = xp;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(Entity merchant, Random random) {
            ItemStack in = stack(this.bought, this.boughtCount);
            ItemStack out = stack(this.sold, this.soldCount);
            if (in == null || out == null) return null;
            return new MerchantOffer(new ItemStack(Items.EMERALD, this.emeralds), in, out, this.maxUses, this.xp, 0.05F);
        }
    }

    /** An item enchanted at a random level of 5 to 19, for its base price plus that level (at most 64). */
    static final class EnchantedItemForEmeralds implements ITrade {
        private final String item;
        private final int emeralds, maxUses, xp;
        private final float multiplier;

        EnchantedItemForEmeralds(String item, int emeralds, int maxUses, int xp, float multiplier) {
            this.item = item;
            this.emeralds = emeralds;
            this.maxUses = maxUses;
            this.xp = xp;
            this.multiplier = multiplier;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(Entity merchant, Random random) {
            ItemStack stack = stack(this.item, 1);
            if (stack == null) return null;
            int level = 5 + random.nextInt(15);
            stack = EnchantmentHelper.addRandomEnchantment(random, stack, level, false);
            int price = Math.min(this.emeralds + level, 64);
            return new MerchantOffer(new ItemStack(Items.EMERALD, price), stack, this.maxUses, this.xp, this.multiplier);
        }
    }

    /** A random enchanted book (any enchantment villagers trade), priced by level; treasure enchantments cost double. */
    static final class EnchantedBookForEmeralds implements ITrade {
        private final int xp;

        EnchantedBookForEmeralds(int xp) {
            this.xp = xp;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(Entity merchant, Random random) {
            List<Enchantment> tradeable = new ArrayList<>();
            for (Enchantment enchantment : ForgeRegistries.ENCHANTMENTS) {
                if (com.michaelsebero.portium.enchantment.EnchantmentSoulSpeed.class.isInstance(enchantment)) continue;
                tradeable.add(enchantment);
            }
            if (tradeable.isEmpty()) return null;
            Enchantment enchantment = tradeable.get(random.nextInt(tradeable.size()));
            int level = MathHelper.getInt(random, enchantment.getMinLevel(), enchantment.getMaxLevel());
            ItemStack book = ItemEnchantedBook.getEnchantedItemStack(new EnchantmentData(enchantment, level));
            int price = 2 + random.nextInt(5 + level * 10) + 3 * level;
            if (enchantment.isTreasureEnchantment()) price *= 2;
            if (price > 64) price = 64;
            return new MerchantOffer(new ItemStack(Items.EMERALD, price), new ItemStack(Items.BOOK), book, 12, this.xp, 0.2F);
        }
    }

    /** A suspicious stew with one effect for an emerald. */
    static final class SuspiciousStewForEmerald implements ITrade {
        private final Potion effect;
        private final int duration;

        SuspiciousStewForEmerald(Potion effect, int duration) {
            this.effect = effect;
            this.duration = duration;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(Entity merchant, Random random) {
            Item stewItem = com.michaelsebero.portium.init.PortiumItems.SUSPICIOUS_STEW;
            if (stewItem == null) return null;
            ItemStack stew = new ItemStack(stewItem);
            com.michaelsebero.portium.item.ItemSuspiciousStew.addEffect(stew, this.effect, this.duration);
            return new MerchantOffer(new ItemStack(Items.EMERALD), stew, 12, 15, 0.05F);
        }
    }

    /** Buys the boat of the villager's biome type. */
    static final class EmeraldForVillagerTypeItem implements ITrade {
        private final int count, maxUses, xp;

        EmeraldForVillagerTypeItem(int count, int maxUses, int xp) {
            this.count = count;
            this.maxUses = maxUses;
            this.xp = xp;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(Entity merchant, Random random) {
            if (!(merchant instanceof IVillagerDataHolder)) return null;
            VillagerType116 type = ((IVillagerDataHolder) merchant).getVillagerData().getType();
            Item boat;
            switch (type) {
                case TAIGA:
                case SNOW:
                    boat = Items.SPRUCE_BOAT;
                    break;
                case DESERT:
                case JUNGLE:
                    boat = Items.JUNGLE_BOAT;
                    break;
                case SAVANNA:
                    boat = Items.ACACIA_BOAT;
                    break;
                case SWAMP:
                    boat = Items.DARK_OAK_BOAT;
                    break;
                default:
                    boat = Items.BOAT;
            }
            return new MerchantOffer(new ItemStack(boat, this.count), new ItemStack(Items.EMERALD), this.maxUses, this.xp, 0.05F);
        }
    }

    /** Tips arrows: arrows and emeralds for tipped arrows of a random brewable potion. */
    static final class TippedArrowForItemsAndEmeralds implements ITrade {
        private final int emeralds, arrows, tipped, maxUses, xp;

        TippedArrowForItemsAndEmeralds(int emeralds, int arrows, int tipped, int maxUses, int xp) {
            this.emeralds = emeralds;
            this.arrows = arrows;
            this.tipped = tipped;
            this.maxUses = maxUses;
            this.xp = xp;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(Entity merchant, Random random) {
            List<PotionType> potions = new ArrayList<>();
            for (PotionType potion : ForgeRegistries.POTION_TYPES) {
                // 1.16's brewable potions: every one with effects but luck, which no brewing makes.
                if (!potion.getEffects().isEmpty() && !"luck".equals(potion.getRegistryName().getPath())) potions.add(potion);
            }
            if (potions.isEmpty()) return null;
            ItemStack arrows = PotionUtils.addPotionToItemStack(new ItemStack(Items.TIPPED_ARROW, this.tipped), potions.get(random.nextInt(potions.size())));
            return new MerchantOffer(new ItemStack(Items.EMERALD, this.emeralds), new ItemStack(Items.ARROW, this.arrows), arrows, this.maxUses, this.xp,
                    0.05F);
        }
    }

    /** Leather armour dyed with one to three random dyes (70% chance of each further dye, as 1.16). */
    static final class DyedArmorForEmeralds implements ITrade {
        private final String item;
        private final int emeralds, maxUses, xp;

        DyedArmorForEmeralds(String item, int emeralds, int maxUses, int xp) {
            this.item = item;
            this.emeralds = emeralds;
            this.maxUses = maxUses;
            this.xp = xp;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(Entity merchant, Random random) {
            ItemStack armor = stack(this.item, 1);
            if (armor == null) return null;
            List<EnumDyeColor> dyes = new ArrayList<>();
            dyes.add(EnumDyeColor.byDyeDamage(random.nextInt(16)));
            if (random.nextFloat() > 0.7F) dyes.add(EnumDyeColor.byDyeDamage(random.nextInt(16)));
            if (random.nextFloat() > 0.8F) dyes.add(EnumDyeColor.byDyeDamage(random.nextInt(16)));
            int r = 0, g = 0, b = 0, max = 0;
            for (EnumDyeColor dye : dyes) {
                float[] rgb = dye.getColorComponentValues();
                int dr = (int) (rgb[0] * 255.0F), dg = (int) (rgb[1] * 255.0F), db = (int) (rgb[2] * 255.0F);
                max += Math.max(dr, Math.max(dg, db));
                r += dr;
                g += dg;
                b += db;
            }
            int n = dyes.size();
            r /= n;
            g /= n;
            b /= n;
            float average = (float) max / n;
            float brightest = Math.max(r, Math.max(g, b));
            if (brightest > 0) {
                r = (int) (r * average / brightest);
                g = (int) (g * average / brightest);
                b = (int) (b * average / brightest);
            }
            int color = (r << 16) | (g << 8) | b;
            if (armor.getItem() instanceof ItemArmor) {
                ((ItemArmor) armor.getItem()).setColor(armor, color);
            } else {
                net.minecraft.nbt.NBTTagCompound display = armor.getOrCreateSubCompound("display");
                display.setInteger("color", color);
            }
            return new MerchantOffer(new ItemStack(Items.EMERALD, this.emeralds), armor, this.maxUses, this.xp, 0.2F);
        }
    }

    /** An explorer map to the nearest unexplored structure, for emeralds and a compass. */
    static final class EmeraldForMap implements ITrade {
        private final int emeralds, maxUses, xp;
        private final String structure, name;
        private final MapDecoration.Type decoration;

        EmeraldForMap(int emeralds, String structure, MapDecoration.Type decoration, String name, int maxUses, int xp) {
            this.emeralds = emeralds;
            this.structure = structure;
            this.decoration = decoration;
            this.name = name;
            this.maxUses = maxUses;
            this.xp = xp;
        }

        @Nullable
        @Override
        public MerchantOffer getOffer(Entity merchant, Random random) {
            World world = merchant.world;
            if (!(world instanceof WorldServer)) return null;
            BlockPos found = world.findNearestStructure(this.structure, new BlockPos(merchant), true);
            if (found == null) return null;
            ItemStack map = ItemMap.setupNewMap(world, found.getX(), found.getZ(), (byte) 2, true, true);
            ItemMap.renderBiomePreviewMap(world, map);
            MapData.addTargetDecoration(map, found, "+", this.decoration);
            map.setTranslatableName(this.name);
            return new MerchantOffer(new ItemStack(Items.EMERALD, this.emeralds), new ItemStack(Items.COMPASS), map, this.maxUses, this.xp, 0.2F);
        }
    }

    private VillagerTrades116() {}
}
