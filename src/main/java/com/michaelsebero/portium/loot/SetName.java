package com.michaelsebero.portium.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.michaelsebero.portium.Tags;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;

import java.util.Random;

/**
 * 1.13's {@code set_name} loot function: names the item with a text component. 1.12 stores item names as plain text,
 * so translatable names are resolved in the server's language when the loot is generated.
 */
public class SetName extends LootFunction {

    private final ITextComponent name;

    public SetName(LootCondition[] conditions, ITextComponent name) {
        super(conditions);
        this.name = name;
    }

    @Override
    public ItemStack apply(ItemStack stack, Random random, LootContext context) {
        stack.setStackDisplayName(this.name.getUnformattedText());
        return stack;
    }

    public static class Serializer extends LootFunction.Serializer<SetName> {

        public Serializer() {
            super(new ResourceLocation(Tags.MOD_ID, "set_name"), SetName.class);
        }

        @Override
        public void serialize(JsonObject json, SetName function, JsonSerializationContext context) {
            json.add("name", new JsonParser().parse(ITextComponent.Serializer.componentToJson(function.name)));
        }

        @Override
        public SetName deserialize(JsonObject json, JsonDeserializationContext context, LootCondition[] conditions) {
            return new SetName(conditions, ITextComponent.Serializer.fromJsonLenient(json.get("name").toString()));
        }
    }
}
