package com.michaelsebero.portium.modern.shim;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.command.arguments.ParticleArgument;
import com.michaelsebero.portium.modern.mc.particles.BlockParticleData;
import com.michaelsebero.portium.modern.mc.particles.IParticleData;
import com.michaelsebero.portium.modern.mc.particles.ItemParticleData;
import com.michaelsebero.portium.modern.mc.particles.RedstoneParticleData;
import com.michaelsebero.portium.network.PortiumParticles;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;

/**
 * 1.13's particle methods on worlds, which take a particle with its arguments ({@link IParticleData}) where 1.12's
 * take an {@code EnumParticleTypes} and numbers. Particles 1.12 lacks are Portium's own; particles from types neither
 * knows are dropped.
 */
public final class ParticleShims {

    /** {@code World.addParticle}: shows a particle, on the client. */
    public static void addParticle(World world, IParticleData data, double x, double y, double z, double motionX, double motionY, double motionZ) {
        addParticle(world, data, false, x, y, z, motionX, motionY, motionZ);
    }

    /** {@code World.addParticle} that may show the particle however far away it is. */
    public static void addParticle(World world, IParticleData data, boolean force, double x, double y, double z, double motionX, double motionY,
                                   double motionZ) {
        ParticleArgument.Particle particle = resolve(data);
        if (particle == null) return;
        if (particle.portium != null) {
            if (world.isRemote) Portium.proxy.spawnParticle(particle.portium, world, x, y, z, motionX, motionY, motionZ);
        } else if (particle.color != null) {
            // 1.12 colours dust by its motion, and treats a red of 0 as 1.
            world.spawnParticle(particle.vanilla, force, x, y, z, particle.color[0] == 0.0F ? Float.MIN_VALUE : particle.color[0], particle.color[1],
                    particle.color[2], particle.arguments);
        } else {
            world.spawnParticle(particle.vanilla, force || data.getType().getAlwaysShow(), x, y, z, motionX, motionY, motionZ, particle.arguments);
        }
    }

    /** {@code World.addOptionalParticle}: a particle that shows even at the "minimal" particle setting. */
    public static void addOptionalParticle(World world, IParticleData data, double x, double y, double z, double motionX, double motionY, double motionZ) {
        ParticleArgument.Particle particle = resolve(data);
        if (particle == null) return;
        if (particle.vanilla != null && particle.color == null) {
            world.spawnAlwaysVisibleParticle(particle.vanilla.getParticleID(), x, y, z, motionX, motionY, motionZ, particle.arguments);
        } else {
            addParticle(world, data, x, y, z, motionX, motionY, motionZ);
        }
    }

    /** {@code WorldServer.spawnParticle}: sends particles to the players close enough to see them, returning how many. */
    public static int spawnParticle(WorldServer world, IParticleData data, double x, double y, double z, int count, double deltaX, double deltaY,
                                    double deltaZ, double speed) {
        ParticleArgument.Particle particle = resolve(data);
        if (particle == null) return 0;
        int sent = 0;
        for (EntityPlayer player : world.playerEntities) {
            if (player instanceof EntityPlayerMP && PortiumParticles.canSee((EntityPlayerMP) player, x, y, z, false)) {
                PortiumParticles.send((EntityPlayerMP) player, particle, x, y, z, deltaX, deltaY, deltaZ, (float) speed, count, false);
                sent++;
            }
        }
        return sent;
    }

    /** {@code WorldServer.spawnParticle} for one player; returns whether they were close enough. */
    public static boolean spawnParticle(WorldServer world, EntityPlayerMP player, IParticleData data, boolean force, double x, double y, double z, int count,
                                        double deltaX, double deltaY, double deltaZ, double speed) {
        ParticleArgument.Particle particle = resolve(data);
        if (particle == null || player.getServerWorld() != world || !PortiumParticles.canSee(player, x, y, z, force)) return false;
        PortiumParticles.send(player, particle, x, y, z, deltaX, deltaY, deltaZ, (float) speed, count, force);
        return true;
    }

    @Nullable
    private static ParticleArgument.Particle resolve(IParticleData data) {
        String name = data.getType().getId().getPath();
        int[] arguments = new int[0];
        float[] color = null;
        if (data instanceof ItemParticleData) {
            ItemStack stack = ((ItemParticleData) data).getItemStack();
            arguments = new int[] {Item.getIdFromItem(stack.getItem()), stack.getMetadata()};
        } else if (data instanceof BlockParticleData) {
            arguments = new int[] {Block.getStateId(((BlockParticleData) data).getBlockState())};
        } else if (data instanceof RedstoneParticleData) {
            RedstoneParticleData dust = (RedstoneParticleData) data;
            color = new float[] {dust.getRed(), dust.getGreen(), dust.getBlue()};
        }
        return ParticleArgument.byName(name, arguments, color, data.getParameters());
    }

    private ParticleShims() {}
}
