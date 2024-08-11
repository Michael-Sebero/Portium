package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;
import com.michaelsebero.portium.datapack.CommandFunction;
import com.michaelsebero.portium.datapack.DataPackManager;
import com.michaelsebero.portium.datapack.Tag;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** A function ({@code ns:name}) or function tag ({@code #ns:tag}), looked up when the command runs. */
public class FunctionArgument implements ArgumentType<FunctionArgument.Result> {

    public interface Result {

        Collection<CommandFunction> get(CommandContext<CommandSource> context) throws CommandSyntaxException;
    }

    public static FunctionArgument function() {
        return new FunctionArgument();
    }

    public static Collection<CommandFunction> getFunctions(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, Result.class).get(context);
    }

    private static DataPackManager manager() throws CommandSyntaxException {
        DataPackManager manager = DataPackManager.get();
        if (manager == null) throw CommandExceptions.FUNCTION_UNKNOWN.create("?");
        return manager;
    }

    @Override
    public Result parse(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '#') {
            reader.skip();
            ResourceLocation id = ResourceLocationArgument.read(reader);
            return context -> {
                DataPackManager manager = manager();
                Tag<ResourceLocation> tag = manager.getTags().getFunctions().get(id);
                if (tag == null) throw CommandExceptions.FUNCTION_TAG_UNKNOWN.create(id.toString());
                List<CommandFunction> functions = new ArrayList<>();
                for (ResourceLocation functionId : tag.getValues()) {
                    CommandFunction function = manager.getFunctions().get(functionId);
                    if (function == null) throw CommandExceptions.FUNCTION_UNKNOWN.create(functionId.toString());
                    functions.add(function);
                }
                return functions;
            };
        }
        ResourceLocation id = ResourceLocationArgument.read(reader);
        return context -> {
            CommandFunction function = manager().getFunctions().get(id);
            if (function == null) throw CommandExceptions.FUNCTION_UNKNOWN.create(id.toString());
            return Collections.singleton(function);
        };
    }

    @Override
    public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        DataPackManager manager = DataPackManager.get();
        if (manager == null) return Suggestions.empty();
        IdSuggestions.suggest(manager.getTags().getFunctions().getIds(), builder, "#");
        return IdSuggestions.suggest(manager.getFunctions().getIds(), builder);
    }
}
