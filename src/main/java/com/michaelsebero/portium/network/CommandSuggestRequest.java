package com.michaelsebero.portium.network;

import com.michaelsebero.portium.command.ChatSuggestions;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;

/** A command being typed in chat, for the server to colour, complete and explain. See {@link ChatSuggestions}. */
public class CommandSuggestRequest implements IMessage {

    /** Chat input is at most 256 characters. */
    private static final int MAX_LENGTH = 256;

    public int id;
    public String text = "";
    public int cursor;
    /** Whether to work out suggestions too, or only the colours (while a suggestion is being applied). */
    public boolean wantSuggestions;
    @Nullable
    public BlockPos targetBlock;
    @Nullable
    public String targetEntity;

    public CommandSuggestRequest() {}

    public CommandSuggestRequest(int id, String text, int cursor, boolean wantSuggestions, @Nullable BlockPos targetBlock, @Nullable String targetEntity) {
        this.id = id;
        this.text = text;
        this.cursor = cursor;
        this.wantSuggestions = wantSuggestions;
        this.targetBlock = targetBlock;
        this.targetEntity = targetEntity;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.id = ByteBufUtils.readVarInt(buf, 5);
        this.text = ByteBufUtils.readUTF8String(buf);
        if (this.text.length() > MAX_LENGTH) this.text = this.text.substring(0, MAX_LENGTH);
        this.cursor = Math.max(0, Math.min(ByteBufUtils.readVarInt(buf, 5), this.text.length()));
        this.wantSuggestions = buf.readBoolean();
        this.targetBlock = buf.readBoolean() ? BlockPos.fromLong(buf.readLong()) : null;
        this.targetEntity = buf.readBoolean() ? ByteBufUtils.readUTF8String(buf) : null;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeVarInt(buf, this.id, 5);
        ByteBufUtils.writeUTF8String(buf, this.text);
        ByteBufUtils.writeVarInt(buf, this.cursor, 5);
        buf.writeBoolean(this.wantSuggestions);
        buf.writeBoolean(this.targetBlock != null);
        if (this.targetBlock != null) buf.writeLong(this.targetBlock.toLong());
        buf.writeBoolean(this.targetEntity != null);
        if (this.targetEntity != null) ByteBufUtils.writeUTF8String(buf, this.targetEntity);
    }

    public static class Handler implements IMessageHandler<CommandSuggestRequest, IMessage> {

        @Override
        public IMessage onMessage(CommandSuggestRequest message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> PortiumNetwork.CHANNEL.sendTo(ChatSuggestions.respond(player, message), player));
            return null;
        }
    }
}
