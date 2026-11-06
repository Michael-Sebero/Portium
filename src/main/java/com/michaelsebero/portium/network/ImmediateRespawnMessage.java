package com.michaelsebero.portium.network;

import com.michaelsebero.portium.Portium;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** 1.15's {@code doImmediateRespawn}: sent to a dying player before the death packet, so its client respawns at once. */
public class ImmediateRespawnMessage implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<ImmediateRespawnMessage, IMessage> {
        @Override
        public IMessage onMessage(ImmediateRespawnMessage message, MessageContext context) {
            Portium.proxy.respawnImmediately();
            return null;
        }
    }
}
