package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import net.minecraft.util.text.ITextComponent;

import java.util.Arrays;
import java.util.Collection;

/** JSON text such as {@code {"text":"hi","color":"red"}} for {@code tellraw}, {@code title} and the like. */
public class ComponentArgument implements ArgumentType<ITextComponent> {

    public static ComponentArgument component() {
        return new ComponentArgument();
    }

    public static ITextComponent getComponent(CommandContext<?> context, String name) {
        return context.getArgument(name, ITextComponent.class);
    }

    @Override
    public ITextComponent parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        int end = jsonEnd(reader.getString(), start);
        if (end <= start) throw CommandExceptions.COMPONENT_EXPECTED.createWithContext(reader);
        String json = reader.getString().substring(start, end);
        try {
            ITextComponent component = ITextComponent.Serializer.fromJsonLenient(json);
            if (component == null) throw CommandExceptions.COMPONENT_EXPECTED.createWithContext(reader);
            reader.setCursor(end);
            return component;
        } catch (RuntimeException e) {
            String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            throw CommandExceptions.COMPONENT_INVALID.createWithContext(reader, message);
        }
    }

    /**
     * Where the JSON value starting at {@code start} ends: the matching bracket for objects and arrays (ignoring
     * brackets in strings), the closing quote for strings, or the next space for bare values.
     */
    static int jsonEnd(String text, int start) {
        if (start >= text.length()) return start;
        char first = text.charAt(start);
        if (first == '"') return skipString(text, start);
        if (first != '{' && first != '[') {
            int end = text.indexOf(' ', start);
            return end < 0 ? text.length() : end;
        }
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                i = skipString(text, i) - 1;
            } else if (c == '{' || c == '[') {
                depth++;
            } else if (c == '}' || c == ']') {
                if (--depth == 0) return i + 1;
            }
        }
        return text.length();
    }

    private static int skipString(String text, int quote) {
        for (int i = quote + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\') i++;
            else if (c == '"') return i + 1;
        }
        return text.length();
    }

    @Override
    public Collection<String> getExamples() {
        return Arrays.asList("\"hello world\"", "\"\"", "\"{\"text\":\"hello world\"}", "[\"\"]");
    }
}
