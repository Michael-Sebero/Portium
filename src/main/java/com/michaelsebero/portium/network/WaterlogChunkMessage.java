package com.michaelsebero.portium.network;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.world.waterlog.WaterlogData;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** A chunk's waterlogged cells, sent along with the chunk. */
public class WaterlogChunkMessage implements IMessage {

    public int chunkX;
    public int chunkZ;
    public WaterlogData data = new WaterlogData();

    public WaterlogChunkMessage() {}

    public WaterlogChunkMessage(int chunkX, int chunkZ, WaterlogData data) {
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

    public static class Handler implements IMessageHandler<WaterlogChunkMessage, IMessage> {

        @Override
        public IMessage onMessage(WaterlogChunkMessage message, MessageContext context) {
            Portium.proxy.receiveWaterlogChunk(message);
            return null;
        }
    }
}
