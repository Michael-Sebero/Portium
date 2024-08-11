package com.michaelsebero.portium.command.brigadier;

/** A command that couldn't be parsed or run, with where in the input it went wrong. */
public class CommandSyntaxException extends Exception {

    public static final int CONTEXT_AMOUNT = 10;

    private final CommandExceptionType type;
    private final Message message;
    private final String input;
    private final int cursor;

    public CommandSyntaxException(CommandExceptionType type, Message message) {
        this(type, message, null, -1);
    }

    public CommandSyntaxException(CommandExceptionType type, Message message, String input, int cursor) {
        super(message.getString(), null, false, false);
        this.type = type;
        this.message = message;
        this.input = input;
        this.cursor = cursor;
    }

    @Override
    public String getMessage() {
        String message = this.message.getString();
        String context = this.getContext();
        return context == null ? message : message + " at position " + this.cursor + ": " + context;
    }

    public Message getRawMessage() {
        return this.message;
    }

    /** The last few characters of input before the error, then {@code <--[HERE]}, as 1.13 shows it. */
    public String getContext() {
        if (this.input == null || this.cursor < 0) return null;
        StringBuilder builder = new StringBuilder();
        int cursor = Math.min(this.input.length(), this.cursor);
        if (cursor > CONTEXT_AMOUNT) builder.append("...");
        builder.append(this.input, Math.max(0, cursor - CONTEXT_AMOUNT), cursor);
        builder.append("<--[HERE]");
        return builder.toString();
    }

    public CommandExceptionType getType() {
        return this.type;
    }

    public String getInput() {
        return this.input;
    }

    public int getCursor() {
        return this.cursor;
    }
}
