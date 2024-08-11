package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.arguments.selector.EntitySelector;
import com.michaelsebero.portium.command.arguments.selector.EntitySelectorParser;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** One or more entities or players, by selector, name or UUID. */
public class EntityArgument implements ArgumentType<EntitySelector> {

    private final boolean single;
    private final boolean playersOnly;

    private EntityArgument(boolean single, boolean playersOnly) {
        this.single = single;
        this.playersOnly = playersOnly;
    }

    public static EntityArgument entity() {
        return new EntityArgument(true, false);
    }

    public static EntityArgument entities() {
        return new EntityArgument(false, false);
    }

    public static EntityArgument player() {
        return new EntityArgument(true, true);
    }

    public static EntityArgument players() {
        return new EntityArgument(false, true);
    }

    public static Entity getEntity(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, EntitySelector.class).selectOne(context.getSource());
    }

    /** At least one entity; none is an error. */
    public static Collection<? extends Entity> getEntities(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        Collection<? extends Entity> entities = getEntitiesAllowingNone(context, name);
        if (entities.isEmpty()) throw CommandExceptions.ENTITY_NOT_FOUND.create();
        return entities;
    }

    public static Collection<? extends Entity> getEntitiesAllowingNone(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, EntitySelector.class).select(context.getSource());
    }

    public static Collection<EntityPlayerMP> getPlayersAllowingNone(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, EntitySelector.class).selectPlayers(context.getSource());
    }

    public static EntityPlayerMP getPlayer(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, EntitySelector.class).selectOnePlayer(context.getSource());
    }

    public static Collection<EntityPlayerMP> getPlayers(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        List<EntityPlayerMP> players = context.getArgument(name, EntitySelector.class).selectPlayers(context.getSource());
        if (players.isEmpty()) throw CommandExceptions.PLAYER_NOT_FOUND.create();
        return players;
    }

    @Override
    public EntitySelector parse(StringReader reader) throws CommandSyntaxException {
        EntitySelector selector = new EntitySelectorParser(reader).parse();
        if (selector.getLimit() > 1 && this.single) {
            reader.setCursor(0);
            throw (this.playersOnly ? CommandExceptions.PLAYER_TOO_MANY : CommandExceptions.ENTITY_TOO_MANY).createWithContext(reader);
        }
        if (selector.includesEntities() && this.playersOnly && !selector.isSelfSelector()) {
            reader.setCursor(0);
            throw CommandExceptions.PLAYER_ONLY.createWithContext(reader);
        }
        return selector;
    }

    @Override
    public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        StringReader reader = new StringReader(builder.getInput());
        reader.setCursor(builder.getStart());
        EntitySelectorParser parser = new EntitySelectorParser(reader, !(context.getSource() instanceof CommandSource) || ((CommandSource) context.getSource()).hasPermissionLevel(2));
        try {
            parser.parse();
        } catch (CommandSyntaxException ignored) {
            // Suggest for whatever was understood.
        }
        return parser.fillSuggestions(builder, names -> {
            if (!(context.getSource() instanceof CommandSource)) return;
            CommandSource source = (CommandSource) context.getSource();
            List<String> candidates = new ArrayList<>(Arrays.asList(source.getServer().getOnlinePlayerNames()));
            // 1.13 also offers the entity the player is looking at, by UUID.
            if (!this.playersOnly && source.getTargetedEntity() != null) candidates.add(source.getTargetedEntity());
            for (String candidate : candidates) {
                if (candidate.toLowerCase(Locale.ROOT).startsWith(names.getRemaining().toLowerCase(Locale.ROOT))) names.suggest(candidate);
            }
        });
    }

    @Override
    public Collection<String> getExamples() {
        return this.single ? Arrays.asList("Player", "0123", "@e", "@e[type=foo]") : Arrays.asList("Player", "0123", "dd12be42-52a9-4a91-a8a1-11c01849e498", "@e");
    }
}
