package com.michaelsebero.portium.mods116.shim;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

import javax.annotation.Nullable;

/**
 * 1.16's server chunk provider, which is 1.12's {@code ChunkProviderServer} here: these answer the 1.16 calls 1.12's
 * lacks (Botania's flowers ask whether their chunk ticks; its block entities sync to the players tracking the chunk).
 */
public final class ChunkProviderShims116 {

    private static IChunkProvider of(Object provider) {
        return (IChunkProvider) provider;
    }

    @Nullable
    public static Chunk getChunk(Object provider, int x, int z, boolean load) {
        return load ? of(provider).provideChunk(x, z) : of(provider).getLoadedChunk(x, z);
    }

    @Nullable
    public static Chunk getChunkNow(Object provider, int x, int z) {
        return of(provider).getLoadedChunk(x, z);
    }

    public static boolean chunkExists(Object provider, int x, int z) {
        return of(provider).getLoadedChunk(x, z) != null;
    }

    /** {@code isChunkLoaded(entity)}: the entity's chunk is loaded (1.16: and ticking). */
    public static boolean entityChunkLoaded(Object provider, Entity entity) {
        return chunkExists(provider, MathHelper.floor(entity.posX) >> 4, MathHelper.floor(entity.posZ) >> 4);
    }

    public static boolean chunkLoaded(Object provider, ChunkPos pos) {
        return chunkExists(provider, pos.x, pos.z);
    }

    /** {@code canTick(pos)}: the chunk there is loaded. */
    public static boolean canTick(Object provider, BlockPos pos) {
        return chunkExists(provider, pos.getX() >> 4, pos.getZ() >> 4);
    }

    /**
     * 1.16's {@code serverChunkProvider.chunkManager} has no 1.12 counterpart: the glue gives code none (typed as 1.16's,
     * so code using it still verifies) and remembers the provider asked, for the call that follows on it.
     */
    private static final ThreadLocal<Object> LAST_PROVIDER = new ThreadLocal<>();

    public static void rememberProvider(Object provider) {
        LAST_PROVIDER.set(provider);
    }

    /**
     * 1.16's {@code chunkManager.getTrackingPlayers(chunk, onlyOnBorder)}: the players the chunk is sent to, by the
     * provider whose chunk manager was just asked for (Botania syncs its block entities to them).
     */
    public static java.util.stream.Stream<net.minecraft.entity.player.EntityPlayerMP> trackingPlayers(@Nullable Object manager, ChunkPos chunk, boolean onlyOnBorder) {
        Object provider = manager != null ? manager : LAST_PROVIDER.get();
        if (!(provider instanceof net.minecraft.world.gen.ChunkProviderServer)) return java.util.stream.Stream.empty();
        net.minecraft.server.management.PlayerChunkMapEntry entry = ((net.minecraft.world.gen.ChunkProviderServer) provider).world.getPlayerChunkMap().getEntry(chunk.x, chunk.z);
        return entry == null ? java.util.stream.Stream.empty() : entry.getWatchingPlayers().stream();
    }

    private ChunkProviderShims116() {}
}
