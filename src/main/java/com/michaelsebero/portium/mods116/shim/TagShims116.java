package com.michaelsebero.portium.mods116.shim;

import com.michaelsebero.portium.compat.LegacyBlock;
import com.michaelsebero.portium.compat.LegacyItem;
import com.michaelsebero.portium.datapack.DataPackManager;
import com.michaelsebero.portium.datapack.Tag;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What 1.16 mods' tags hold, from the data packs Portium loads for the running world: blocks, items and fluids (the
 * fluid's block, in 1.12). Empty while no world runs.
 */
public final class TagShims116 {

    public static boolean contains(String kind, ResourceLocation id, Object value) {
        DataPackManager manager = DataPackManager.get();
        if (manager == null || value == null) return false;
        switch (kind) {
            case "blocks": {
                Tag<LegacyBlock> tag = manager.getTags().getBlocks().get(id);
                if (tag == null) return false;
                for (LegacyBlock block : tag.getValues()) if (block.getBlock() == value) return true;
                return false;
            }
            case "items": {
                Tag<LegacyItem> tag = manager.getTags().getItems().get(id);
                if (tag == null) return false;
                for (LegacyItem item : tag.getValues()) if (item.getItem() == value) return true;
                return false;
            }
            case "fluids": {
                Tag<Block> tag = manager.getTags().getFluids().get(id);
                if (tag == null) return false;
                // 1.16 asks with a fluid; 1.12's fluid tags hold the fluids' blocks.
                if (value instanceof com.michaelsebero.portium.modern.mc.fluid.Fluid) {
                    net.minecraft.block.state.IBlockState state = ((com.michaelsebero.portium.modern.mc.fluid.Fluid) value).getDefaultState().getBlockState();
                    value = state == null ? null : state.getBlock();
                }
                return value != null && tag.getValues().contains(value);
            }
            case "entity_types": {
                Tag<ResourceLocation> tag = manager.getTags().getEntityTypes().get(id);
                if (tag == null) return false;
                ResourceLocation name = value instanceof com.michaelsebero.portium.modern.mc.entity.EntityType
                        ? net.minecraftforge.fml.common.registry.EntityRegistry.getEntry(((com.michaelsebero.portium.modern.mc.entity.EntityType<?>) value).getEntityClass()) == null ? null
                        : net.minecraftforge.fml.common.registry.EntityRegistry.getEntry(((com.michaelsebero.portium.modern.mc.entity.EntityType<?>) value).getEntityClass()).getRegistryName()
                        : null;
                return name != null && tag.getValues().contains(name);
            }
            default:
                return false;
        }
    }

    public static List<Object> values(String kind, ResourceLocation id) {
        DataPackManager manager = DataPackManager.get();
        if (manager == null) return Collections.emptyList();
        List<Object> values = new ArrayList<>();
        switch (kind) {
            case "blocks": {
                Tag<LegacyBlock> tag = manager.getTags().getBlocks().get(id);
                if (tag != null) for (LegacyBlock block : tag.getValues()) if (!values.contains(block.getBlock())) values.add(block.getBlock());
                break;
            }
            case "items": {
                Tag<LegacyItem> tag = manager.getTags().getItems().get(id);
                if (tag != null) for (LegacyItem item : tag.getValues()) {
                    Item value = item.getItem();
                    if (!values.contains(value)) values.add(value);
                }
                break;
            }
            case "fluids": {
                Tag<Block> tag = manager.getTags().getFluids().get(id);
                if (tag != null) values.addAll(tag.getValues());
                break;
            }
            default:
                break;
        }
        return values;
    }

    /** Forge 1.16's {@code block.getTags()}: the names of the block tags holding the block. */
    public static java.util.Set<ResourceLocation> blockTags(Block block) {
        DataPackManager manager = DataPackManager.get();
        if (manager == null) return Collections.emptySet();
        java.util.Set<ResourceLocation> names = new java.util.HashSet<>();
        for (ResourceLocation id : manager.getTags().getBlocks().getIds()) {
            Tag<LegacyBlock> tag = manager.getTags().getBlocks().get(id);
            if (tag == null) continue;
            for (LegacyBlock value : tag.getValues()) {
                if (value.getBlock() == block) {
                    names.add(id);
                    break;
                }
            }
        }
        return Collections.unmodifiableSet(names);
    }

    /** Forge 1.16's {@code item.getTags()}: the names of the item tags holding the item. */
    public static java.util.Set<ResourceLocation> itemTags(Item item) {
        DataPackManager manager = DataPackManager.get();
        if (manager == null) return Collections.emptySet();
        java.util.Set<ResourceLocation> names = new java.util.HashSet<>();
        for (ResourceLocation id : manager.getTags().getItems().getIds()) {
            Tag<LegacyItem> tag = manager.getTags().getItems().get(id);
            if (tag == null) continue;
            for (LegacyItem value : tag.getValues()) {
                if (value.getItem() == item) {
                    names.add(id);
                    break;
                }
            }
        }
        return Collections.unmodifiableSet(names);
    }

    /** An item tag's stacks for an ingredient: a wildcard where the tag takes an item of any metadata. */
    public static List<Object> itemStacks(ResourceLocation id) {
        DataPackManager manager = DataPackManager.get();
        if (manager == null) return Collections.emptyList();
        Tag<LegacyItem> tag = manager.getTags().getItems().get(id);
        if (tag == null) return Collections.emptyList();
        List<Object> stacks = new ArrayList<>();
        for (LegacyItem item : tag.getValues()) stacks.add(stack(item));
        return stacks;
    }

    /** Whether an item tag of this name is loaded (1.16 refuses an ingredient of an unknown tag). */
    public static boolean hasItemTag(ResourceLocation id) {
        DataPackManager manager = DataPackManager.get();
        return manager == null || manager.getTags().getItems().get(id) != null;
    }

    /** The stack for a 1.16 item name (a flattened name gives 1.12's item and metadata), or null if there's none. */
    @javax.annotation.Nullable
    public static Object itemStack(String name) {
        LegacyItem item = com.michaelsebero.portium.compat.LegacyItems.resolve(name);
        return item == null || item.getItem() == net.minecraft.init.Items.AIR ? null : stack(item);
    }

    private static net.minecraft.item.ItemStack stack(LegacyItem item) {
        net.minecraft.item.ItemStack stack = item.createStack(1);
        if (item.getMeta() == LegacyItem.ANY_META) stack.setItemDamage(net.minecraftforge.oredict.OreDictionary.WILDCARD_VALUE);
        return stack;
    }

    private TagShims116() {}
}
