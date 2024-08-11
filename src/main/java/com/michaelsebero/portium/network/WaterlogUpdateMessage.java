package com.michaelsebero.portium.network;

import com.michaelsebero.portium.Portium;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** One cell gaining or losing its water. */
public class WaterlogUpdateMessage implements IMessage {

    public BlockPos pos = BlockPos.ORIGIN;
    public boolean waterlogged;

    public WaterlogUpdateMessage() {}

    public WaterlogUpdateMessage(BlockPos pos, boolean waterlogged) {
        this.pos = pos;
        this.waterlogged = waterlogged;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = BlockPos.fromLong(buf.readLong());
        this.waterlogged = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.pos.toLong());
        buf.writeBoolean(this.waterlogged);
    }

    public static class Handler implements IMessageHandler<WaterlogUpdateMessage, IMessage> {

        @Override
        public IMessage onMessage(WaterlogUpdateMessage message, MessageContext context) {
            Portium.proxy.receiveWaterlogUpdate(message);
            return null;
        }
    }
}
