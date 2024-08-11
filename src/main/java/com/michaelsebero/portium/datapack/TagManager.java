package com.michaelsebero.portium.datapack;

import com.michaelsebero.portium.command.arguments.selector.EntityTypes;
import com.michaelsebero.portium.compat.LegacyBlock;
import com.michaelsebero.portium.compat.LegacyBlockStates;
import com.michaelsebero.portium.compat.LegacyItem;
import com.michaelsebero.portium.compat.LegacyItems;
import com.michaelsebero.portium.modern.shim.FluidShims;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

import java.util.List;

/** All tag types data packs can define: blocks, items, fluids, functions and (1.14) entity types. */
public final class TagManager {

    private final TagCollection<LegacyBlock> blocks = new TagCollection<>("blocks", id -> LegacyBlockStates.resolve(id.toString()));
    private final TagCollection<LegacyItem> items = new TagCollection<>("items", id -> LegacyItems.resolve(id.toString()));
    private final TagCollection<Block> fluids = new TagCollection<>("fluids", TagManager::fluid);
    private final TagCollection<ResourceLocation> functions = new TagCollection<>("functions", id -> id);
    private final TagCollection<ResourceLocation> entityTypes = new TagCollection<>("entity_types", EntityTypes::toLegacy);

    /**
     * 1.13 fluids are {@code minecraft:water}/{@code flowing_water} and the lava pair; 1.12 has a block for each.
     * Fluids 1.13 mods register are their blocks.
     */
    private static Block fluid(ResourceLocation id) {
        switch (id.toString()) {
            case "minecraft:water": return Blocks.WATER;
            case "minecraft:flowing_water": return Blocks.FLOWING_WATER;
            case "minecraft:lava": return Blocks.LAVA;
            case "minecraft:flowing_lava": return Blocks.FLOWING_LAVA;
            default: return FluidShims.fluidBlock(id);
        }
    }

    public void load(List<DataPack> packs) {
        this.blocks.load(packs);
        this.items.load(packs);
        this.fluids.load(packs);
        this.functions.load(packs);
        this.entityTypes.load(packs);
    }

    public TagCollection<LegacyBlock> getBlocks() {
        return this.blocks;
    }

    public TagCollection<LegacyItem> getItems() {
        return this.items;
    }

    public TagCollection<Block> getFluids() {
        return this.fluids;
    }

    public TagCollection<ResourceLocation> getFunctions() {
        return this.functions;
    }

    public TagCollection<ResourceLocation> getEntityTypes() {
        return this.entityTypes;
    }
}
