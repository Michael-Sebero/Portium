package net.minecraftforge.fml.network;

import com.michaelsebero.portium.mods116.ModernNetwork116;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/** Portium's replacement for Forge 1.16's: who a message goes to, sent through Forge 1.12's channels. */
public class PacketDistributor<T> {

    private enum Kind { PLAYER, DIMENSION, NEAR, ALL, SERVER, TRACKING_ENTITY, TRACKING_ENTITY_AND_SELF, TRACKING_CHUNK, NMLIST }

    public static final PacketDistributor<ServerPlayerEntity> PLAYER = new PacketDistributor<>(Kind.PLAYER);
    public static final PacketDistributor<RegistryKey<World>> DIMENSION = new PacketDistributor<>(Kind.DIMENSION);
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

        /** A packet the mod built itself: to a player or everyone (the targets mods send ready packets to). */
        public void send(IPacket<?> packet) {
            Object argument = this.argument.get();
            switch (this.kind) {
                case PLAYER:
                    ((ServerPlayerEntity) argument).field_71135_a.func_147359_a(packet);
                    break;
                case ALL:
                    net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().func_184103_al().func_148540_a(packet);
                    break;
                default:
                    throw new UnsupportedOperationException("Portium sends ready-made packets only to a player or everyone, not " + this.kind);
            }
        }

        /** For Portium's {@code SimpleChannel}: a payload to the target. */
        @SuppressWarnings("unchecked")
        public void send(Object channel, PacketBuffer payload) {
            Object argument = this.argument.get();
            switch (this.kind) {
                case PLAYER:
                    ModernNetwork116.toPlayer(channel, payload, argument);
                    break;
                case DIMENSION:
                    ModernNetwork116.toDimension(channel, payload, ((RegistryKey<World>) argument).func_240901_a_().toString());
                    break;
                case NEAR: {
                    TargetPoint point = (TargetPoint) argument;
                    ModernNetwork116.near(channel, payload, point.dimension.func_240901_a_().toString(), point.x, point.y, point.z, point.range, point.excluded);
                    break;
                }
                case ALL:
                    ModernNetwork116.toAll(channel, payload);
                    break;
                case SERVER:
                    ModernNetwork116.toServer(channel, payload);
                    break;
                case TRACKING_ENTITY:
                    ModernNetwork116.tracking(channel, payload, argument, false);
                    break;
                case TRACKING_ENTITY_AND_SELF:
                    ModernNetwork116.tracking(channel, payload, argument, true);
                    break;
                case TRACKING_CHUNK:
                    ModernNetwork116.trackingChunk(channel, payload, argument);
                    break;
                case NMLIST:
                    for (NetworkManager manager : (List<NetworkManager>) argument) ModernNetwork116.toConnection(channel, payload, manager);
                    break;
                default:
                    throw new UnsupportedOperationException(String.valueOf(this.kind));
            }
        }
    }

    public static final class TargetPoint {

        @Nullable
        final ServerPlayerEntity excluded;
        final double x;
        final double y;
        final double z;
        final double range;
        final RegistryKey<World> dimension;

        public TargetPoint(@Nullable ServerPlayerEntity excluded, double x, double y, double z, double range, RegistryKey<World> dimension) {
            this.excluded = excluded;
            this.x = x;
            this.y = y;
            this.z = z;
            this.range = range;
            this.dimension = dimension;
        }

        public TargetPoint(double x, double y, double z, double range, RegistryKey<World> dimension) {
            this(null, x, y, z, range, dimension);
        }

        public static Supplier<TargetPoint> p(double x, double y, double z, double range, RegistryKey<World> dimension) {
            TargetPoint point = new TargetPoint(x, y, z, range, dimension);
            return () -> point;
        }
    }
}
