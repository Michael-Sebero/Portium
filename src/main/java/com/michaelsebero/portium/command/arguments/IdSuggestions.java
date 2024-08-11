package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;
import com.michaelsebero.portium.compat.LegacyBlockStates;
import com.michaelsebero.portium.datapack.DataPackManager;
import net.minecraft.util.ResourceLocation;

/** Suggests namespaced ids, matching what's typed against the full id or just its path (1.13's behaviour). */
public final class IdSuggestions {

    public static Suggestions suggest(Iterable<?> ids, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(java.util.Locale.ROOT);
        for (Object id : ids) {
            String text = id.toString();
            String path = text.contains(":") ? text.substring(text.indexOf(':') + 1) : text;
            if (text.startsWith(remaining) || path.startsWith(remaining)) builder.suggest(text);
        }
        return builder.build();
    }

    /** Ids with a prefix (such as {@code #} for tags). */
    public static Suggestions suggest(Iterable<ResourceLocation> ids, SuggestionsBuilder builder, String prefix) {
        String remaining = builder.getRemaining().toLowerCase(java.util.Locale.ROOT);
        if (!remaining.startsWith(prefix) && !prefix.startsWith(remaining)) return builder.build();
        String typed = remaining.length() >= prefix.length() ? remaining.substring(prefix.length()) : "";
        for (ResourceLocation id : ids) {
            if (id.toString().startsWith(typed) || id.getPath().startsWith(typed)) builder.suggest(prefix + id);
        }
        return builder.build();
    }

    static Suggestions blocks(SuggestionsBuilder builder, boolean tags) {
        if (tags && DataPackManager.get() != null) suggest(DataPackManager.get().getTags().getBlocks().getIds(), builder, "#");
        return suggest(LegacyBlockStates.modernNames(), builder);
    }

    private IdSuggestions() {}
}
