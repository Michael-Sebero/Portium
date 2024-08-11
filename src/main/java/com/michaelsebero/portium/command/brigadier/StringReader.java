package com.michaelsebero.portium.command.brigadier;

/** A cursor over a command string, with the token readers 1.13's command parser uses. */
public class StringReader {

    private static final char ESCAPE = '\\';
    private final String string;
    private int cursor;

    public StringReader(String string) {
        this.string = string;
    }

    public StringReader(StringReader other) {
        this.string = other.string;
        this.cursor = other.cursor;
    }

    public String getString() {
        return this.string;
    }

    public int getCursor() {
        return this.cursor;
    }

    public void setCursor(int cursor) {
        this.cursor = cursor;
    }

    public int getRemainingLength() {
        return this.string.length() - this.cursor;
    }

    public int getTotalLength() {
        return this.string.length();
    }

    public String getRead() {
        return this.string.substring(0, this.cursor);
    }

    public String getRemaining() {
        return this.string.substring(this.cursor);
    }

    public boolean canRead(int length) {
        return this.cursor + length <= this.string.length();
    }

    public boolean canRead() {
        return this.canRead(1);
    }

    public char peek() {
        return this.string.charAt(this.cursor);
    }

    public char peek(int offset) {
        return this.string.charAt(this.cursor + offset);
    }

    public char read() {
        return this.string.charAt(this.cursor++);
    }

    public void skip() {
        this.cursor++;
    }

    public static boolean isAllowedNumber(char c) {
        return c >= '0' && c <= '9' || c == '.' || c == '-';
    }

    public static boolean isQuotedStringStart(char c) {
        return c == '"' || c == '\'';
    }

    public static boolean isAllowedInUnquotedString(char c) {
        return c >= '0' && c <= '9' || c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c == '_' || c == '-' || c == '.' || c == '+';
    }

    public void skipWhitespace() {
        while (this.canRead() && Character.isWhitespace(this.peek())) this.skip();
    }

    private String readNumberToken() {
        int start = this.cursor;
        while (this.canRead() && isAllowedNumber(this.peek())) this.skip();
        return this.string.substring(start, this.cursor);
    }

    public int readInt() throws CommandSyntaxException {
        int start = this.cursor;
        String number = this.readNumberToken();
        if (number.isEmpty()) throw BuiltInExceptions.READER_EXPECTED_INT.createWithContext(this);
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            this.cursor = start;
            throw BuiltInExceptions.READER_INVALID_INT.createWithContext(this, number);
        }
    }

    public long readLong() throws CommandSyntaxException {
        int start = this.cursor;
        String number = this.readNumberToken();
        if (number.isEmpty()) throw BuiltInExceptions.READER_EXPECTED_LONG.createWithContext(this);
        try {
            return Long.parseLong(number);
        } catch (NumberFormatException e) {
            this.cursor = start;
            throw BuiltInExceptions.READER_INVALID_LONG.createWithContext(this, number);
        }
    }

    public double readDouble() throws CommandSyntaxException {
        int start = this.cursor;
        String number = this.readNumberToken();
        if (number.isEmpty()) throw BuiltInExceptions.READER_EXPECTED_DOUBLE.createWithContext(this);
        try {
            return Double.parseDouble(number);
        } catch (NumberFormatException e) {
            this.cursor = start;
            throw BuiltInExceptions.READER_INVALID_DOUBLE.createWithContext(this, number);
        }
    }

    public float readFloat() throws CommandSyntaxException {
        int start = this.cursor;
        String number = this.readNumberToken();
        if (number.isEmpty()) throw BuiltInExceptions.READER_EXPECTED_FLOAT.createWithContext(this);
        try {
            return Float.parseFloat(number);
        } catch (NumberFormatException e) {
            this.cursor = start;
            throw BuiltInExceptions.READER_INVALID_FLOAT.createWithContext(this, number);
        }
    }

    public String readUnquotedString() {
        int start = this.cursor;
        while (this.canRead() && isAllowedInUnquotedString(this.peek())) this.skip();
        return this.string.substring(start, this.cursor);
    }

    public String readQuotedString() throws CommandSyntaxException {
        if (!this.canRead()) return "";
        char quote = this.peek();
        if (!isQuotedStringStart(quote)) throw BuiltInExceptions.READER_EXPECTED_START_OF_QUOTE.createWithContext(this);
        this.skip();
        return this.readStringUntil(quote);
    }

    public String readStringUntil(char terminator) throws CommandSyntaxException {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        while (this.canRead()) {
            char c = this.read();
            if (escaped) {
                if (c != terminator && c != ESCAPE) {
                    this.setCursor(this.getCursor() - 1);
                    throw BuiltInExceptions.READER_INVALID_ESCAPE.createWithContext(this, String.valueOf(c));
                }
                result.append(c);
                escaped = false;
            } else if (c == ESCAPE) {
                escaped = true;
            } else if (c == terminator) {
                return result.toString();
            } else {
                result.append(c);
            }
        }
        throw BuiltInExceptions.READER_EXPECTED_END_OF_QUOTE.createWithContext(this);
    }

    public String readString() throws CommandSyntaxException {
        if (!this.canRead()) return "";
        char next = this.peek();
        if (isQuotedStringStart(next)) {
            this.skip();
            return this.readStringUntil(next);
        }
        return this.readUnquotedString();
    }

    public boolean readBoolean() throws CommandSyntaxException {
        int start = this.cursor;
        String value = this.readString();
        if (value.isEmpty()) throw BuiltInExceptions.READER_EXPECTED_BOOL.createWithContext(this);
        if (value.equals("true")) return true;
        if (value.equals("false")) return false;
        this.cursor = start;
        throw BuiltInExceptions.READER_INVALID_BOOL.createWithContext(this, value);
    }

    public void expect(char c) throws CommandSyntaxException {
        if (!this.canRead() || this.peek() != c) throw BuiltInExceptions.READER_EXPECTED_SYMBOL.createWithContext(this, String.valueOf(c));
        this.skip();
    }
}
