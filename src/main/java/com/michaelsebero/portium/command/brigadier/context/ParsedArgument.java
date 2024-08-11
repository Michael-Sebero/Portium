package com.michaelsebero.portium.command.brigadier.context;

public class ParsedArgument<S, T> {

    private final StringRange range;
    private final T result;

    public ParsedArgument(int start, int end, T result) {
        this.range = StringRange.between(start, end);
        this.result = result;
    }

    public StringRange getRange() {
        return this.range;
    }

    public T getResult() {
        return this.result;
    }
}
