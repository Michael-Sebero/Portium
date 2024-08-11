package com.michaelsebero.portium.mods116.adapter;

import net.minecraft.profiler.Profiler;
import net.minecraft.world.DimensionType;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nullable;
import java.io.File;
import java.util.function.Supplier;

/**
 * What a 1.16 mod's worlds extend in place of 1.16's world: Create's contraptions keep their blocks in a world of
 * their own that hands most questions to the real one. It's a 1.12 world with the real world's info and a provider
 * for its dimension, saving nothing and holding no chunks of its own; a mod's world answers for its blocks itself.
 * <p>
 * Made with 1.16's constructor arguments, loosely typed (the dimension key is 1.16's own class): the info, the
 * dimension's key and type, a profiler, whether it's a client's, whether it's a debug world and the seed.
 */
public abstract class World116 extends World {

    protected World116(Object info, Object dimensionKey, Object dimensionType, Supplier<?> profiler, boolean remote, boolean debug, long seed) {
        super(NoSaving.INSTANCE, (WorldInfo) info, provider(dimensionType), new Profiler(), remote);
        this.provider.setWorld(this);
        this.chunkProvider = createChunkProvider();
    }

    private static WorldProvider provider(Object dimensionType) {
        int id = dimensionType instanceof DimensionType ? ((DimensionType) dimensionType).getId() : 0;
        return DimensionManager.createProviderFor(id);
    }

    @Override
    protected IChunkProvider createChunkProvider() {
        return new NoChunks(this);
    }

    @Override
    protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
        return allowEmpty;
    }

    /** Chunks of nothing: a mod's world gives its blocks itself. */
    private static final class NoChunks implements IChunkProvider {

        private final World world;

        NoChunks(World world) {
            this.world = world;
        }

        @Nullable
        @Override
        public Chunk getLoadedChunk(int x, int z) {
            return null;
        }

        @Override
        public Chunk provideChunk(int x, int z) {
            return new EmptyChunk(this.world, x, z);
        }

        @Override
        public boolean tick() {
            return false;
        }

        @Override
        public String makeString() {
            return "World116";
        }

        @Override
        public boolean isChunkGeneratedAt(int x, int z) {
            return false;
        }
    }

    /** Saves nothing: the world lives only as long as what made it. */
    private enum NoSaving implements ISaveHandler {
        INSTANCE;

        @Nullable
        @Override
        public WorldInfo loadWorldInfo() {
            return null;
        }

        @Override
        public void checkSessionLock() throws MinecraftException {}

        @Override
        public IChunkLoader getChunkLoader(WorldProvider provider) {
            throw new UnsupportedOperationException("A 1.16 mod's world keeps no chunks");
        }

        @Override
        public void saveWorldInfoWithPlayer(WorldInfo info, net.minecraft.nbt.NBTTagCompound player) {}

        @Override
        public void saveWorldInfo(WorldInfo info) {}

        @Override
        public IPlayerFileData getPlayerNBTManager() {
            return null;
        }

        @Override
        public void flush() {}

        @Override
        public File getWorldDirectory() {
            return null;
        }

        @Override
        public File getMapFileFromName(String name) {
            return null;
        }

        @Override
        public TemplateManager getStructureTemplateManager() {
            return null;
        }
    }
}
