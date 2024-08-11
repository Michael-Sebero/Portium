package net.minecraftforge.fml.network;

import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

/**
 * Portium's replacement for Forge 1.13's: a payload on a named channel, as {@link NetworkDirection#buildPacket} makes
 * it. {@link #getThis} is the packet to send, a Forge 1.12 one on the channel's Forge 1.12 counterpart.
 */
public interface ICustomPacket<T extends Packet<?>> {

    PacketBuffer getInternalData();

    ResourceLocation getName();

    int getIndex();

    NetworkDirection getDirection();

    T getThis();
}
