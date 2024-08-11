package com.michaelsebero.portium.command.brigadier;

/** Text for a command error or suggestion tooltip. Minecraft's text components implement it through a wrapper. */
public interface Message {

    String getString();

    static Message literal(String text) {
        return () -> text;
    }
}
