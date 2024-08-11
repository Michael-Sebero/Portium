package com.michaelsebero.portium.command.brigadier;

import com.michaelsebero.portium.command.TextMessage;

/** The parser's own errors, with 1.13's translation keys. Longs have no 1.13 keys, so their errors stay in English. */
public final class BuiltInExceptions {

    public static final CommandExceptionType.Dynamic2 DOUBLE_TOO_SMALL = new CommandExceptionType.Dynamic2((found, min) -> TextMessage.translate("argument.double.low", min, found));
    public static final CommandExceptionType.Dynamic2 DOUBLE_TOO_BIG = new CommandExceptionType.Dynamic2((found, max) -> TextMessage.translate("argument.double.big", max, found));
    public static final CommandExceptionType.Dynamic2 FLOAT_TOO_SMALL = new CommandExceptionType.Dynamic2((found, min) -> TextMessage.translate("argument.float.low", min, found));
    public static final CommandExceptionType.Dynamic2 FLOAT_TOO_BIG = new CommandExceptionType.Dynamic2((found, max) -> TextMessage.translate("argument.float.big", max, found));
    public static final CommandExceptionType.Dynamic2 INTEGER_TOO_SMALL = new CommandExceptionType.Dynamic2((found, min) -> TextMessage.translate("argument.integer.low", min, found));
    public static final CommandExceptionType.Dynamic2 INTEGER_TOO_BIG = new CommandExceptionType.Dynamic2((found, max) -> TextMessage.translate("argument.integer.big", max, found));
    public static final CommandExceptionType.Dynamic LITERAL_INCORRECT = dynamic("argument.literal.incorrect");
    public static final CommandExceptionType.Simple READER_EXPECTED_START_OF_QUOTE = simple("parsing.quote.expected.start");
    public static final CommandExceptionType.Simple READER_EXPECTED_END_OF_QUOTE = simple("parsing.quote.expected.end");
    public static final CommandExceptionType.Dynamic READER_INVALID_ESCAPE = dynamic("parsing.quote.escape");
    public static final CommandExceptionType.Dynamic READER_INVALID_BOOL = dynamic("parsing.bool.invalid");
    public static final CommandExceptionType.Dynamic READER_INVALID_INT = dynamic("parsing.int.invalid");
    public static final CommandExceptionType.Simple READER_EXPECTED_INT = simple("parsing.int.expected");
    public static final CommandExceptionType.Dynamic READER_INVALID_LONG = new CommandExceptionType.Dynamic(v -> Message.literal("Invalid long '" + v + "'"));
    public static final CommandExceptionType.Simple READER_EXPECTED_LONG = new CommandExceptionType.Simple(Message.literal("Expected long"));
    public static final CommandExceptionType.Dynamic READER_INVALID_DOUBLE = dynamic("parsing.double.invalid");
    public static final CommandExceptionType.Simple READER_EXPECTED_DOUBLE = simple("parsing.double.expected");
    public static final CommandExceptionType.Dynamic READER_INVALID_FLOAT = dynamic("parsing.float.invalid");
    public static final CommandExceptionType.Simple READER_EXPECTED_FLOAT = simple("parsing.float.expected");
    public static final CommandExceptionType.Simple READER_EXPECTED_BOOL = simple("parsing.bool.expected");
    public static final CommandExceptionType.Dynamic READER_EXPECTED_SYMBOL = dynamic("parsing.expected");
    public static final CommandExceptionType.Simple DISPATCHER_UNKNOWN_COMMAND = simple("command.unknown.command");
    public static final CommandExceptionType.Simple DISPATCHER_UNKNOWN_ARGUMENT = simple("command.unknown.argument");
    public static final CommandExceptionType.Simple DISPATCHER_EXPECTED_ARGUMENT_SEPARATOR = simple("command.expected.separator");
    public static final CommandExceptionType.Dynamic DISPATCHER_PARSE_EXCEPTION = dynamic("command.exception");

    private static CommandExceptionType.Simple simple(String key) {
        return new CommandExceptionType.Simple(TextMessage.translate(key));
    }

    private static CommandExceptionType.Dynamic dynamic(String key) {
        return new CommandExceptionType.Dynamic(arg -> TextMessage.translate(key, arg));
    }

    private BuiltInExceptions() {}
}
