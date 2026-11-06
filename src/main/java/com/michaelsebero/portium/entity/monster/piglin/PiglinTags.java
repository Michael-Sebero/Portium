package com.michaelsebero.portium.entity.monster.piglin;

import com.michaelsebero.portium.compat.LegacyBlock;
import com.michaelsebero.portium.compat.LegacyBlockStates;
import com.michaelsebero.portium.compat.LegacyItem;
import com.michaelsebero.portium.compat.LegacyItems;
import com.michaelsebero.portium.datapack.DataPackManager;
import com.michaelsebero.portium.datapack.Tag;
import com.michaelsebero.portium.block.light.BlockCampfire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The 1.16 tags piglins and hoglins go by: {@code piglin_loved} and {@code piglin_repellents} items, and the
 * {@code piglin_repellents}, {@code hoglin_repellents} and {@code guarded_by_piglins} blocks. Read from data packs (the
 * vanilla pack fetches 1.16.5's); without them (a client), the lists 1.16.5 ships.
 */
public final class PiglinTags {

    public static final ResourceLocation PIGLIN_LOVED = new ResourceLocation("piglin_loved");
    public static final ResourceLocation PIGLIN_REPELLENTS = new ResourceLocation("piglin_repellents");
    public static final ResourceLocation HOGLIN_REPELLENTS = new ResourceLocation("hoglin_repellents");
    public static final ResourceLocation GUARDED_BY_PIGLINS = new ResourceLocation("guarded_by_piglins");

    private static final String[] GOLD_ORES = {"minecraft:gold_ore", "minecraft:nether_gold_ore"};
    private static final String[] LOVED = {"minecraft:gold_block", "minecraft:gilded_blackstone", "minecraft:light_weighted_pressure_plate",
            "minecraft:gold_ingot", "minecraft:bell", "minecraft:clock", "minecraft:golden_carrot", "minecraft:glistering_melon_slice",
            "minecraft:golden_apple", "minecraft:enchanted_golden_apple", "minecraft:golden_helmet", "minecraft:golden_chestplate",
            "minecraft:golden_leggings", "minecraft:golden_boots", "minecraft:golden_horse_armor", "minecraft:golden_sword",
            "minecraft:golden_pickaxe", "minecraft:golden_shovel", "minecraft:golden_axe", "minecraft:golden_hoe"};
    private static final String[] REPELLENT_ITEMS = {"minecraft:soul_torch", "minecraft:soul_lantern", "minecraft:soul_campfire"};
    private static final String[] PIGLIN_REPELLENT_BLOCKS = {"minecraft:soul_fire", "minecraft:soul_torch", "minecraft:soul_lantern",
            "minecraft:soul_wall_torch", "minecraft:soul_campfire"};
    private static final String[] HOGLIN_REPELLENT_BLOCKS = {"minecraft:warped_fungus", "minecraft:potted_warped_fungus", "minecraft:nether_portal",
            "minecraft:respawn_anchor"};
    private static final String[] GUARDED_BLOCKS = {"minecraft:gold_block", "minecraft:barrel", "minecraft:chest", "minecraft:ender_chest",
            "minecraft:gilded_blackstone", "minecraft:trapped_chest", "minecraft:white_shulker_box", "minecraft:orange_shulker_box",
            "minecraft:magenta_shulker_box", "minecraft:light_blue_shulker_box", "minecraft:yellow_shulker_box", "minecraft:lime_shulker_box",
            "minecraft:pink_shulker_box", "minecraft:gray_shulker_box", "minecraft:light_gray_shulker_box", "minecraft:cyan_shulker_box",
            "minecraft:purple_shulker_box", "minecraft:blue_shulker_box", "minecraft:brown_shulker_box", "minecraft:green_shulker_box",
            "minecraft:red_shulker_box", "minecraft:black_shulker_box", "minecraft:shulker_box"};

    private static List<LegacyItem> lovedFallback;
    private static List<LegacyItem> repellentItemsFallback;
    private static List<LegacyBlock> piglinRepellentFallback;
    private static List<LegacyBlock> hoglinRepellentFallback;
    private static List<LegacyBlock> guardedFallback;

    public static boolean isLoved(ItemStack stack) {
        return !stack.isEmpty() && itemTag(PIGLIN_LOVED, () -> lovedFallback == null ? lovedFallback = items(Stream.concat(Stream.of(LOVED), Stream.of(GOLD_ORES))) : lovedFallback, stack);
    }

    public static boolean isRepellentItem(ItemStack stack) {
        return !stack.isEmpty() && itemTag(PIGLIN_REPELLENTS, () -> repellentItemsFallback == null ? repellentItemsFallback = items(Stream.of(REPELLENT_ITEMS)) : repellentItemsFallback, stack);
    }

    /** Soul fire and its lights; a soul campfire only while lit. */
    public static boolean isPiglinRepellent(IBlockState state) {
        boolean tagged = blockTag(PIGLIN_REPELLENTS, () -> piglinRepellentFallback == null ? piglinRepellentFallback = blocks(Stream.of(PIGLIN_REPELLENT_BLOCKS)) : piglinRepellentFallback, state);
        if (tagged && state.getBlock() instanceof BlockCampfire && state.getPropertyKeys().contains(BlockCampfire.LIT)) return state.getValue(BlockCampfire.LIT);
        return tagged;
    }

    public static boolean isHoglinRepellent(IBlockState state) {
        return blockTag(HOGLIN_REPELLENTS, () -> hoglinRepellentFallback == null ? hoglinRepellentFallback = blocks(Stream.of(HOGLIN_REPELLENT_BLOCKS)) : hoglinRepellentFallback, state);
    }

    public static boolean isGuardedByPiglins(IBlockState state) {
        return blockTag(GUARDED_BY_PIGLINS, () -> guardedFallback == null ? guardedFallback = blocks(Stream.concat(Stream.of(GUARDED_BLOCKS), Stream.of(GOLD_ORES))) : guardedFallback, state);
    }

    private static boolean itemTag(ResourceLocation id, java.util.function.Supplier<List<LegacyItem>> fallback, ItemStack stack) {
        DataPackManager packs = DataPackManager.get();
        Tag<LegacyItem> tag = packs == null ? null : packs.getTags().getItems().get(id);
        if (tag != null) return tag.anyMatch(item -> item.test(stack));
        return fallback.get().stream().anyMatch(item -> item.test(stack));
    }

    private static boolean blockTag(ResourceLocation id, java.util.function.Supplier<List<LegacyBlock>> fallback, IBlockState state) {
        DataPackManager packs = DataPackManager.get();
        Tag<LegacyBlock> tag = packs == null ? null : packs.getTags().getBlocks().get(id);
        if (tag != null) return tag.anyMatch(block -> block.test(state));
        return fallback.get().stream().anyMatch(block -> block.test(state));
    }

    private static List<LegacyItem> items(Stream<String> names) {
        return names.map(PiglinTags::item).filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<LegacyBlock> blocks(Stream<String> names) {
        return names.map(PiglinTags::block).filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
    }

    private static LegacyItem item(String name) {
        try {
            return LegacyItems.resolve(name);
        } catch (RuntimeException missing) {
            return null;
        }
    }

    private static LegacyBlock block(String name) {
        try {
            return LegacyBlockStates.resolve(name);
        } catch (RuntimeException missing) {
            return null;
        }
    }

    private PiglinTags() {}
}
