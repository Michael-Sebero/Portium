package com.michaelsebero.portium.command.brigadier.suggestion;

import com.michaelsebero.portium.command.brigadier.Message;
import com.michaelsebero.portium.command.brigadier.context.StringRange;

public class Suggestion implements Comparable<Suggestion> {

    private final StringRange range;
    private final String text;
    private final Message tooltip;

    public Suggestion(StringRange range, String text) {
        this(range, text, null);
    }

    public Suggestion(StringRange range, String text, Message tooltip) {
        this.range = range;
        this.text = text;
        this.tooltip = tooltip;
    }

    public StringRange getRange() {
        return this.range;
    }

    public String getText() {
        return this.text;
    }

    public Message getTooltip() {
        return this.tooltip;
    }

    /** The input with this suggestion applied. */
    public String apply(String input) {
        if (this.range.getStart() == 0 && this.range.getEnd() == input.length()) return this.text;
        StringBuilder result = new StringBuilder();
        if (this.range.getStart() > 0) result.append(input, 0, this.range.getStart());
        result.append(this.text);
        if (this.range.getEnd() < input.length()) result.append(input.substring(this.range.getEnd()));
        return result.toString();
    }

    /** This suggestion widened to cover {@code range} of {@code command}. */
    public Suggestion expand(String command, StringRange range) {
        if (range.equals(this.range)) return this;
        StringBuilder result = new StringBuilder();
        if (range.getStart() < this.range.getStart()) result.append(command, range.getStart(), this.range.getStart());
        result.append(this.text);
        if (range.getEnd() > this.range.getEnd()) result.append(command, this.range.getEnd(), range.getEnd());
        return new Suggestion(range, result.toString(), this.tooltip);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Suggestion)) return false;
        Suggestion that = (Suggestion) o;
        return this.range.equals(that.range) && this.text.equals(that.text) && java.util.Objects.equals(this.tooltip, that.tooltip);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(this.range, this.text, this.tooltip);
    }

    @Override
    public int compareTo(Suggestion o) {
        return this.text.compareTo(o.text);
    }

    public int compareToIgnoreCase(Suggestion b) {
        return this.text.compareToIgnoreCase(b.text);
    }
}
