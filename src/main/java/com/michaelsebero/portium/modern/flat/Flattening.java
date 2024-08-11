package com.michaelsebero.portium.modern.flat;

import com.michaelsebero.portium.compat.LegacyBlock;
import com.michaelsebero.portium.compat.LegacyBlockStates;
import com.michaelsebero.portium.compat.LegacyItem;
import com.michaelsebero.portium.compat.LegacyItems;
import com.michaelsebero.portium.modern.mc.util.IItemProvider;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.13's flattened blocks and items, for 1.13 code. 1.13 named every variant ({@code polished_granite}); 1.12 keeps
 * variants in a block's state and an item's metadata ({@code stone[variant=smooth_granite]}, stone item 4). Each 1.13
 * block or item 1.12 has only as a variant gets a view: an unregistered block or item standing for it. 1.13 code reads
 * these from {@code Blocks} and {@code Items}, and gets them back from {@code state.getBlock()} and
 * {@code stack.getItem()} for those variants, so it sees the same objects both ways; what it places or makes from
 * them is the 1.12 variant. A variant that is its block's or item's default stands for itself.
 */
public final class Flattening {

    private static final Map<String, Block> BLOCKS = new ConcurrentHashMap<>();
    private static final Map<String, Item> ITEMS = new ConcurrentHashMap<>();
    private static volatile Map<IBlockState, FlattenedBlock> byState = Collections.emptyMap();
    private static volatile Map<Item, Map<Integer, Item>> byItem = Collections.emptyMap();
    private static List<String> blockNames = Collections.emptyList();
    private static List<String> itemNames = Collections.emptyList();
    private static volatile boolean indexed;

    /** 1.13's block and item names, in its {@code Blocks} and {@code Items}; given as 1.13 mods load. */
    public static void setNames(List<String> blocks, List<String> items) {
        blockNames = blocks;
        itemNames = items;
        indexed = false;
    }

    /** {@code Blocks.X} for a block 1.12's {@code Blocks} lacks, by 1.13 name; of the field's type if it can be. */
    public static Block block(String name, Class<?> type) {
        index();
        Block block = BLOCKS.computeIfAbsent(name, Flattening::resolveBlock);
        if (type.isInstance(block)) return block;
        return block instanceof FlattenedBlock ? ((FlattenedBlock) block).base() : block;
    }

    /** {@code Items.X} for an item 1.12's {@code Items} lacks, by 1.13 name. */
    public static Item item(String name, Class<?> type) {
        index();
        Item item = ITEMS.computeIfAbsent(name, Flattening::resolveItem);
        if (type.isInstance(item)) return item;
        return item instanceof FlattenedItemView ? ((FlattenedItemView) item).base() : item;
    }

    /** 1.13's {@code state.getBlock()}: the view for a flattened variant. */
    public static Block getBlock(IBlockState state) {
        Block block = state.getBlock();
        if (!indexed) index();
        FlattenedBlock view = byState.get(state);
        return view != null ? view : block;
    }

    /** 1.13's {@code stack.getItem()}: the view for a flattened variant. */
    public static Item getItem(ItemStack stack) {
        Item item = stack.getItem();
        if (!indexed) index();
        Map<Integer, Item> variants = byItem.get(item);
        if (variants == null) return item;
        Item view = variants.get(stack.getMetadata());
        return view != null ? view : item;
    }

    /** 1.13's {@code Item.getItemFromBlock}: a view's item view. */
    public static Item itemFromBlock(Block block) {
        if (block instanceof FlattenedBlock) return ((FlattenedBlock) block).asItem();
        return Item.getItemFromBlock(block);
    }

    /** 1.13's {@code Block.getBlockFromItem}. */
    public static Block blockFromItem(Item item) {
        if (item instanceof FlattenedItemBlock) return ((FlattenedItemBlock) item).getBlock();
        return Block.getBlockFromItem(item instanceof FlattenedItemView ? ((FlattenedItemView) item).base() : item);
    }

    /** The 1.12 item a stack made from an item provider holds, and its metadata. */
    public static Item stackItem(@Nullable IItemProvider provider) {
        Item item = provider == null ? Items.AIR : provider.asItem();
        return item instanceof FlattenedItemView ? ((FlattenedItemView) item).base() : item;
    }

    public static int stackMeta(@Nullable IItemProvider provider) {
        Item item = provider == null ? Items.AIR : provider.asItem();
        return item instanceof FlattenedItemView ? ((FlattenedItemView) item).meta() : 0;
    }

    /** Always one: the count of {@code new ItemStack(provider)}. */
    public static int one(@Nullable IItemProvider provider) {
        return 1;
    }

    /** Makes every view, so {@code getBlock} and {@code getItem} know them before any is asked for by name. */
    private static synchronized void index() {
        if (indexed) return;
        Map<IBlockState, FlattenedBlock> states = new IdentityHashMap<>();
        for (String name : blockNames) {
            Block block = BLOCKS.computeIfAbsent(name, Flattening::resolveBlock);
            if (block instanceof FlattenedBlock) states.put(((FlattenedBlock) block).getDefaultState(), (FlattenedBlock) block);
        }
        // Every state of a variant (a variant of stairs or slabs may have more properties) is the view's.
        Map<IBlockState, FlattenedBlock> all = new IdentityHashMap<>(states);
        for (FlattenedBlock view : new ArrayList<>(states.values())) {
            for (IBlockState state : view.base().getBlockState().getValidStates()) {
                if (!all.containsKey(state) && view.matches(state)) all.put(state, view);
            }
        }
        Map<Item, Map<Integer, Item>> items = new IdentityHashMap<>();
        for (String name : itemNames) {
            Item item = ITEMS.computeIfAbsent(name, Flattening::resolveItem);
            if (item instanceof FlattenedItemView) {
                FlattenedItemView view = (FlattenedItemView) item;
                items.computeIfAbsent(view.base(), key -> new HashMap<>()).put(view.meta(), item);
            }
        }
        byState = all;
        byItem = items;
        indexed = true;
    }

    private static Block resolveBlock(String name) {
        String id = new ResourceLocation(name).toString();
        LegacyBlock legacy = LegacyBlockStates.resolve(id);
        if (legacy == null) return Blocks.AIR;
        if (legacy.getVariant().isEmpty()) return legacy.getBlock();
        IBlockState state = legacy.getBlock().getDefaultState();
        for (Map.Entry<IProperty<?>, Comparable<?>> entry : legacy.getVariant().entrySet()) state = with(state, entry.getKey(), entry.getValue());
        if (state == legacy.getBlock().getDefaultState()) return legacy.getBlock();
        return new FlattenedBlock(id, state, legacy);
    }

    private static Item resolveItem(String name) {
        String id = new ResourceLocation(name).toString();
        LegacyItem legacy = LegacyItems.resolve(id);
        if (legacy == null) return Items.AIR;
        int meta = legacy.getMeta() == LegacyItem.ANY_META ? 0 : legacy.getMeta();
        if (meta == 0) return legacy.getItem();
        Block block = BLOCKS.get(id);
        if (legacy.getItem() instanceof ItemBlock && block instanceof FlattenedBlock) {
            return new FlattenedItemBlock(id, (FlattenedBlock) block, (ItemBlock) legacy.getItem(), meta);
        }
        return new FlattenedItem(id, legacy.getItem(), meta);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static IBlockState with(IBlockState state, IProperty property, Comparable value) {
        return state.withProperty(property, value);
    }

    private Flattening() {}
}
