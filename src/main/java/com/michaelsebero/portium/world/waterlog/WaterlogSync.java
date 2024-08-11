package com.michaelsebero.portium.world.waterlog;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.network.PortiumNetwork;
import com.michaelsebero.portium.network.WaterlogChunkMessage;
import com.michaelsebero.portium.network.WaterlogUpdateMessage;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Saves waterlogged cells with their chunk (in a tag 1.12 ignores) and keeps clients up to date. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class WaterlogSync {

    public static final String TAG = "PortiumWaterlogged";

    @SubscribeEvent
    public static void onChunkLoad(ChunkDataEvent.Load event) {
        NBTTagCompound level = event.getData().getCompoundTag("Level");
        if (level.hasKey(TAG, 9)) ((IWaterlogChunk) event.getChunk()).portium$getWaterlog().read(level.getTagList(TAG, 10));
    }

    @SubscribeEvent
    public static void onChunkSave(ChunkDataEvent.Save event) {
        WaterlogData data = ((IWaterlogChunk) event.getChunk()).portium$getWaterlog();
        if (!data.isEmpty()) event.getData().getCompoundTag("Level").setTag(TAG, data.write());
    }

    /** Sent right after the chunk itself, on the same connection, so it arrives after it. */
    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        Chunk chunk = event.getChunkInstance();
        if (chunk == null) return;
        WaterlogData data = ((IWaterlogChunk) chunk).portium$getWaterlog();
        if (!data.isEmpty()) PortiumNetwork.CHANNEL.sendTo(new WaterlogChunkMessage(chunk.x, chunk.z, data), event.getPlayer());
    }

    static void sendUpdate(WorldServer world, BlockPos pos, boolean waterlogged) {
        PlayerChunkMapEntry entry = world.getPlayerChunkMap().getEntry(pos.getX() >> 4, pos.getZ() >> 4);
        if (entry != null) entry.sendPacket(PortiumNetwork.CHANNEL.getPacketFrom(new WaterlogUpdateMessage(pos.toImmutable(), waterlogged)));
    }

    private WaterlogSync() {}
}
