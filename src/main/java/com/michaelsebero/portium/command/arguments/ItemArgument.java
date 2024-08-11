package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;
import com.michaelsebero.portium.command.nbt.SnbtParser;
import com.michaelsebero.portium.compat.LegacyItem;
import com.michaelsebero.portium.compat.LegacyItems;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Arrays;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

/** An item in 1.13's syntax ({@code minecraft:red_wool{display:{Name:'"x"'}}}), translated to 1.12. */
public class ItemArgument implements ArgumentType<ItemInput> {

    public static ItemArgument item() {
        return new ItemArgument();
    }

    public static ItemInput getItem(CommandContext<?> context, String name) {
        return context.getArgument(name, ItemInput.class);
    }

    @Override
    public ItemInput parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        ResourceLocation id = ResourceLocationArgument.read(reader);
        LegacyItem item = LegacyItems.resolve(id.toString());
        if (item == null) {
            reader.setCursor(start);
            throw CommandExceptions.ITEM_ID_INVALID.createWithContext(reader, id.toString());
        }
        NBTTagCompound tag = reader.canRead() && reader.peek() == '{' ? new SnbtParser(reader).readStruct() : null;
        return new ItemInput(item, tag);
    }

    /** 1.13 item ids for suggestions: Portium's items under {@code minecraft}, plus every flattened block. */
    static SortedSet<String> modernIds() {
        SortedSet<String> ids = new TreeSet<>(com.michaelsebero.portium.compat.LegacyBlockStates.modernNames());
        for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
            String modern = com.michaelsebero.portium.Tags.MOD_ID.equals(id.getNamespace()) ? "minecraft:" + id.getPath() : id.toString();
            if (LegacyItems.resolve(modern) != null) ids.add(modern);
        }
        return ids;
    }

    @Override
    public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return IdSuggestions.suggest(modernIds(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return Arrays.asList("stick", "minecraft:stick", "stick{foo=bar}");
    }
}
