package com.michaelsebero.portium.init;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.item.ItemDebugStick;
import com.michaelsebero.portium.entity.fish.EntityCod;
import com.michaelsebero.portium.entity.fish.EntityPufferfish;
import com.michaelsebero.portium.entity.fish.EntitySalmon;
import com.michaelsebero.portium.entity.fish.EntityTropicalFish;
import com.michaelsebero.portium.item.ItemDriedKelp;
import com.michaelsebero.portium.item.ItemFishBucket;
import com.michaelsebero.portium.item.ItemSimple;
import com.michaelsebero.portium.item.ItemTrident;
import com.michaelsebero.portium.item.ItemTurtleHelmet;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Portium's items. Each is registered only when {@link PortiumConfig#items} (or, for a block's item, its block) is
 * on; the field of one that's off stays null.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PortiumItems {

    public static Item KELP;
    public static Item DRIED_KELP;
    public static Item HEART_OF_THE_SEA;
    public static Item NAUTILUS_SHELL;
    public static Item PHANTOM_MEMBRANE;
    public static Item SCUTE;
    public static Item TURTLE_HELMET;
    public static Item TRIDENT;
    public static Item DEBUG_STICK;
    public static Item COD_BUCKET;
    public static Item SALMON_BUCKET;
    public static Item PUFFERFISH_BUCKET;
    public static Item TROPICAL_FISH_BUCKET;
    public static Item NETHERITE_SCRAP;
    public static Item NETHERITE_INGOT;
    public static Item NETHERITE_SWORD;
    public static Item NETHERITE_PICKAXE;
    public static Item NETHERITE_AXE;
    public static Item NETHERITE_SHOVEL;
    public static Item NETHERITE_HOE;
    public static Item NETHERITE_HELMET;
    public static Item NETHERITE_CHESTPLATE;
    public static Item NETHERITE_LEGGINGS;
    public static Item NETHERITE_BOOTS;

    public static Item WHITE_DYE;
    public static Item BLUE_DYE;
    public static Item BROWN_DYE;
    public static Item BLACK_DYE;
    public static Item SWEET_BERRIES;
    public static Item CROSSBOW;
    public static Item WARPED_FUNGUS_ON_A_STICK;
    public static Item MUSIC_DISC_PIGSTEP;
    public static Item LEATHER_HORSE_ARMOR;
    public static Item SUSPICIOUS_STEW;
    public static Item HONEY_BOTTLE;
    public static Item HONEYCOMB;

    private static final List<Item> ALL = new ArrayList<>();

    public static List<Item> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static boolean registered;

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        registerItems(event.getRegistry());
    }

    /** Registers Portium's items, once: at construction, after its blocks (see {@link PortiumBlocks#registerBlocks}). */
    public static synchronized void registerItems(IForgeRegistry<Item> registry) {
        if (registered) return;
        registered = true;

        for (Map.Entry<Block, Function<Block, Item>> entry : PortiumBlocks.ITEM_BLOCKS.entrySet()) {
            Block block = entry.getKey();
            Item item = entry.getValue().apply(block);
            item.setRegistryName(block.getRegistryName());
            registry.register(item);
            PortiumNames.alias(registry, item.getRegistryName());
            ALL.add(item);
        }
        PortiumConfig.Items enabled = PortiumConfig.items;
        PortiumConfig.Mobs mobs = PortiumConfig.mobs;
        if (PortiumBlocks.KELP != null) {
            KELP = Item.getItemFromBlock(PortiumBlocks.KELP);
            DRIED_KELP = register(registry, "dried_kelp", new ItemDriedKelp());
        }
        if (enabled.heartOfTheSea) {
            HEART_OF_THE_SEA = register(registry, "heart_of_the_sea", new ItemSimple(EnumRarity.UNCOMMON).setCreativeTab(CreativeTabs.MATERIALS));
        }
        if (enabled.nautilusShell) {
            NAUTILUS_SHELL = register(registry, "nautilus_shell", new ItemSimple(EnumRarity.UNCOMMON).setCreativeTab(CreativeTabs.MATERIALS));
        }
        if (enabled.phantomMembrane) {
            PHANTOM_MEMBRANE = register(registry, "phantom_membrane", new ItemSimple(EnumRarity.COMMON).setCreativeTab(CreativeTabs.BREWING));
        }
        if (enabled.turtleShell) {
            SCUTE = register(registry, "scute", new ItemSimple(EnumRarity.COMMON).setCreativeTab(CreativeTabs.MATERIALS));
            TURTLE_HELMET = register(registry, "turtle_helmet", new ItemTurtleHelmet());
        }
        if (enabled.trident) TRIDENT = register(registry, "trident", new ItemTrident());
        if (enabled.netherite && PortiumConfig.blocks.ancientDebris) {
            NETHERITE_SCRAP = register(registry, "netherite_scrap", new com.michaelsebero.portium.item.ItemFireproof(CreativeTabs.MATERIALS));
            NETHERITE_INGOT = register(registry, "netherite_ingot", new com.michaelsebero.portium.item.ItemFireproof(CreativeTabs.MATERIALS));
            com.michaelsebero.portium.item.netherite.NetheriteMaterials.setRepairItem(NETHERITE_INGOT);
            NETHERITE_SWORD = register(registry, "netherite_sword", new com.michaelsebero.portium.item.netherite.NetheriteTools.Sword());
            NETHERITE_PICKAXE = register(registry, "netherite_pickaxe", new com.michaelsebero.portium.item.netherite.NetheriteTools.Pickaxe());
            NETHERITE_AXE = register(registry, "netherite_axe", new com.michaelsebero.portium.item.netherite.NetheriteTools.Axe());
            NETHERITE_SHOVEL = register(registry, "netherite_shovel", new com.michaelsebero.portium.item.netherite.NetheriteTools.Shovel());
            NETHERITE_HOE = register(registry, "netherite_hoe", new com.michaelsebero.portium.item.netherite.NetheriteTools.Hoe());
            NETHERITE_HELMET = register(registry, "netherite_helmet", new com.michaelsebero.portium.item.netherite.ItemNetheriteArmor(EntityEquipmentSlot.HEAD));
            NETHERITE_CHESTPLATE = register(registry, "netherite_chestplate",
                    new com.michaelsebero.portium.item.netherite.ItemNetheriteArmor(EntityEquipmentSlot.CHEST));
            NETHERITE_LEGGINGS = register(registry, "netherite_leggings", new com.michaelsebero.portium.item.netherite.ItemNetheriteArmor(EntityEquipmentSlot.LEGS));
            NETHERITE_BOOTS = register(registry, "netherite_boots", new com.michaelsebero.portium.item.netherite.ItemNetheriteArmor(EntityEquipmentSlot.FEET));
        }
        if (enabled.debugStick) DEBUG_STICK = register(registry, "debug_stick", new ItemDebugStick());
        if (enabled.fishBuckets) {
            if (mobs.cod) COD_BUCKET = register(registry, "cod_bucket", new ItemFishBucket(EntityCod::new));
            if (mobs.salmon) SALMON_BUCKET = register(registry, "salmon_bucket", new ItemFishBucket(EntitySalmon::new));
            if (mobs.pufferfish) PUFFERFISH_BUCKET = register(registry, "pufferfish_bucket", new ItemFishBucket(EntityPufferfish::new));
            if (mobs.tropicalFish) TROPICAL_FISH_BUCKET = register(registry, "tropical_fish_bucket", new ItemFishBucket(EntityTropicalFish::new));
        }

        if (enabled.dyes114) {
            WHITE_DYE = register(registry, "white_dye", new com.michaelsebero.portium.item.ItemDye114(net.minecraft.item.EnumDyeColor.WHITE));
            BLUE_DYE = register(registry, "blue_dye", new com.michaelsebero.portium.item.ItemDye114(net.minecraft.item.EnumDyeColor.BLUE));
            BROWN_DYE = register(registry, "brown_dye", new com.michaelsebero.portium.item.ItemDye114(net.minecraft.item.EnumDyeColor.BROWN));
            BLACK_DYE = register(registry, "black_dye", new com.michaelsebero.portium.item.ItemDye114(net.minecraft.item.EnumDyeColor.BLACK));
        }
        if (PortiumBlocks.SWEET_BERRY_BUSH != null) SWEET_BERRIES = register(registry, "sweet_berries", new com.michaelsebero.portium.item.ItemSweetBerries());
        if (com.michaelsebero.portium.PortiumConfig.items.crossbow) CROSSBOW = register(registry, "crossbow", new com.michaelsebero.portium.item.ItemCrossbow());
        if (com.michaelsebero.portium.PortiumConfig.items.leatherHorseArmor) {
            LEATHER_HORSE_ARMOR = register(registry, "leather_horse_armor", new com.michaelsebero.portium.item.ItemLeatherHorseArmor());
        }
        if (com.michaelsebero.portium.PortiumConfig.items.suspiciousStew) {
            SUSPICIOUS_STEW = register(registry, "suspicious_stew", new com.michaelsebero.portium.item.ItemSuspiciousStew());
        }
        if (com.michaelsebero.portium.PortiumConfig.items.pigstep) {
            MUSIC_DISC_PIGSTEP = register(registry, "music_disc_pigstep", new com.michaelsebero.portium.item.ItemMusicDisc116("pigstep", PortiumSounds.MUSIC_DISC_PIGSTEP));
        }
        if (PortiumBlocks.WARPED_FUNGUS != null && com.michaelsebero.portium.PortiumConfig.mobs.striders) {
            WARPED_FUNGUS_ON_A_STICK = register(registry, "warped_fungus_on_a_stick", new com.michaelsebero.portium.item.ItemWarpedFungusOnAStick());
        }
        if (PortiumBlocks.HONEY_BLOCK != null) {
            HONEY_BOTTLE = register(registry, "honey_bottle", new com.michaelsebero.portium.item.ItemHoneyBottle());
            HONEYCOMB = register(registry, "honeycomb", new Item().setCreativeTab(net.minecraft.creativetab.CreativeTabs.MATERIALS));
        }
        if (PortiumBlocks.LOOM != null) {
            // 1.14's banner patterns, used in the loom.
            String[][] patterns = {{"flower", "flo", "COMMON"}, {"creeper", "cre", "UNCOMMON"}, {"skull", "sku", "UNCOMMON"}, {"mojang", "moj", "EPIC"},
                    {"globe", "glb", "COMMON"}, {"piglin", "pig", "COMMON"}};
            for (String[] pattern : patterns) {
                register(registry, pattern[0] + "_banner_pattern", new com.michaelsebero.portium.item.ItemBannerPattern(pattern[1], EnumRarity.valueOf(pattern[2])));
            }
        }

        PortiumRecipes.registerOreDictionary();
    }

    private static <T extends Item> T register(IForgeRegistry<Item> registry, String name, T item) {
        item.setRegistryName(PortiumNames.of(registry, name));
        item.setTranslationKey(Tags.MOD_ID + "." + name);
        registry.register(item);
        PortiumNames.alias(registry, item.getRegistryName());
        ALL.add(item);
        return item;
    }

    private PortiumItems() {}
}
