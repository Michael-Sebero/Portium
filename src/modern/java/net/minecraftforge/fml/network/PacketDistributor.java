package net.minecraftforge.fml.network;

import com.michaelsebero.portium.modern.ModernChannel;
import com.michaelsebero.portium.modern.ModernPackets;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/** Portium's replacement for Forge 1.13's: who a message goes to, sent through Forge 1.12's channels. */
public class PacketDistributor<T> {

    private enum Kind { PLAYER, DIMENSION, NEAR, ALL, SERVER, TRACKING_ENTITY, TRACKING_ENTITY_AND_SELF, TRACKING_CHUNK, NMLIST }

    public static final PacketDistributor<EntityPlayerMP> PLAYER = new PacketDistributor<>(Kind.PLAYER);
    public static final PacketDistributor<DimensionType> DIMENSION = new PacketDistributor<>(Kind.DIMENSION);
    public static final PacketDistributor<TargetPoint> NEAR = new PacketDistributor<>(Kind.NEAR);
    public static final PacketDistributor<Void> ALL = new PacketDistributor<>(Kind.ALL);
    public static final PacketDistributor<Void> SERVER = new PacketDistributor<>(Kind.SERVER);
    public static final PacketDistributor<Entity> TRACKING_ENTITY = new PacketDistributor<>(Kind.TRACKING_ENTITY);
    public static final PacketDistributor<Entity> TRACKING_ENTITY_AND_SELF = new PacketDistributor<>(Kind.TRACKING_ENTITY_AND_SELF);
    public static final PacketDistributor<Chunk> TRACKING_CHUNK = new PacketDistributor<>(Kind.TRACKING_CHUNK);
    public static final PacketDistributor<List<NetworkManager>> NMLIST = new PacketDistributor<>(Kind.NMLIST);

    private final Kind kind;

    private PacketDistributor(Kind kind) {
        this.kind = kind;
    }

    public PacketTarget with(Supplier<T> argument) {
        return new PacketTarget(this.kind, argument);
    }

    public PacketTarget noArg() {
        return new PacketTarget(this.kind, () -> null);
    }

    public static class PacketTarget {

        private final Kind kind;
        private final Supplier<?> argument;

        PacketTarget(Kind kind, Supplier<?> argument) {
            this.kind = kind;
            this.argument = argument;
        }

        public NetworkDirection getDirection() {
            return this.kind == Kind.SERVER ? NetworkDirection.PLAY_TO_SERVER : NetworkDirection.PLAY_TO_CLIENT;
        }

        /** A packet the mod built (with {@link NetworkDirection#buildPacket}, say) to the target's players, or the server. */
        public void send(Packet<?> packet) {
            Object argument = this.argument.get();
            switch (this.kind) {
                case PLAYER:
                    ModernPackets.toPlayer(packet, (EntityPlayerMP) argument);
                    break;
                case DIMENSION:
                    ModernPackets.toDimension(packet, ((DimensionType) argument).func_186068_a());
                    break;
                case NEAR: {
                    TargetPoint point = (TargetPoint) argument;
                    ModernPackets.near(packet, point.dimension.func_186068_a(), point.x, point.y, point.z, point.range, point.excluded);
                    break;
                }
                case ALL:
                    ModernPackets.toAll(packet);
                    break;
                case SERVER:
                    ModernPackets.toServer(packet);
                    break;
                case TRACKING_ENTITY:
                    ModernPackets.tracking(packet, (Entity) argument, false);
                    break;
                case TRACKING_ENTITY_AND_SELF:
                    ModernPackets.tracking(packet, (Entity) argument, true);
                    break;
                case TRACKING_CHUNK: {
                    Chunk chunk = (Chunk) argument;
                    if (chunk.func_177412_p() instanceof WorldServer) {
                        ModernPackets.trackingChunk(packet, (WorldServer) chunk.func_177412_p(), chunk.field_76635_g, chunk.field_76647_h);
                    }
                    break;
                }
                case NMLIST:
                    @SuppressWarnings("unchecked")
                    List<NetworkManager> managers = (List<NetworkManager>) argument;
                    for (NetworkManager manager : managers) ModernPackets.toConnection(packet, manager);
                    break;
                default:
                    throw new UnsupportedOperationException(String.valueOf(this.kind));
            }
        }

        /** For Portium's {@code SimpleChannel}: a payload to the target. */
        public void send(ModernChannel channel, PacketBuffer payload) {
            Object argument = this.argument.get();
            switch (this.kind) {
                case PLAYER:
                    channel.sendTo(payload, (EntityPlayerMP) argument);
                    break;
                case DIMENSION:
                    channel.sendToDimension(payload, ((DimensionType) argument).func_186068_a());
                    break;
                case NEAR: {
                    TargetPoint point = (TargetPoint) argument;
                    channel.sendToAllAround(payload, point.dimension.func_186068_a(), point.x, point.y, point.z, point.range, point.excluded);
                    break;
                }
                case ALL:
                    channel.sendToAll(payload);
                    break;
                case SERVER:
                    channel.sendToServer(payload);
                    break;
                case TRACKING_ENTITY:
                    channel.sendToTracking(payload, (Entity) argument, false);
                    break;
                case TRACKING_ENTITY_AND_SELF:
                    channel.sendToTracking(payload, (Entity) argument, true);
                    break;
                case TRACKING_CHUNK: {
                    Chunk chunk = (Chunk) argument;
                    if (chunk.func_177412_p() instanceof WorldServer) channel.sendToTrackingChunk(payload, (WorldServer) chunk.func_177412_p(), chunk.field_76635_g, chunk.field_76647_h);
                    break;
                }
                default:
                    throw new UnsupportedOperationException("Sending to network managers isn't supported");
            }
        }
    }

    public static final class TargetPoint {

        @Nullable
        final EntityPlayerMP excluded;
        final double x;
        final double y;
        final double z;
        final double range;
        final DimensionType dimension;

        public TargetPoint(@Nullable EntityPlayerMP excluded, double x, double y, double z, double range, DimensionType dimension) {
            this.excluded = excluded;
            this.x = x;
            this.y = y;
            this.z = z;
            this.range = range;
            this.dimension = dimension;
        }

        public TargetPoint(double x, double y, double z, double range, DimensionType dimension) {
            this(null, x, y, z, range, dimension);
        }

        public static Supplier<TargetPoint> p(double x, double y, double z, double range, DimensionType dimension) {
            TargetPoint point = new TargetPoint(x, y, z, range, dimension);
            return () -> point;
        }
    }
}
