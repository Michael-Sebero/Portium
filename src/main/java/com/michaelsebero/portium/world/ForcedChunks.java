package com.michaelsebero.portium.world;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.Tags;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Chunks kept loaded with {@code /forceload} (1.13.1). The list is saved with each dimension; Forge chunk tickets
 * do the loading, which also keeps entities and random ticks running in those chunks as in 1.13.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ForcedChunks extends WorldSavedData {

    private static final String NAME = "portium_forced_chunks";

    private final Set<Long> chunks = new LinkedHashSet<>();
    private final List<ForgeChunkManager.Ticket> tickets = new ArrayList<>();

    public ForcedChunks(String name) {
        super(name);
    }

    public static ForcedChunks get(WorldServer world) {
        MapStorage storage = world.getPerWorldStorage();
        ForcedChunks data = (ForcedChunks) storage.getOrLoadData(ForcedChunks.class, NAME);
        if (data == null) {
            data = new ForcedChunks(NAME);
            storage.setData(NAME, data);
        }
        return data;
    }

    public static void registerCallback() {
        // The saved list is authoritative, so tickets Forge restores are dropped and requested again on world load.
        ForgeChunkManager.setForcedChunkLoadingCallback(Portium.instance, (tickets, world) -> tickets.forEach(ForgeChunkManager::releaseTicket));
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        World world = event.getWorld();
        if (world.isRemote || !(world instanceof WorldServer)) return;
        ForcedChunks data = get((WorldServer) world);
        data.tickets.clear();
        for (long key : data.chunks) data.load((WorldServer) world, new ChunkPos((int) key, (int) (key >> 32)));
    }

    public Set<Long> getChunks() {
        return Collections.unmodifiableSet(this.chunks);
    }

    public boolean isForced(int x, int z) {
        return this.chunks.contains(ChunkPos.asLong(x, z));
    }

    /** Forces or releases a chunk, returning whether anything changed. */
    public boolean setForced(WorldServer world, int x, int z, boolean forced) {
        long key = ChunkPos.asLong(x, z);
        if (forced ? !this.chunks.add(key) : !this.chunks.remove(key)) return false;
        this.markDirty();
        ChunkPos pos = new ChunkPos(x, z);
        if (forced) {
            this.load(world, pos);
        } else {
            for (ForgeChunkManager.Ticket ticket : new ArrayList<>(this.tickets)) {
                if (!ticket.getChunkList().contains(pos)) continue;
                ForgeChunkManager.unforceChunk(ticket, pos);
                if (ticket.getChunkList().isEmpty()) {
                    ForgeChunkManager.releaseTicket(ticket);
                    this.tickets.remove(ticket);
                }
            }
        }
        return true;
    }

    private void load(WorldServer world, ChunkPos pos) {
        ForgeChunkManager.Ticket ticket = null;
        for (ForgeChunkManager.Ticket existing : this.tickets) {
            if (existing.getChunkList().size() < existing.getMaxChunkListDepth()) {
                ticket = existing;
                break;
            }
        }
        if (ticket == null) {
            ticket = ForgeChunkManager.requestTicket(Portium.instance, world, ForgeChunkManager.Type.NORMAL);
            if (ticket == null) {
                Portium.LOGGER.warn("Forge refused another chunk ticket, so chunk {} in dimension {} is not force loaded", pos, world.provider.getDimension());
                return;
            }
            this.tickets.add(ticket);
        }
        ForgeChunkManager.forceChunk(ticket, pos);
        world.getChunkProvider().provideChunk(pos.x, pos.z);
    }

    // Saved as x, z pairs: 1.12's long array tag can be written but not read back.
    @Override
    public void readFromNBT(NBTTagCompound compound) {
        this.chunks.clear();
        int[] forced = compound.getIntArray("Forced");
        for (int i = 0; i + 1 < forced.length; i += 2) this.chunks.add(ChunkPos.asLong(forced[i], forced[i + 1]));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        int[] forced = new int[this.chunks.size() * 2];
        int i = 0;
        for (long key : this.chunks) {
            forced[i++] = (int) key;
            forced[i++] = (int) (key >> 32);
        }
        compound.setIntArray("Forced", forced);
        return compound;
    }
}
