package com.michaelsebero.portium.network;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.proxy.ParticleKind;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Spawns one of Portium's particles on the client, laid out as 1.12's particle packet does: with a count of zero,
 * one particle moving by {@code speed * delta}; otherwise {@code count} particles spread by {@code delta}.
 */
public class ParticleMessage implements IMessage {

    public ParticleKind kind;
    public double x, y, z;
    public float deltaX, deltaY, deltaZ;
    public float speed;
    public int count;

    public ParticleMessage() {}

    public ParticleMessage(ParticleKind kind, double x, double y, double z, float deltaX, float deltaY, float deltaZ, float speed, int count) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.z = z;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.deltaZ = deltaZ;
        this.speed = speed;
        this.count = count;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.kind = ParticleKind.values()[buf.readByte()];
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.deltaX = buf.readFloat();
        this.deltaY = buf.readFloat();
        this.deltaZ = buf.readFloat();
        this.speed = buf.readFloat();
        this.count = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(this.kind.ordinal());
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeFloat(this.deltaX);
        buf.writeFloat(this.deltaY);
        buf.writeFloat(this.deltaZ);
        buf.writeFloat(this.speed);
        buf.writeInt(this.count);
    }

    public static class Handler implements IMessageHandler<ParticleMessage, IMessage> {

        @Override
        public IMessage onMessage(ParticleMessage message, MessageContext context) {
            Portium.proxy.spawnParticles(message);
            return null;
        }
    }
}
