package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.ComponentResolver;
import com.michaelsebero.portium.command.arguments.selector.EntitySelector;
import com.michaelsebero.portium.command.arguments.selector.EntitySelectorParser;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;
import com.michaelsebero.portium.compat.LegacyCriteria;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.IScoreCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Arguments for {@code scoreboard}, {@code team} and {@code execute ... score}. */
public final class ScoreboardArguments {

    public static Scoreboard scoreboard(CommandSource source) {
        return source.getServer().getWorld(0).getScoreboard();
    }

    // ---- Objectives ----

    public static ArgumentType<String> objective() {
        return new ArgumentType<String>() {
            @Override
            public String parse(StringReader reader) {
                return reader.readUnquotedString();
            }

            @Override
            public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                if (!(context.getSource() instanceof CommandSource)) return Suggestions.empty();
                List<String> names = new ArrayList<>();
                for (ScoreObjective objective : scoreboard((CommandSource) context.getSource()).getScoreObjectives()) names.add(objective.getName());
                return IdSuggestions.suggest(names, builder);
            }
        };
    }

    public static ScoreObjective getObjective(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        String objectiveName = context.getArgument(name, String.class);
        ScoreObjective objective = scoreboard(context.getSource()).getObjective(objectiveName);
        if (objective == null) throw CommandExceptions.OBJECTIVE_UNKNOWN.create(objectiveName);
        return objective;
    }

    /** An objective commands may change, which excludes read-only criteria such as {@code health}. */
    public static ScoreObjective getWritableObjective(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        ScoreObjective objective = getObjective(context, name);
        if (objective.getCriteria().isReadOnly()) throw CommandExceptions.OBJECTIVE_READ_ONLY.create(objective.getName());
        return objective;
    }

    public static ArgumentType<IScoreCriteria> criteria() {
        return new ArgumentType<IScoreCriteria>() {
            @Override
            public IScoreCriteria parse(StringReader reader) throws CommandSyntaxException {
                int start = reader.getCursor();
                while (reader.canRead() && reader.peek() != ' ') reader.skip();
                String name = reader.getString().substring(start, reader.getCursor());
                IScoreCriteria criteria = LegacyCriteria.convert(name);
                if (criteria == null) {
                    reader.setCursor(start);
                    throw CommandExceptions.CRITERIA_INVALID.create(name);
                }
                return criteria;
            }

            @Override
            public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                List<String> names = new ArrayList<>();
                for (String name : new String[] {"dummy", "trigger", "deathCount", "playerKillCount", "totalKillCount", "health", "xp", "level", "food", "air", "armor"}) names.add(name);
                for (TextFormatting color : TextFormatting.values()) {
                    if (color.isColor()) {
                        names.add("teamkill." + color.getFriendlyName());
                        names.add("killedByTeam." + color.getFriendlyName());
                    }
                }
                for (String type : new String[] {"custom", "mined", "crafted", "used", "broken", "picked_up", "dropped", "killed", "killed_by"}) names.add("minecraft." + type + ":");
                return IdSuggestions.suggest(names, builder);
            }
        };
    }

    // ---- Score holders ----

    /** One or many score holders: a name, {@code *} (every holder; or the source when only one is allowed) or a selector. */
    public interface ScoreHolders {

        Collection<String> get(CommandSource source, Collection<String> everyone) throws CommandSyntaxException;
    }

    public static ArgumentType<ScoreHolders> scoreHolder(boolean multiple) {
        return new ArgumentType<ScoreHolders>() {
            @Override
            public ScoreHolders parse(StringReader reader) throws CommandSyntaxException {
                if (reader.canRead() && reader.peek() == '@') {
                    EntitySelector selector = new EntitySelectorParser(reader).parse();
                    if (!multiple && selector.getLimit() > 1) throw CommandExceptions.ENTITY_TOO_MANY.create();
                    return (source, everyone) -> {
                        List<String> names = new ArrayList<>();
                        for (Entity entity : selector.select(source)) names.add(ComponentResolver.scoreHolder(entity));
                        if (names.isEmpty()) throw CommandExceptions.ENTITY_NOT_FOUND.create();
                        return names;
                    };
                }
                int start = reader.getCursor();
                while (reader.canRead() && reader.peek() != ' ') reader.skip();
                String name = reader.getString().substring(start, reader.getCursor());
                if ("*".equals(name)) {
                    return (source, everyone) -> {
                        if (everyone.isEmpty()) throw CommandExceptions.SCORE_HOLDER_EMPTY.create();
                        return everyone;
                    };
                }
                return (source, everyone) -> Collections.singleton(name);
            }

            @Override
            public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                return EntityArgument.entities().listSuggestions(context, builder);
            }
        };
    }

    /** {@code *} means every name with a score (1.12 misnames this list {@code getObjectiveNames}). */
    public static Collection<String> getScoreHolders(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        Set<String> everyone = new LinkedHashSet<>(scoreboard(context.getSource()).getObjectiveNames());
        return context.getArgument(name, ScoreHolders.class).get(context.getSource(), everyone);
    }

    public static String getScoreHolder(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        Collection<String> holders = context.getArgument(name, ScoreHolders.class).get(context.getSource(), Collections.emptySet());
        if (holders.size() > 1) throw CommandExceptions.ENTITY_TOO_MANY.create();
        return holders.iterator().next();
    }

    // ---- Operations ----

    public interface Operation {

        /** Applies to the target score, given the source score; returns nothing, changing the scores in place. */
        void apply(Score target, Score source) throws CommandSyntaxException;
    }

    public static ArgumentType<Operation> operation() {
        return new ArgumentType<Operation>() {
            @Override
            public Operation parse(StringReader reader) throws CommandSyntaxException {
                int start = reader.getCursor();
                while (reader.canRead() && reader.peek() != ' ') reader.skip();
                String op = reader.getString().substring(start, reader.getCursor());
                switch (op) {
                    case "=": return (a, b) -> a.setScorePoints(b.getScorePoints());
                    case "+=": return (a, b) -> a.setScorePoints(a.getScorePoints() + b.getScorePoints());
                    case "-=": return (a, b) -> a.setScorePoints(a.getScorePoints() - b.getScorePoints());
                    case "*=": return (a, b) -> a.setScorePoints(a.getScorePoints() * b.getScorePoints());
                    case "/=": return (a, b) -> {
                        if (b.getScorePoints() == 0) throw CommandExceptions.OPERATION_DIVIDE_BY_ZERO.create();
                        a.setScorePoints(Math.floorDiv(a.getScorePoints(), b.getScorePoints()));
                    };
                    case "%=": return (a, b) -> {
                        if (b.getScorePoints() == 0) throw CommandExceptions.OPERATION_DIVIDE_BY_ZERO.create();
                        a.setScorePoints(Math.floorMod(a.getScorePoints(), b.getScorePoints()));
                    };
                    case "<": return (a, b) -> a.setScorePoints(Math.min(a.getScorePoints(), b.getScorePoints()));
                    case ">": return (a, b) -> a.setScorePoints(Math.max(a.getScorePoints(), b.getScorePoints()));
                    case "><": return (a, b) -> {
                        int swap = a.getScorePoints();
                        a.setScorePoints(b.getScorePoints());
                        b.setScorePoints(swap);
                    };
                    default:
                        reader.setCursor(start);
                        throw CommandExceptions.OPERATION_INVALID.createWithContext(reader);
                }
            }

            @Override
            public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                for (String op : new String[] {"=", "+=", "-=", "*=", "/=", "%=", "<", ">", "><"}) builder.suggest(op);
                return builder.build();
            }
        };
    }

    // ---- Display slots, teams, colours ----

    public static ArgumentType<Integer> displaySlot() {
        return new ArgumentType<Integer>() {
            @Override
            public Integer parse(StringReader reader) throws CommandSyntaxException {
                String name = reader.readUnquotedString();
                int slot = Scoreboard.getObjectiveDisplaySlotNumber(name);
                if (slot < 0) throw CommandExceptions.SCOREBOARD_SLOT_INVALID.create(name);
                return slot;
            }

            @Override
            public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                List<String> names = new ArrayList<>();
                for (int i = 0; i < 19; i++) {
                    String name = Scoreboard.getObjectiveDisplaySlot(i);
                    if (name != null) names.add(name);
                }
                return IdSuggestions.suggest(names, builder);
            }
        };
    }

    public static ArgumentType<String> team() {
        return new ArgumentType<String>() {
            @Override
            public String parse(StringReader reader) {
                return reader.readUnquotedString();
            }

            @Override
            public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                if (!(context.getSource() instanceof CommandSource)) return Suggestions.empty();
                return IdSuggestions.suggest(scoreboard((CommandSource) context.getSource()).getTeamNames(), builder);
            }
        };
    }

    public static ScorePlayerTeam getTeam(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        String teamName = context.getArgument(name, String.class);
        ScorePlayerTeam team = scoreboard(context.getSource()).getTeam(teamName);
        if (team == null) throw CommandExceptions.TEAM_UNKNOWN.create(teamName);
        return team;
    }

    public static ArgumentType<TextFormatting> color() {
        return new ArgumentType<TextFormatting>() {
            @Override
            public TextFormatting parse(StringReader reader) throws CommandSyntaxException {
                String name = reader.readUnquotedString();
                TextFormatting color = TextFormatting.getValueByName(name);
                if (color == null || !color.isColor() && color != TextFormatting.RESET) throw CommandExceptions.COLOR_INVALID.create(name);
                return color;
            }

            @Override
            public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                return IdSuggestions.suggest(TextFormatting.getValidValues(true, false), builder);
            }
        };
    }

    private ScoreboardArguments() {}
}
