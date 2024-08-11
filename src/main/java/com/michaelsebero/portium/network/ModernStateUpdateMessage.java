package com.michaelsebero.portium.network;

import com.michaelsebero.portium.Portium;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** One cell's full state of a 1.13 block with more than 16 states. */
public class ModernStateUpdateMessage implements IMessage {

    public BlockPos pos = BlockPos.ORIGIN;
    public int index;

    public ModernStateUpdateMessage() {}

    public ModernStateUpdateMessage(BlockPos pos, int index) {
        this.pos = pos;
        this.index = index;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = BlockPos.fromLong(buf.readLong());
        this.index = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.pos.toLong());
        buf.writeInt(this.index);
    }

    public static class Handler implements IMessageHandler<ModernStateUpdateMessage, IMessage> {

        @Override
        public IMessage onMessage(ModernStateUpdateMessage message, MessageContext context) {
            Portium.proxy.receiveModernState(message);
            return null;
        }
    }
}
