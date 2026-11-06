package com.michaelsebero.portium.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.michaelsebero.portium.item.ItemSuspiciousStew;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 1.14's {@code set_stew_effect}: one of the listed effects on a suspicious stew, for a random number of seconds
 * (ticks, for an instant effect such as saturation), as shipwreck supply chests give them.
 */
public class SetStewEffect extends LootFunction {

    private final List<Potion> effects;
    private final List<RandomValueRange> durations;

    SetStewEffect(LootCondition[] conditions, List<Potion> effects, List<RandomValueRange> durations) {
        super(conditions);
        this.effects = effects;
        this.durations = durations;
    }

    @Override
    public ItemStack apply(ItemStack stack, Random random, LootContext context) {
        if (!(stack.getItem() instanceof ItemSuspiciousStew) || this.effects.isEmpty()) return stack;
        int index = random.nextInt(this.effects.size());
        Potion effect = this.effects.get(index);
        int duration = this.durations.get(index).generateInt(random);
        if (!effect.isInstant()) duration *= 20;
        ItemSuspiciousStew.addEffect(stack, effect, duration);
        return stack;
    }

    public static class Serializer extends LootFunction.Serializer<SetStewEffect> {

        public Serializer() {
            super(new ResourceLocation("set_stew_effect"), SetStewEffect.class);
        }

        @Override
        public void serialize(JsonObject json, SetStewEffect value, JsonSerializationContext context) {}

        @Override
        public SetStewEffect deserialize(JsonObject json, JsonDeserializationContext context, LootCondition[] conditions) {
            List<Potion> effects = new ArrayList<>();
            List<RandomValueRange> durations = new ArrayList<>();
            for (JsonElement element : JsonUtils.getJsonArray(json, "effects", new com.google.gson.JsonArray())) {
                JsonObject entry = element.getAsJsonObject();
                Potion effect = ForgeRegistries.POTIONS.getValue(new ResourceLocation(JsonUtils.getString(entry, "type")));
                if (effect == null) continue;
                effects.add(effect);
                durations.add(JsonUtils.deserializeClass(entry, "duration", context, RandomValueRange.class));
            }
            return new SetStewEffect(conditions, effects, durations);
        }
    }
}
