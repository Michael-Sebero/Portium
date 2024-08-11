package com.michaelsebero.portium.command.brigadier.arguments;

import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;

public class StringArgumentType implements ArgumentType<String> {

    public enum StringType {
        /** A single unquoted word. */
        SINGLE_WORD,
        /** A word, or a quoted phrase. */
        QUOTABLE_PHRASE,
        /** Everything to the end of the command. */
        GREEDY_PHRASE
    }

    private final StringType type;

    private StringArgumentType(StringType type) {
        this.type = type;
    }

    public static StringArgumentType word() {
        return new StringArgumentType(StringType.SINGLE_WORD);
    }

    public static StringArgumentType string() {
        return new StringArgumentType(StringType.QUOTABLE_PHRASE);
    }

    public static StringArgumentType greedyString() {
        return new StringArgumentType(StringType.GREEDY_PHRASE);
    }

    public static String getString(CommandContext<?> context, String name) {
        return context.getArgument(name, String.class);
    }

    public StringType getType() {
        return this.type;
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        switch (this.type) {
            case GREEDY_PHRASE:
                String text = reader.getRemaining();
                reader.setCursor(reader.getTotalLength());
                return text;
            case SINGLE_WORD:
                return reader.readUnquotedString();
            default:
                return reader.readString();
        }
    }

    /** Quotes a string if it would not survive being read back as a word. */
    public static String escapeIfRequired(String input) {
        for (char c : input.toCharArray()) {
            if (!StringReader.isAllowedInUnquotedString(c)) return escape(input);
        }
        return input;
    }

    private static String escape(String input) {
        StringBuilder result = new StringBuilder("\"");
        for (char c : input.toCharArray()) {
            if (c == '\\' || c == '"') result.append('\\');
            result.append(c);
        }
        return result.append('"').toString();
    }
}
