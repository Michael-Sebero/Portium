package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.EntityAnchor;
import com.michaelsebero.portium.command.arguments.selector.EntitySelector;
import com.michaelsebero.portium.command.arguments.selector.EntitySelectorParser;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Small argument types: entity anchors, durations, inventory slots, number ranges and chat messages. */
public final class MiscArguments {

    public static ArgumentType<EntityAnchor> anchor() {
        return new ArgumentType<EntityAnchor>() {
            @Override
            public EntityAnchor parse(StringReader reader) throws CommandSyntaxException {
                int start = reader.getCursor();
                String name = reader.readUnquotedString();
                EntityAnchor anchor = EntityAnchor.byName(name);
                if (anchor == null) {
                    reader.setCursor(start);
                    throw CommandExceptions.ANCHOR_INVALID.createWithContext(reader, name);
                }
                return anchor;
            }

            @Override
            public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                return IdSuggestions.suggest(Arrays.asList("eyes", "feet"), builder);
            }
        };
    }

    /** A time in ticks, written plainly or with a unit: {@code 1d} (a day), {@code 5s}, {@code 20t}. */
    public static ArgumentType<Integer> time() {
        return new ArgumentType<Integer>() {
            @Override
            public Integer parse(StringReader reader) throws CommandSyntaxException {
                float value = reader.readFloat();
                String unit = reader.readUnquotedString();
                int scale;
                switch (unit) {
                    case "": case "t": scale = 1; break;
                    case "s": scale = 20; break;
                    case "d": scale = 24000; break;
                    default: throw CommandExceptions.TIME_INVALID_UNIT.create();
                }
                return Math.round(value * scale);
            }

            @Override
            public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                String remaining = builder.getRemaining();
                if (!remaining.isEmpty() && Character.isDigit(remaining.charAt(remaining.length() - 1))) {
                    for (String unit : new String[] {"d", "s", "t"}) builder.suggest(remaining + unit);
                }
                return builder.build();
            }
        };
    }

    private static final Map<String, Integer> SLOTS = new HashMap<>();

    static {
        for (int i = 0; i < 54; i++) SLOTS.put("container." + i, i);
        for (int i = 0; i < 9; i++) SLOTS.put("hotbar." + i, i);
        for (int i = 0; i < 27; i++) SLOTS.put("inventory." + i, 9 + i);
        for (int i = 0; i < 27; i++) SLOTS.put("enderchest." + i, 200 + i);
        for (int i = 0; i < 8; i++) SLOTS.put("villager." + i, 300 + i);
        for (int i = 0; i < 15; i++) SLOTS.put("horse." + i, 500 + i);
        SLOTS.put("weapon", 98);
        SLOTS.put("weapon.mainhand", 98);
        SLOTS.put("weapon.offhand", 99);
        SLOTS.put("armor.head", 103);
        SLOTS.put("armor.chest", 102);
        SLOTS.put("armor.legs", 101);
        SLOTS.put("armor.feet", 100);
        SLOTS.put("horse.saddle", 400);
        SLOTS.put("horse.armor", 401);
        SLOTS.put("horse.chest", 499);
    }

    /** An inventory slot by 1.13 name ({@code weapon.mainhand}, {@code container.5}), as a 1.12 slot number. */
    public static ArgumentType<Integer> slot() {
        return new ArgumentType<Integer>() {
            @Override
            public Integer parse(StringReader reader) throws CommandSyntaxException {
                String name = reader.readUnquotedString();
                Integer slot = SLOTS.get(name);
                if (slot == null) throw CommandExceptions.SLOT_UNKNOWN.create(name);
                return slot;
            }

            @Override
            public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                return IdSuggestions.suggest(new java.util.TreeSet<>(SLOTS.keySet()), builder);
            }
        };
    }

    public static ArgumentType<MinMaxBounds> intRange() {
        return MinMaxBounds::parseInt;
    }

    public static ArgumentType<MinMaxBounds> floatRange() {
        return MinMaxBounds::parseFloat;
    }

    // ---- Messages ----

    /** Chat text in which selectors are replaced by the names they match (for sources allowed to use selectors). */
    public static final class Message {

        private final String text;
        private final int[] starts;
        private final int[] ends;
        private final EntitySelector[] selectors;

        Message(String text, int[] starts, int[] ends, EntitySelector[] selectors) {
            this.text = text;
            this.starts = starts;
            this.ends = ends;
            this.selectors = selectors;
        }

        public ITextComponent toComponent(CommandSource source) throws CommandSyntaxException {
            if (this.selectors.length == 0 || !source.hasPermissionLevel(2)) return new TextComponentString(this.text);
            TextComponentString result = new TextComponentString(this.text.substring(0, this.starts[0]));
            int position = this.starts[0];
            for (int i = 0; i < this.selectors.length; i++) {
                if (position < this.starts[i]) result.appendText(this.text.substring(position, this.starts[i]));
                List<? extends Entity> entities = this.selectors[i].select(source);
                for (int j = 0; j < entities.size(); j++) {
                    if (j > 0) result.appendText(", ");
                    result.appendSibling(entities.get(j).getDisplayName());
                }
                position = this.ends[i];
            }
            if (position < this.text.length()) result.appendText(this.text.substring(position));
            return result;
        }
    }

    public static ArgumentType<Message> message() {
        return reader -> {
            String text = reader.getRemaining();
            int base = reader.getCursor();
            List<int[]> spans = new ArrayList<>();
            List<EntitySelector> selectors = new ArrayList<>();
            while (reader.canRead()) {
                if (reader.peek() != '@') {
                    reader.skip();
                    continue;
                }
                int start = reader.getCursor();
                try {
                    selectors.add(new EntitySelectorParser(reader).parse());
                    spans.add(new int[] {start - base, reader.getCursor() - base});
                } catch (CommandSyntaxException e) {
                    if (e.getType() != CommandExceptions.SELECTOR_MISSING && e.getType() != CommandExceptions.UNKNOWN_SELECTOR) throw e;
                    reader.setCursor(start + 1);
                }
            }
            int[] starts = new int[spans.size()];
            int[] ends = new int[spans.size()];
            for (int i = 0; i < spans.size(); i++) {
                starts[i] = spans.get(i)[0];
                ends[i] = spans.get(i)[1];
            }
            return new Message(text, starts, ends, selectors.toArray(new EntitySelector[0]));
        };
    }

    public static ITextComponent getMessage(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, Message.class).toComponent(context.getSource());
    }

    private MiscArguments() {}
}
