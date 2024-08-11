package com.michaelsebero.portium.command.brigadier;

import java.util.function.Function;

/** A kind of command error, which creates exceptions with or without input context. */
public interface CommandExceptionType {

    /** An error with a fixed message. */
    final class Simple implements CommandExceptionType {

        private final Message message;

        public Simple(Message message) {
            this.message = message;
        }

        public CommandSyntaxException create() {
            return new CommandSyntaxException(this, this.message);
        }

        public CommandSyntaxException createWithContext(StringReader reader) {
            return new CommandSyntaxException(this, this.message, reader.getString(), reader.getCursor());
        }
    }

    /** An error whose message depends on one value. */
    final class Dynamic implements CommandExceptionType {

        private final Function<Object, Message> function;

        public Dynamic(Function<Object, Message> function) {
            this.function = function;
        }

        public CommandSyntaxException create(Object arg) {
            return new CommandSyntaxException(this, this.function.apply(arg));
        }

        public CommandSyntaxException createWithContext(StringReader reader, Object arg) {
            return new CommandSyntaxException(this, this.function.apply(arg), reader.getString(), reader.getCursor());
        }
    }

    /** An error whose message depends on two values. */
    final class Dynamic2 implements CommandExceptionType {

        public interface Function2 {
            Message apply(Object a, Object b);
        }

        private final Function2 function;

        public Dynamic2(Function2 function) {
            this.function = function;
        }

        public CommandSyntaxException create(Object a, Object b) {
            return new CommandSyntaxException(this, this.function.apply(a, b));
        }

        public CommandSyntaxException createWithContext(StringReader reader, Object a, Object b) {
            return new CommandSyntaxException(this, this.function.apply(a, b), reader.getString(), reader.getCursor());
        }
    }
}
