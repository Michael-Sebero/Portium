package net.minecraftforge.fml.network;

import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.LogicalSide;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Portium's replacement for Forge 1.16's: which way a message goes. Forge 1.16's network stack doesn't run on 1.12;
 * Portium's {@code SimpleChannel} sends through Forge 1.12's channels (ModernNetwork116).
 */
public enum NetworkDirection {
    PLAY_TO_SERVER(LogicalSide.CLIENT, LogicalSide.SERVER),
    PLAY_TO_CLIENT(LogicalSide.SERVER, LogicalSide.CLIENT),
    LOGIN_TO_SERVER(LogicalSide.CLIENT, LogicalSide.SERVER),
    LOGIN_TO_CLIENT(LogicalSide.SERVER, LogicalSide.CLIENT);

    private final LogicalSide origin;
    private final LogicalSide reception;

    NetworkDirection(LogicalSide origin, LogicalSide reception) {
        this.origin = origin;
        this.reception = reception;
    }

    public NetworkDirection reply() {
        switch (this) {
            case PLAY_TO_SERVER:
                return PLAY_TO_CLIENT;
            case PLAY_TO_CLIENT:
                return PLAY_TO_SERVER;
            case LOGIN_TO_SERVER:
                return LOGIN_TO_CLIENT;
            default:
                return LOGIN_TO_SERVER;
        }
    }

    public LogicalSide getOriginationSide() {
        return this.origin;
    }

    public LogicalSide getReceptionSide() {
        return this.reception;
    }

    /** A payload as a packet on a channel (the index only matters to login messages, which Portium doesn't send). */
    @SuppressWarnings("unchecked")
    public <T extends IPacket<?>> ICustomPacket<T> buildPacket(Pair<PacketBuffer, Integer> data, ResourceLocation channel) {
        NetworkDirection direction = this;
        PacketBuffer payload = data.getLeft();
        int index = data.getRight() == null ? Integer.MIN_VALUE : data.getRight();
        T packet = (T) com.michaelsebero.portium.mods116.ModernNetwork116.vanillaPacket(channel, payload);
        return new ICustomPacket<T>() {
            @Override
            public PacketBuffer getInternalData() {
                return payload;
            }

            @Override
            public ResourceLocation getName() {
                return channel;
            }

            @Override
            public int getIndex() {
                return index;
            }

            @Override
            public NetworkDirection getDirection() {
                return direction;
            }

            @Override
            public T getThis() {
                return packet;
            }
        };
    }
}
