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
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Players by selector, or by name even when offline, as game profiles (1.13): for bans, operators and the whitelist. */
public class GameProfileArgument implements ArgumentType<GameProfileArgument.Result> {

    @FunctionalInterface
    public interface Result {
        Collection<GameProfile> getProfiles(CommandSource source) throws CommandSyntaxException;
    }

    public static GameProfileArgument gameProfile() {
        return new GameProfileArgument();
    }

    public static Collection<GameProfile> getGameProfiles(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, Result.class).getProfiles(context.getSource());
    }

    @Override
    public Result parse(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '@') {
            EntitySelector selector = new EntitySelectorParser(reader).parse();
            if (selector.includesEntities()) throw CommandExceptions.PLAYER_ONLY.create();
            return source -> {
                List<EntityPlayerMP> players = selector.selectPlayers(source);
                if (players.isEmpty()) throw CommandExceptions.PLAYER_NOT_FOUND.create();
                List<GameProfile> profiles = new ArrayList<>();
                for (EntityPlayerMP player : players) profiles.add(player.getGameProfile());
                return profiles;
            };
        }
        int start = reader.getCursor();
        while (reader.canRead() && reader.peek() != ' ') reader.skip();
        String name = reader.getString().substring(start, reader.getCursor());
        return source -> {
            GameProfile profile = source.getServer().getPlayerProfileCache().getGameProfileForUsername(name);
            if (profile == null) throw CommandExceptions.PLAYER_UNKNOWN.create();
            return Collections.singleton(profile);
        };
    }

    @Override
    public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return EntityArgument.players().listSuggestions(context, builder);
    }
}
