package com.michaelsebero.portium.advancements;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.compat.LegacyItem;
import com.michaelsebero.portium.datapack.DataPackManager;
import com.michaelsebero.portium.datapack.Tag;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.advancements.critereon.ItemPredicates;

import java.util.Map;

/**
 * 1.13 item predicates may name an item tag instead of an item. Data pack advancements are rewritten to use this
 * as {@code {"type": "portium:tag", "tag": ...}}; the predicate's other conditions still apply.
 */
public class TagItemPredicate extends ItemPredicate {

    private final ResourceLocation tag;
    private final ItemPredicate others;

    private TagItemPredicate(ResourceLocation tag, ItemPredicate others) {
        this.tag = tag;
        this.others = others;
    }

    static void register() {
        ItemPredicates.register(new ResourceLocation(Tags.MOD_ID, "tag"), json -> {
            JsonObject others = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                if (!entry.getKey().equals("type") && !entry.getKey().equals("tag")) others.add(entry.getKey(), entry.getValue());
            }
            return new TagItemPredicate(new ResourceLocation(JsonUtils.getString(json, "tag")), ItemPredicate.deserialize(others));
        });
    }

    @Override
    public boolean test(ItemStack stack) {
        DataPackManager packs = DataPackManager.get();
        Tag<LegacyItem> items = packs == null ? null : packs.getTags().getItems().get(this.tag);
        return items != null && items.anyMatch(item -> item.test(stack)) && this.others.test(stack);
    }
}
