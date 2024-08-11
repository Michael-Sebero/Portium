package com.michaelsebero.portium.init;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.LootFunctionManager;
import com.michaelsebero.portium.loot.ExplorationMap;
import com.michaelsebero.portium.loot.SetName;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Loot tables for Portium's mobs, and 1.13's changes to fishing. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PortiumLootTables {

    public static final ResourceLocation COD = register("entities/cod");
    public static final ResourceLocation SALMON = register("entities/salmon");
    public static final ResourceLocation PUFFERFISH = register("entities/pufferfish");
    public static final ResourceLocation TROPICAL_FISH = register("entities/tropical_fish");
    public static final ResourceLocation DOLPHIN = register("entities/dolphin");
    public static final ResourceLocation TURTLE = register("entities/turtle");
    public static final ResourceLocation DROWNED = register("entities/drowned");
    public static final ResourceLocation PHANTOM = register("entities/phantom");

    public static final ResourceLocation SHIPWRECK_MAP = register("chests/shipwreck_map");
    public static final ResourceLocation SHIPWRECK_SUPPLY = register("chests/shipwreck_supply");
    public static final ResourceLocation SHIPWRECK_TREASURE = register("chests/shipwreck_treasure");
    public static final ResourceLocation UNDERWATER_RUIN_SMALL = register("chests/underwater_ruin_small");
    public static final ResourceLocation UNDERWATER_RUIN_BIG = register("chests/underwater_ruin_big");
    public static final ResourceLocation BURIED_TREASURE = register("chests/buried_treasure");

    private static final ResourceLocation FISHING_TREASURE = new ResourceLocation("gameplay/fishing/treasure");
    private static final ResourceLocation FISHING_JUNK = new ResourceLocation("gameplay/fishing/junk");

    private static ResourceLocation register(String path) {
        return LootTableList.register(new ResourceLocation(Tags.MOD_ID, path));
    }

    /** Registers the tables and Portium's loot functions. */
    public static void init() {
        LootFunctionManager.registerFunction(new SetName.Serializer());
        LootFunctionManager.registerFunction(new ExplorationMap.Serializer());
    }

    /** 1.13 fishing: nautilus shells are treasure, and lily pads become junk. */
    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (!PortiumConfig.mechanics.newFishingLoot) return;
        Item lilyPad = Item.getItemFromBlock(Blocks.WATERLILY);
        if (event.getName().equals(FISHING_TREASURE)) {
            LootPool pool = event.getTable().getPool("main");
            if (pool == null) return;
            pool.removeEntry("minecraft:waterlily");
            if (PortiumItems.NAUTILUS_SHELL == null) return;
            pool.addEntry(new LootEntryItem(PortiumItems.NAUTILUS_SHELL, 1, 0, new LootFunction[0], new LootCondition[0], Tags.MOD_ID + ":nautilus_shell"));
        } else if (event.getName().equals(FISHING_JUNK)) {
            LootPool pool = event.getTable().getPool("main");
            if (pool == null || pool.getEntry("minecraft:waterlily") != null) return;
            pool.addEntry(new LootEntryItem(lilyPad, 17, 0, new LootFunction[0], new LootCondition[0], "minecraft:waterlily"));
        }
    }

    private PortiumLootTables() {}
}
