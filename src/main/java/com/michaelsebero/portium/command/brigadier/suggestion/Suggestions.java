package com.michaelsebero.portium.command.brigadier.suggestion;

import com.michaelsebero.portium.command.brigadier.context.StringRange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Suggestions {

    private static final Suggestions EMPTY = new Suggestions(StringRange.at(0), Collections.emptyList());

    private final StringRange range;
    private final List<Suggestion> suggestions;

    public Suggestions(StringRange range, List<Suggestion> suggestions) {
        this.range = range;
        this.suggestions = suggestions;
    }

    public StringRange getRange() {
        return this.range;
    }

    public List<Suggestion> getList() {
        return this.suggestions;
    }

    public boolean isEmpty() {
        return this.suggestions.isEmpty();
    }

    public static Suggestions empty() {
        return EMPTY;
    }

    public static Suggestions merge(String command, Collection<Suggestions> input) {
        if (input.isEmpty()) return EMPTY;
        if (input.size() == 1) return input.iterator().next();
        Set<Suggestion> texts = new HashSet<>();
        for (Suggestions suggestions : input) texts.addAll(suggestions.getList());
        return create(command, texts);
    }

    public static Suggestions create(String command, Collection<Suggestion> suggestions) {
        if (suggestions.isEmpty()) return EMPTY;
        int start = Integer.MAX_VALUE;
        int end = Integer.MIN_VALUE;
        for (Suggestion suggestion : suggestions) {
            start = Math.min(suggestion.getRange().getStart(), start);
            end = Math.max(suggestion.getRange().getEnd(), end);
        }
        StringRange range = new StringRange(start, end);
        Set<Suggestion> texts = new HashSet<>();
        for (Suggestion suggestion : suggestions) texts.add(suggestion.expand(command, range));
        List<Suggestion> sorted = new ArrayList<>(texts);
        sorted.sort(Suggestion::compareToIgnoreCase);
        return new Suggestions(range, sorted);
    }
}
