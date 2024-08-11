package com.michaelsebero.portium.modern.state;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.api.IManyStateBlock;
import com.michaelsebero.portium.modern.adapter.IModernBlock;
import com.michaelsebero.portium.network.ModernStateChunkMessage;
import com.michaelsebero.portium.network.ModernStateUpdateMessage;
import com.michaelsebero.portium.network.PortiumNetwork;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.13 blocks with more than 16 states (doors, stairs, fences, chests...) keep every state: what 1.12's metadata can't
 * hold is kept per chunk ({@link ModernStateData}), saved with it in a tag 1.12 ignores, sent to the players who see
 * it, and read back wherever 1.12 reads a chunk's block state.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ModernStates {

    public static final String TAG = "PortiumModernStates";

    /** Per block with more than 16 states, the index of each state; blocks with fewer aren't in it. */
    private static final Map<Block, Map<IBlockState, Integer>> INDEXES = new ConcurrentHashMap<>();
    private static final Map<Block, Boolean> MANY = new ConcurrentHashMap<>();

    /** Whether a block is a 1.13 block, or one of Portium's, with more states than 1.12's metadata holds. */
    public static boolean hasManyStates(Block block) {
        if (!(block instanceof IModernBlock) && !(block instanceof IManyStateBlock)) return false;
        return MANY.computeIfAbsent(block, key -> key.getBlockState().getValidStates().size() > 16);
    }

    public static int index(IBlockState state) {
        Map<IBlockState, Integer> indexes = INDEXES.computeIfAbsent(state.getBlock(), block -> {
            Map<IBlockState, Integer> map = new IdentityHashMap<>();
            List<IBlockState> states = block.getBlockState().getValidStates();
            for (int i = 0; i < states.size(); i++) map.put(states.get(i), i);
            return map;
        });
        Integer index = indexes.get(state);
        return index == null ? -1 : index;
    }

    /** The full state for what a chunk read from its storage, if the cell keeps one. */
    public static IBlockState full(Chunk chunk, IBlockState stored, int x, int y, int z) {
        Block block = stored.getBlock();
        if (!hasManyStates(block)) return stored;
        int index = ((IModernStateChunk) chunk).portium$getModernStates().get(x, y, z);
        List<IBlockState> states = block.getBlockState().getValidStates();
        if (index < 0 || index >= states.size()) return stored;
        IBlockState full = states.get(index);
        // A stale entry for a block replaced while the entry wasn't updated: what storage says wins.
        return ModernMeta.meta(full) == ModernMeta.meta(stored) ? full : stored;
    }

    /**
     * Keeps a cell's full state as the chunk sets it. On a client, a vanilla block update brings only the state its
     * metadata names: one agreeing with the full state Portium's own message brought (in either order) leaves it.
     */
    public static void set(Chunk chunk, World world, BlockPos pos, IBlockState state) {
        ModernStateData data = ((IModernStateChunk) chunk).portium$getModernStates();
        if (!hasManyStates(state.getBlock())) {
            if (data.get(pos.getX(), pos.getY(), pos.getZ()) >= 0) data.remove(pos.getX(), pos.getY(), pos.getZ());
            return;
        }
        int index = index(state);
        if (world.isRemote) {
            int kept = data.get(pos.getX(), pos.getY(), pos.getZ());
            List<IBlockState> states = state.getBlock().getBlockState().getValidStates();
            if (kept >= 0 && kept < states.size() && ModernMeta.meta(states.get(kept)) == ModernMeta.meta(state) && index == ModernMeta.meta(state)) return;
            data.put(pos.getX(), pos.getY(), pos.getZ(), index);
            return;
        }
        int before = data.put(pos.getX(), pos.getY(), pos.getZ(), index);
        if (before != index && world instanceof WorldServer) {
            PlayerChunkMapEntry entry = ((WorldServer) world).getPlayerChunkMap().getEntry(pos.getX() >> 4, pos.getZ() >> 4);
            if (entry != null) entry.sendPacket(PortiumNetwork.CHANNEL.getPacketFrom(new ModernStateUpdateMessage(pos.toImmutable(), index)));
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkDataEvent.Load event) {
        NBTTagCompound level = event.getData().getCompoundTag("Level");
        if (level.hasKey(TAG, 11)) ((IModernStateChunk) event.getChunk()).portium$getModernStates().read(level.getIntArray(TAG));
    }

    @SubscribeEvent
    public static void onChunkSave(ChunkDataEvent.Save event) {
        ModernStateData data = ((IModernStateChunk) event.getChunk()).portium$getModernStates();
        if (!data.isEmpty()) event.getData().getCompoundTag("Level").setTag(TAG, data.write());
    }

    /** Sent right after the chunk itself, on the same connection, so it arrives after it. */
    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        Chunk chunk = event.getChunkInstance();
        if (chunk == null) return;
        ModernStateData data = ((IModernStateChunk) chunk).portium$getModernStates();
        if (!data.isEmpty()) PortiumNetwork.CHANNEL.sendTo(new ModernStateChunkMessage(chunk.x, chunk.z, data), event.getPlayer());
    }

    private ModernStates() {}
}
