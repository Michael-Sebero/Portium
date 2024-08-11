package com.michaelsebero.portium.network;

import com.michaelsebero.portium.Portium;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** What the chat screen shows for a typed command: argument colours, suggestions, and usage or error lines. */
public class CommandSuggestResponse implements IMessage {

    public int id;
    public String text = "";
    /** Start and end of each parsed argument, in order; the chat screen colours them in turn. */
    public int[] arguments = new int[0];
    /** The part that couldn't be parsed, shown in red, or -1. */
    public int errorStart = -1;
    public int errorEnd = -1;
    /** Suggestions are left out when only the colours were asked for. */
    public boolean hasSuggestions;
    public int suggestionStart;
    public final List<Entry> suggestions = new ArrayList<>();
    /** Lines shown above the input when there are no suggestions to show: errors, or the usage of what comes next. */
    public final List<ITextComponent> lines = new ArrayList<>();
    /** Where usage lines line up in the input, or -1 for errors, which span the screen. */
    public int usageStart = -1;

    public static final class Entry {

        public final int start;
        public final int end;
        public final String text;
        @Nullable
        public final ITextComponent tooltip;

        public Entry(int start, int end, String text, @Nullable ITextComponent tooltip) {
            this.start = start;
            this.end = end;
            this.text = text;
            this.tooltip = tooltip;
        }

        /** The input with this suggestion's range replaced by its text. */
        public String apply(String input) {
            if (this.start == 0 && this.end == input.length()) return this.text;
            StringBuilder result = new StringBuilder();
            if (this.start > 0) result.append(input, 0, Math.min(this.start, input.length()));
            result.append(this.text);
            if (this.end < input.length()) result.append(input.substring(this.end));
            return result.toString();
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.id = ByteBufUtils.readVarInt(buf, 5);
        this.text = ByteBufUtils.readUTF8String(buf);
        this.arguments = new int[ByteBufUtils.readVarInt(buf, 3)];
        for (int i = 0; i < this.arguments.length; i++) this.arguments[i] = ByteBufUtils.readVarInt(buf, 5);
        this.errorStart = buf.readInt();
        this.errorEnd = buf.readInt();
        this.hasSuggestions = buf.readBoolean();
        if (this.hasSuggestions) {
            this.suggestionStart = ByteBufUtils.readVarInt(buf, 5);
            int count = ByteBufUtils.readVarInt(buf, 5);
            for (int i = 0; i < count; i++) {
                int start = ByteBufUtils.readVarInt(buf, 5);
                int end = ByteBufUtils.readVarInt(buf, 5);
                String text = ByteBufUtils.readUTF8String(buf);
                ITextComponent tooltip = buf.readBoolean() ? readComponent(buf) : null;
                this.suggestions.add(new Entry(start, end, text, tooltip));
            }
        }
        int lines = ByteBufUtils.readVarInt(buf, 3);
        for (int i = 0; i < lines; i++) this.lines.add(readComponent(buf));
        this.usageStart = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeVarInt(buf, this.id, 5);
        ByteBufUtils.writeUTF8String(buf, this.text);
        ByteBufUtils.writeVarInt(buf, this.arguments.length, 3);
        for (int value : this.arguments) ByteBufUtils.writeVarInt(buf, value, 5);
        buf.writeInt(this.errorStart);
        buf.writeInt(this.errorEnd);
        buf.writeBoolean(this.hasSuggestions);
        if (this.hasSuggestions) {
            ByteBufUtils.writeVarInt(buf, this.suggestionStart, 5);
            ByteBufUtils.writeVarInt(buf, this.suggestions.size(), 5);
            for (Entry entry : this.suggestions) {
                ByteBufUtils.writeVarInt(buf, entry.start, 5);
                ByteBufUtils.writeVarInt(buf, entry.end, 5);
                ByteBufUtils.writeUTF8String(buf, entry.text);
                buf.writeBoolean(entry.tooltip != null);
                if (entry.tooltip != null) writeComponent(buf, entry.tooltip);
            }
        }
        ByteBufUtils.writeVarInt(buf, this.lines.size(), 3);
        for (ITextComponent line : this.lines) writeComponent(buf, line);
        buf.writeInt(this.usageStart);
    }

    private static ITextComponent readComponent(ByteBuf buf) {
        return ITextComponent.Serializer.jsonToComponent(ByteBufUtils.readUTF8String(buf));
    }

    private static void writeComponent(ByteBuf buf, ITextComponent component) {
        ByteBufUtils.writeUTF8String(buf, ITextComponent.Serializer.componentToJson(component));
    }

    public static class Handler implements IMessageHandler<CommandSuggestResponse, IMessage> {

        @Override
        public IMessage onMessage(CommandSuggestResponse message, MessageContext context) {
            Portium.proxy.receiveCommandSuggestions(message);
            return null;
        }
    }
}
