package com.michaelsebero.portium.network;

import com.michaelsebero.portium.Portium;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Sent on login: what this Portium server can do for the client beyond 1.12's protocol. */
public class ServerFeaturesMessage implements IMessage {

    /** The server answers {@link CommandSuggestRequest}s. */
    public boolean commandHelp;

    public ServerFeaturesMessage() {}

    public ServerFeaturesMessage(boolean commandHelp) {
        this.commandHelp = commandHelp;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.commandHelp = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.commandHelp);
    }

    public static class Handler implements IMessageHandler<ServerFeaturesMessage, IMessage> {

        @Override
        public IMessage onMessage(ServerFeaturesMessage message, MessageContext context) {
            Portium.proxy.receiveServerFeatures(message);
            return null;
        }
    }
}
