package com.michaelsebero.portium.mods116.shim;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldEventListener;
import net.minecraft.world.World;
import net.minecraftforge.event.AttachCapabilitiesEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Forge 1.16's {@code AttachCapabilitiesEvent.addListener}: work to run when the capabilities just attached are
 * invalidated, as Forge 1.16 does when the entity leaves its world. 1.12 has no such step, so Portium runs an
 * entity's listeners as it's removed from its world (through a world listener on each world).
 */
public final class CapabilityShims116 {

    private static final Map<Object, List<Runnable>> LISTENERS = Collections.synchronizedMap(new WeakHashMap<>());

    public static void addListener(AttachCapabilitiesEvent<?> event, Runnable listener) {
        LISTENERS.computeIfAbsent(event.getObject(), key -> new ArrayList<>()).add(listener);
    }

    /** Runs and forgets the listeners for an object whose capabilities are gone. */
    static void invalidate(Object owner) {
        List<Runnable> listeners = LISTENERS.remove(owner);
        if (listeners != null) for (Runnable listener : listeners) listener.run();
    }

    /** Watches a world for entities leaving it. */
    public static void watch(World world) {
        world.addEventListener(new Removals());
    }

    private static final class Removals implements IWorldEventListener {

        @Override
        public void onEntityRemoved(Entity entity) {
            invalidate(entity);
        }

        @Override
        public void notifyBlockUpdate(World world, BlockPos pos, IBlockState oldState, IBlockState newState, int flags) {}

        @Override
        public void notifyLightSet(BlockPos pos) {}

        @Override
        public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {}

        @Override
        public void playSoundToAllNearExcept(@Nullable EntityPlayer player, SoundEvent sound, SoundCategory category, double x, double y, double z, float volume,
                                             float pitch) {}

        @Override
        public void playRecord(SoundEvent sound, BlockPos pos) {}

        @Override
        public void spawnParticle(int particle, boolean ignoreRange, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int... parameters) {}

        @Override
        public void spawnParticle(int id, boolean ignoreRange, boolean minimiseParticleLevel, double x, double y, double z, double xSpeed, double ySpeed,
                                  double zSpeed, int... parameters) {}

        @Override
        public void onEntityAdded(Entity entity) {}

        @Override
        public void broadcastSound(int sound, BlockPos pos, int data) {}

        @Override
        public void playEvent(EntityPlayer player, int type, BlockPos pos, int data) {}

        @Override
        public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {}
    }

    private CapabilityShims116() {}
}
