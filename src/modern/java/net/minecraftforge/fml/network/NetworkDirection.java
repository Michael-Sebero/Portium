package net.minecraftforge.fml.network;

import com.michaelsebero.portium.modern.ModernChannel;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.LogicalSide;
import org.apache.commons.lang3.tuple.Pair;

/** Portium's replacement for Forge 1.13's: which way a message goes (see {@link net.minecraftforge.fml.network.simple.SimpleChannel}). */
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
            case PLAY_TO_SERVER: return PLAY_TO_CLIENT;
            case PLAY_TO_CLIENT: return PLAY_TO_SERVER;
            case LOGIN_TO_SERVER: return LOGIN_TO_CLIENT;
            default: return LOGIN_TO_SERVER;
        }
    }

    public LogicalSide getOriginationSide() {
        return this.origin;
    }

    public LogicalSide getReceptionSide() {
        return this.reception;
    }

    /** A payload as a packet on a channel (its index only matters to login messages, which Portium doesn't send). */
    public <T extends Packet<?>> ICustomPacket<T> buildPacket(Pair<PacketBuffer, Integer> data, ResourceLocation channel) {
        NetworkDirection direction = this;
        PacketBuffer payload = data.getLeft();
        int index = data.getRight() == null ? Integer.MIN_VALUE : data.getRight();
        @SuppressWarnings("unchecked")
        T packet = (T) ModernChannel.vanillaPacket(channel, payload);
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
