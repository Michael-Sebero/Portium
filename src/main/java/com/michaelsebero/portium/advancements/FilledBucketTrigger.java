package com.michaelsebero.portium.advancements;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** {@code minecraft:filled_bucket} (1.13): the player filled a bucket with water or lava, or scooped up a fish. */
public class FilledBucketTrigger extends PortiumTrigger<FilledBucketTrigger.Instance> {

    public FilledBucketTrigger() {
        super(new ResourceLocation("filled_bucket"));
    }

    @Override
    public Instance deserializeInstance(JsonObject json, JsonDeserializationContext context) {
        return new Instance(this.getId(), ItemPredicate.deserialize(json.get("item")));
    }

    public void trigger(EntityPlayerMP player, ItemStack filled) {
        this.trigger(player, instance -> instance.item.test(filled));
    }

    public static class Instance extends AbstractCriterionInstance {

        private final ItemPredicate item;

        Instance(ResourceLocation id, ItemPredicate item) {
            super(id);
            this.item = item;
        }
    }
}
