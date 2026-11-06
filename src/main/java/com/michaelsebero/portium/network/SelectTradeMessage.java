package com.michaelsebero.portium.network;

import com.michaelsebero.portium.merchant.ContainerMerchant116;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** 1.14's {@code CSelectTradePacket}: the trade picked on the trade screen. */
public class SelectTradeMessage implements IMessage {

    private int index;

    public SelectTradeMessage() {}

    public SelectTradeMessage(int index) {
        this.index = index;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.index = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.index);
    }

    public static class Handler implements IMessageHandler<SelectTradeMessage, IMessage> {
        @Override
        public IMessage onMessage(SelectTradeMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (!(player.openContainer instanceof ContainerMerchant116)) return;
                ContainerMerchant116 container = (ContainerMerchant116) player.openContainer;
                container.setCurrentRecipeIndex(message.index);
                container.fillPaymentFor(message.index);
            });
            return null;
        }
    }
}
