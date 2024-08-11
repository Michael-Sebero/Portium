package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 1.13's coordinate suggestions. The candidate is the block the typing player looks at, or {@code ~ ~ ~}, or
 * {@code ^ ^ ^} once local coordinates are started. It is offered a part at a time ({@code ~}, {@code ~ ~},
 * {@code ~ ~ ~}), continuing whatever parts are already typed, and only if the whole parses as the argument.
 */
final class CoordinateSuggestions {

    static <S> Suggestions suggest(CommandContext<S> context, SuggestionsBuilder builder, boolean threeAxes, ArgumentType<?> argument) {
        String remaining = builder.getRemaining();
        String[] candidate = candidate(context, remaining);
        String x = candidate[0];
        String y = candidate[1];
        String z = candidate[2];
        List<String> options = new ArrayList<>();
        String[] typed = remaining.isEmpty() ? new String[0] : remaining.split(" ");
        if (threeAxes) {
            if (typed.length == 0 && parses(argument, x + " " + y + " " + z)) {
                options.add(x);
                options.add(x + " " + y);
                options.add(x + " " + y + " " + z);
            } else if (typed.length == 1 && parses(argument, typed[0] + " " + y + " " + z)) {
                options.add(typed[0] + " " + y);
                options.add(typed[0] + " " + y + " " + z);
            } else if (typed.length == 2 && parses(argument, typed[0] + " " + typed[1] + " " + z)) {
                options.add(typed[0] + " " + typed[1] + " " + z);
            }
        } else {
            if (typed.length == 0 && parses(argument, x + " " + z)) {
                options.add(x);
                options.add(x + " " + z);
            } else if (typed.length == 1 && parses(argument, typed[0] + " " + z)) {
                options.add(typed[0] + " " + z);
            }
        }
        String lower = remaining.toLowerCase(Locale.ROOT);
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) builder.suggest(option);
        }
        return builder.build();
    }

    private static <S> String[] candidate(CommandContext<S> context, String remaining) {
        if (!remaining.isEmpty() && remaining.charAt(0) == '^') return new String[] {"^", "^", "^"};
        BlockPos target = context.getSource() instanceof CommandSource ? ((CommandSource) context.getSource()).getTargetedBlock() : null;
        if (target == null) return new String[] {"~", "~", "~"};
        return new String[] {Integer.toString(target.getX()), Integer.toString(target.getY()), Integer.toString(target.getZ())};
    }

    private static boolean parses(ArgumentType<?> argument, String text) {
        try {
            argument.parse(new StringReader(text));
            return true;
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    private CoordinateSuggestions() {}
}
