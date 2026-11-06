package com.michaelsebero.portium.network;

import com.michaelsebero.portium.Portium;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Whether a map is locked (1.14), which 1.16 sends in its map packet and 1.12's packet has no room for. */
public class MapLockMessage implements IMessage {

    public int mapId;
    public boolean locked;

    public MapLockMessage() {}

    public MapLockMessage(int mapId, boolean locked) {
        this.mapId = mapId;
        this.locked = locked;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.mapId = buf.readInt();
        this.locked = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.mapId);
        buf.writeBoolean(this.locked);
    }

    public static class Handler implements IMessageHandler<MapLockMessage, IMessage> {

        @Override
        public IMessage onMessage(MapLockMessage message, MessageContext context) {
            Portium.proxy.receiveMapLock(message);
            return null;
        }
    }
}
