package com.michaelsebero.portium.network;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.modern.state.ModernStateData;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** A chunk's full states of 1.13 blocks with more than 16 states, sent after the chunk. */
public class ModernStateChunkMessage implements IMessage {

    public int chunkX;
    public int chunkZ;
    public ModernStateData data = new ModernStateData();

    public ModernStateChunkMessage() {}

    public ModernStateChunkMessage(int chunkX, int chunkZ, ModernStateData data) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.data = data;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.chunkX = buf.readInt();
        this.chunkZ = buf.readInt();
        this.data.read(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.chunkX);
        buf.writeInt(this.chunkZ);
        this.data.write(buf);
    }

    public static class Handler implements IMessageHandler<ModernStateChunkMessage, IMessage> {

        @Override
        public IMessage onMessage(ModernStateChunkMessage message, MessageContext context) {
            Portium.proxy.receiveModernStates(message);
            return null;
        }
    }
}
