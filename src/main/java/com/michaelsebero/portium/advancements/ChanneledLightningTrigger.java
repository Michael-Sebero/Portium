package com.michaelsebero.portium.advancements;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;

/** {@code minecraft:channeled_lightning} (1.13): lightning the player called down with a Channeling trident struck these entities. */
public class ChanneledLightningTrigger extends PortiumTrigger<ChanneledLightningTrigger.Instance> {

    public ChanneledLightningTrigger() {
        super(new ResourceLocation("channeled_lightning"));
    }

    @Override
    public Instance deserializeInstance(JsonObject json, JsonDeserializationContext context) {
        JsonArray list = JsonUtils.getJsonArray(json, "victims", new JsonArray());
        EntityPredicate[] victims = new EntityPredicate[list.size()];
        for (int i = 0; i < victims.length; i++) victims[i] = EntityPredicate.deserialize(list.get(i));
        return new Instance(this.getId(), victims);
    }

    public void trigger(EntityPlayerMP player, Collection<? extends Entity> struck) {
        this.trigger(player, instance -> instance.matches(player, struck));
    }

    public static class Instance extends AbstractCriterionInstance {

        private final EntityPredicate[] victims;

        Instance(ResourceLocation id, EntityPredicate[] victims) {
            super(id);
            this.victims = victims;
        }

        /** Every victim predicate is met by some struck entity. */
        boolean matches(EntityPlayerMP player, Collection<? extends Entity> struck) {
            for (EntityPredicate victim : this.victims) {
                boolean found = false;
                for (Entity entity : struck) {
                    if (victim.test(player, entity)) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
            return true;
        }
    }
}
