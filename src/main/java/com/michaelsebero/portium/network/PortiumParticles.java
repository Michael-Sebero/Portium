package com.michaelsebero.portium.network;

import com.michaelsebero.portium.command.arguments.ParticleArgument;
import com.michaelsebero.portium.proxy.ParticleKind;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketParticles;
import net.minecraft.world.WorldServer;

import java.util.Random;

/** Sends Portium's particles to the players who would see vanilla particles at that spot (within 32 blocks, or 512 when forced). */
public final class PortiumParticles {

    /** Returns how many players were sent the particles. */
    public static int spawn(WorldServer world, ParticleKind kind, double x, double y, double z, double deltaX, double deltaY, double deltaZ, double speed,
                            int count, boolean force) {
        ParticleMessage message = new ParticleMessage(kind, x, y, z, (float) deltaX, (float) deltaY, (float) deltaZ, (float) speed, count);
        int sent = 0;
        for (EntityPlayerMP player : world.getPlayers(EntityPlayerMP.class, player -> true)) {
            if (canSee(player, x, y, z, force)) {
                PortiumNetwork.CHANNEL.sendTo(message, player);
                sent++;
            }
        }
        return sent;
    }

    /** Sends one player a particle named the 1.13 way, as {@code /particle} does. */
    public static void send(EntityPlayerMP player, ParticleArgument.Particle particle, double x, double y, double z, double deltaX, double deltaY, double deltaZ,
                            float speed, int count, boolean force) {
        if (particle.portium != null) {
            PortiumNetwork.CHANNEL.sendTo(new ParticleMessage(particle.portium, x, y, z, (float) deltaX, (float) deltaY, (float) deltaZ, speed, count), player);
        } else if (particle.color != null) {
            // 1.12 colours redstone dust by the motion of a single particle, and treats a red of 0 as 1.
            float red = particle.color[0] == 0.0F ? Float.MIN_VALUE : particle.color[0];
            Random random = player.getRNG();
            for (int i = 0; i < Math.max(count, 1); i++) {
                double spreadX = count == 0 ? x : x + random.nextGaussian() * deltaX;
                double spreadY = count == 0 ? y : y + random.nextGaussian() * deltaY;
                double spreadZ = count == 0 ? z : z + random.nextGaussian() * deltaZ;
                player.connection.sendPacket(new SPacketParticles(particle.vanilla, force, (float) spreadX, (float) spreadY, (float) spreadZ,
                        red, particle.color[1], particle.color[2], 1.0F, 0, particle.arguments));
            }
        } else {
            player.connection.sendPacket(new SPacketParticles(particle.vanilla, force, (float) x, (float) y, (float) z,
                    (float) deltaX, (float) deltaY, (float) deltaZ, speed, count, particle.arguments));
        }
    }

    public static boolean canSee(EntityPlayerMP player, double x, double y, double z, boolean force) {
        double distance = player.getPosition().distanceSq(x, y, z);
        return distance <= 1024.0D || force && distance <= 262144.0D;
    }

    private PortiumParticles() {}
}
