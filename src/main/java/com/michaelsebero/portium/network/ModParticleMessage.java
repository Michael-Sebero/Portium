package com.michaelsebero.portium.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Particles of a newer mod's own type, sent as 1.13 and later send any particle: the type by name, where and how
 * spread, and the particle's data as its type writes it (1.12's particle packet names one of its own kinds).
 */
public class ModParticleMessage implements IMessage {

    public String type = "";
    public double x;
    public double y;
    public double z;
    public float deltaX;
    public float deltaY;
    public float deltaZ;
    public float speed;
    public int count;
    public boolean force;
    public byte[] data = new byte[0];

    public ModParticleMessage() {}

    public ModParticleMessage(String type, double x, double y, double z, float deltaX, float deltaY, float deltaZ, float speed, int count, boolean force, byte[] data) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.deltaZ = deltaZ;
        this.speed = speed;
        this.count = count;
        this.force = force;
        this.data = data;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.type = ByteBufUtils.readUTF8String(buf);
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.deltaX = buf.readFloat();
        this.deltaY = buf.readFloat();
        this.deltaZ = buf.readFloat();
        this.speed = buf.readFloat();
        this.count = buf.readInt();
        this.force = buf.readBoolean();
        int length = buf.readInt();
        if (length < 0 || length > buf.readableBytes()) throw new IllegalArgumentException("Bad particle data length " + length);
        this.data = new byte[length];
        buf.readBytes(this.data);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.type);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeFloat(this.deltaX);
        buf.writeFloat(this.deltaY);
        buf.writeFloat(this.deltaZ);
        buf.writeFloat(this.speed);
        buf.writeInt(this.count);
        buf.writeBoolean(this.force);
        buf.writeInt(this.data.length);
        buf.writeBytes(this.data);
    }

    public static class Handler implements IMessageHandler<ModParticleMessage, IMessage> {
        @Override
        public IMessage onMessage(ModParticleMessage message, MessageContext context) {
            com.michaelsebero.portium.client.ParticleRendering116.receive(message);
            return null;
        }
    }
}
