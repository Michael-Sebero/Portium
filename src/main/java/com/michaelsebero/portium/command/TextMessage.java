package com.michaelsebero.portium.command;

import com.michaelsebero.portium.command.brigadier.Message;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

/** A command message that is a Minecraft text component, so players see it in their own language. */
public final class TextMessage implements Message {

    private final ITextComponent component;

    public TextMessage(ITextComponent component) {
        this.component = component;
    }

    public static TextMessage translate(String key, Object... args) {
        return new TextMessage(new TextComponentTranslation(key, args));
    }

    public ITextComponent getComponent() {
        return this.component;
    }

    @Override
    public String getString() {
        return this.component.getUnformattedText();
    }

    /** The text component for any message, wrapping plain ones. */
    public static ITextComponent toComponent(Message message) {
        return message instanceof TextMessage ? ((TextMessage) message).component : new net.minecraft.util.text.TextComponentString(message.getString());
    }
}
