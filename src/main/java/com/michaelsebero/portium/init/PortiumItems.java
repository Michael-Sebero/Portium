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

    private static final List<Item> ALL = new ArrayList<>();

    public static List<Item> all() {
        return Collections.unmodifiableList(ALL);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();

        for (Map.Entry<Block, Function<Block, Item>> entry : PortiumBlocks.ITEM_BLOCKS.entrySet()) {
            Block block = entry.getKey();
            Item item = entry.getValue().apply(block);
            item.setRegistryName(block.getRegistryName());
            registry.register(item);
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
        if (enabled.debugStick) DEBUG_STICK = register(registry, "debug_stick", new ItemDebugStick());
        if (enabled.fishBuckets) {
            if (mobs.cod) COD_BUCKET = register(registry, "cod_bucket", new ItemFishBucket(EntityCod::new));
            if (mobs.salmon) SALMON_BUCKET = register(registry, "salmon_bucket", new ItemFishBucket(EntitySalmon::new));
            if (mobs.pufferfish) PUFFERFISH_BUCKET = register(registry, "pufferfish_bucket", new ItemFishBucket(EntityPufferfish::new));
            if (mobs.tropicalFish) TROPICAL_FISH_BUCKET = register(registry, "tropical_fish_bucket", new ItemFishBucket(EntityTropicalFish::new));
        }

        PortiumRecipes.registerOreDictionary();
    }

    private static <T extends Item> T register(IForgeRegistry<Item> registry, String name, T item) {
        item.setRegistryName(new ResourceLocation(Tags.MOD_ID, name));
        item.setTranslationKey(Tags.MOD_ID + "." + name);
        registry.register(item);
        ALL.add(item);
        return item;
    }

    private PortiumItems() {}
}
