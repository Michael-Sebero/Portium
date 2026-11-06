package com.michaelsebero.portium.world.v116;

import net.minecraft.block.state.IBlockState;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import com.michaelsebero.portium.Tags;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Light from what decoration places (glowstone, lava, fire). 1.12 works a placed block's light out only once the
 * chunks 16 blocks around it are loaded, and a chunk is often decorated before they are in a freshly reached area, so
 * glowstone near a Nether portal hung there unlit (1.16 lights a chunk before sending it). After a chunk's
 * decoration its light sources are lit at once where they can be, and the rest once the chunks around them load; a
 * chunk already sent to players is sent again with its light.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class GenerationLight {

    /** Light sources still to be lit, by world, oldest first, with the tick they were found at. */
    private static final Map<World, Deque<long[]>> PENDING = new WeakHashMap<>();
    /** Pending ones give up after this long (their surroundings may never load). */
    private static final int GIVE_UP_TICKS = 20 * 60 * 5;
    private static final int PER_TICK = 2048;

    /** After decorating the area 1.12 populates for chunk (x, z): its light sources, lit now or later. */
    public static void afterDecoration(World world, int chunkX, int chunkZ) {
        if (world.isRemote) return;
        int x0 = chunkX * 16 + 8;
        int z0 = chunkZ * 16 + 8;
        int top = world.getActualHeight();
        Deque<long[]> pending = null;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                for (int y = 0; y < top; y++) {
                    pos.setPos(x, y, z);
                    IBlockState state = world.getBlockState(pos);
                    if (state.getLightValue(world, pos) <= 0 || !exposed(world, pos)) continue;
                    if (world.isAreaLoaded(pos, 17, false)) {
                        world.checkLightFor(EnumSkyBlock.BLOCK, pos);
                    } else {
                        if (pending == null) pending = PENDING.computeIfAbsent(world, key -> new ArrayDeque<>());
                        pending.add(new long[] {pos.toLong(), world.getTotalWorldTime()});
                    }
                }
            }
        }
    }

    /** Whether light can leave the block: a neighbour lets some through (lava inside a lava sea can't). */
    private static boolean exposed(World world, BlockPos pos) {
        for (EnumFacing facing : EnumFacing.values()) {
            BlockPos next = pos.offset(facing);
            if (next.getY() < 0 || next.getY() >= world.getHeight()) continue;
            IBlockState neighbour = world.getBlockState(next);
            if (neighbour.getLightOpacity(world, next) < 15 && neighbour.getLightValue(world, next) == 0) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void worldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) return;
        Deque<long[]> pending = PENDING.get(event.world);
        if (pending == null || pending.isEmpty()) return;
        World world = event.world;
        long now = world.getTotalWorldTime();
        Set<ChunkPos> changed = new HashSet<>();
        int left = Math.min(PER_TICK, pending.size());
        for (int i = 0; i < left; i++) {
            long[] entry = pending.poll();
            BlockPos pos = BlockPos.fromLong(entry[0]);
            if (!world.isBlockLoaded(pos, false)) continue;
            if (world.isAreaLoaded(pos, 17, false)) {
                if (world.checkLightFor(EnumSkyBlock.BLOCK, pos)) {
                    // Light reaches 15 blocks: the chunks around it may have changed too.
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) changed.add(new ChunkPos((pos.getX() >> 4) + dx, (pos.getZ() >> 4) + dz));
                    }
                }
            } else if (now - entry[1] < GIVE_UP_TICKS) {
                pending.add(entry);
            }
        }
        if (!changed.isEmpty() && world instanceof WorldServer) resend((WorldServer) world, changed);
    }

    private GenerationLight() {}

    /** Sends chunks whose light changed again to the players who already have them. */
    private static void resend(WorldServer world, Set<ChunkPos> chunks) {
        for (ChunkPos chunkPos : chunks) {
            PlayerChunkMapEntry entry = world.getPlayerChunkMap().getEntry(chunkPos.x, chunkPos.z);
            if (entry == null || !entry.isSentToPlayers()) continue;
            Chunk chunk = entry.getChunk();
            if (chunk != null) entry.sendPacket(new SPacketChunkData(chunk, 65535));
        }
    }
}
