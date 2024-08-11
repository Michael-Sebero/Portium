package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import net.minecraft.util.ResourceLocation;

/** A namespaced id such as {@code minecraft:stone} (the namespace defaults to {@code minecraft}). */
public class ResourceLocationArgument implements ArgumentType<ResourceLocation> {

    public static ResourceLocationArgument resourceLocation() {
        return new ResourceLocationArgument();
    }

    public static ResourceLocation getResourceLocation(CommandContext<?> context, String name) {
        return context.getArgument(name, ResourceLocation.class);
    }

    public static boolean isValidCharacter(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c == '_' || c == ':' || c == '/' || c == '.' || c == '-';
    }

    /** Reads an id, as 1.13's {@code ResourceLocation.read} does. */
    public static ResourceLocation read(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        while (reader.canRead() && isValidCharacter(reader.peek())) reader.skip();
        String id = reader.getString().substring(start, reader.getCursor());
        int colon = id.indexOf(':');
        if (id.isEmpty() || colon != id.lastIndexOf(':') || colon == id.length() - 1) {
            reader.setCursor(start);
            throw CommandExceptions.ID_INVALID.createWithContext(reader);
        }
        return new ResourceLocation(id);
    }

    @Override
    public ResourceLocation parse(StringReader reader) throws CommandSyntaxException {
        return read(reader);
    }
}
