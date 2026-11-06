package com.michaelsebero.portium.network;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.merchant.MerchantOffers;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;

/** 1.14's {@code SMerchantOffersPacket}: an open trade screen's offers, the merchant's level and xp, and what to show. */
public class MerchantOffersMessage implements IMessage {

    private int windowId;
    private MerchantOffers offers = new MerchantOffers();
    private int level;
    private int xp;
    private boolean showProgress;
    private boolean canRestock;

    public MerchantOffersMessage() {}

    public MerchantOffersMessage(int windowId, MerchantOffers offers, int level, int xp, boolean showProgress, boolean canRestock) {
        this.windowId = windowId;
        this.offers = offers;
        this.level = level;
        this.xp = xp;
        this.showProgress = showProgress;
        this.canRestock = canRestock;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        this.windowId = buffer.readVarInt();
        try {
            this.offers = MerchantOffers.read(buffer);
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't read merchant offers", e);
        }
        this.level = buffer.readVarInt();
        this.xp = buffer.readVarInt();
        this.showProgress = buffer.readBoolean();
        this.canRestock = buffer.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeVarInt(this.windowId);
        this.offers.write(buffer);
        buffer.writeVarInt(this.level);
        buffer.writeVarInt(this.xp);
        buffer.writeBoolean(this.showProgress);
        buffer.writeBoolean(this.canRestock);
    }

    public int getWindowId() {
        return this.windowId;
    }

    public MerchantOffers getOffers() {
        return this.offers;
    }

    public int getLevel() {
        return this.level;
    }

    public int getXp() {
        return this.xp;
    }

    public boolean showProgress() {
        return this.showProgress;
    }

    public boolean canRestock() {
        return this.canRestock;
    }

    public static class Handler implements IMessageHandler<MerchantOffersMessage, IMessage> {
        @Override
        public IMessage onMessage(MerchantOffersMessage message, MessageContext ctx) {
            Portium.proxy.receiveMerchantOffers(message);
            return null;
        }
    }
}
