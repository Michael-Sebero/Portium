package com.michaelsebero.portium.mods116.shim;

import com.michaelsebero.portium.modern.mc.util.registry.IRegistry;
import com.michaelsebero.portium.modern.mc.util.registry.SimpleRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;

/** 1.16's world methods 1.12 has otherwise. */
public final class WorldShims116 {

    /**
     * 1.16's {@code world.getDynamicRegistries()}: Portium's stand-in for them, since the registries are asked for by
     * key next (see {@code WorldGlue.registry}).
     */
    public static com.michaelsebero.portium.modern.mc.util.registry.DynamicRegistries registries(World world) {
        return new com.michaelsebero.portium.modern.mc.util.registry.DynamicRegistries(world);
    }

    /** 1.16's {@code world.getDimensionType()}: 1.12's dimension type. */
    public static DimensionType dimensionType(World world) {
        return world.provider.getDimensionType();
    }

    /** The name 1.16 keys a world's dimension by: 1.12's dimension type's name ({@code overworld}, {@code the_nether}). */
    public static String dimensionName(Object world) {
        return ((World) world).provider.getDimensionType().getName();
    }

    @SuppressWarnings("rawtypes")
    private static final SimpleRegistry DIMENSION_TYPES = new SimpleRegistry(new ResourceLocation("overworld"));

    /** 1.12's dimension types by name, as 1.16's dimension type registry (filled as they're asked for). */
    @SuppressWarnings("unchecked")
    public static synchronized IRegistry<?> dimensionTypes() {
        for (DimensionType type : DimensionType.values()) {
            ResourceLocation name = new ResourceLocation(type.getName());
            if (!DIMENSION_TYPES.containsKey(name)) DIMENSION_TYPES.put(name, type);
        }
        return DIMENSION_TYPES;
    }

    /** Forge 1.16's {@code entity.getPersistentData()}: Forge 1.12's entity data. */
    public static NBTTagCompound persistentData(Entity entity) {
        return entity.getEntityData();
    }

    // 1.16's world methods whose own code needs 1.16's chunk, light and entity internals: 1.12's equivalents.

    public static boolean isBlockPresent(World world, net.minecraft.util.math.BlockPos pos) {
        return world.isBlockLoaded(pos);
    }

    public static boolean chunkExists(World world, int x, int z) {
        return world.getChunkProvider().getLoadedChunk(x, z) != null;
    }

    public static boolean isAreaLoaded(World world, int x1, int y1, int z1, int x2, int y2, int z2) {
        return world.isAreaLoaded(new net.minecraft.util.math.BlockPos(x1, y1, z1), new net.minecraft.util.math.BlockPos(x2, y2, z2));
    }

    public static boolean setBlockState(World world, net.minecraft.util.math.BlockPos pos, net.minecraft.block.state.IBlockState state, int flags,
                                        int depth) {
        return world.setBlockState(pos, state, flags);
    }

    public static boolean removeBlock(World world, net.minecraft.util.math.BlockPos pos, boolean moving) {
        return world.setBlockToAir(pos);
    }

    public static boolean destroyBlock(World world, net.minecraft.util.math.BlockPos pos, boolean drop, Entity breaker, int depth) {
        return world.destroyBlock(pos, drop);
    }

    public static boolean destroyBlock(World world, net.minecraft.util.math.BlockPos pos, boolean drop, Entity breaker) {
        return world.destroyBlock(pos, drop);
    }

    public static boolean addEntity(World world, Entity entity) {
        return world.spawnEntity(entity);
    }

    public static void playEvent(World world, net.minecraft.entity.player.EntityPlayer player, int type, net.minecraft.util.math.BlockPos pos, int data) {
        world.playEvent(player, type, pos, data);
    }

    public static void playEvent(World world, int type, net.minecraft.util.math.BlockPos pos, int data) {
        world.playEvent(type, pos, data);
    }

    public static void notifyNeighbors(World world, net.minecraft.util.math.BlockPos pos, net.minecraft.block.Block block) {
        world.notifyNeighborsOfStateChange(pos, block, false);
    }

    public static java.util.List<net.minecraft.entity.player.EntityPlayer> getPlayers(World world) {
        return world.playerEntities;
    }

    public static net.minecraft.entity.player.EntityPlayer getPlayerByUuid(World world, java.util.UUID id) {
        return world.getPlayerEntityByUUID(id);
    }

    @SuppressWarnings("unchecked")
    public static java.util.List getEntities(World world, Class type, net.minecraft.util.math.AxisAlignedBB box) {
        return world.getEntitiesWithinAABB(type, box);
    }

    @SuppressWarnings("unchecked")
    public static java.util.List getEntities(World world, Class type, net.minecraft.util.math.AxisAlignedBB box, java.util.function.Predicate filter) {
        return world.getEntitiesWithinAABB(type, box, filter == null ? null : filter::test);
    }

    public static net.minecraft.entity.player.EntityPlayer getClosestPlayer(World world, Entity entity, double distance) {
        return world.getClosestPlayerToEntity(entity, distance);
    }

    public static net.minecraft.entity.player.EntityPlayer getClosestPlayer(World world, double x, double y, double z, double distance, boolean creative) {
        return world.getClosestPlayer(x, y, z, distance, creative);
    }

    public static boolean isPlayerWithin(World world, double x, double y, double z, double distance) {
        return world.isAnyPlayerWithinRangeAt(x, y, z, distance);
    }

    public static int getLight(World world, net.minecraft.util.math.BlockPos pos) {
        return world.getLight(pos);
    }

    public static boolean canSeeSky(World world, net.minecraft.util.math.BlockPos pos) {
        return world.canSeeSky(pos);
    }

    public static net.minecraft.world.biome.Biome getBiome(World world, net.minecraft.util.math.BlockPos pos) {
        return world.getBiome(pos);
    }

    public static boolean isNightTime(World world) {
        return !world.isDaytime();
    }

    public static int getMoonPhase(World world) {
        return world.getMoonPhase();
    }

    public static float getMoonFactor(World world) {
        return world.getCurrentMoonPhaseFactor();
    }

    public static boolean isDebug(World world) {
        return world.getWorldType() == net.minecraft.world.WorldType.DEBUG_ALL_BLOCK_STATES;
    }

    /** 1.16's height by heightmap type (typed loosely: 1.12 has one heightmap). */
    public static int getHeight(World world, Object heightmap, int x, int z) {
        return world.getHeight(x, z);
    }

    public static net.minecraft.util.math.BlockPos getHeight(World world, Object heightmap, net.minecraft.util.math.BlockPos pos) {
        return world.getHeight(pos);
    }

    public static boolean hasNoCollisions(World world, net.minecraft.util.math.AxisAlignedBB box) {
        return world.getCollisionBoxes(null, box).isEmpty();
    }

    public static boolean hasNoCollisions(World world, Entity entity) {
        return world.getCollisionBoxes(entity, entity.getEntityBoundingBox()).isEmpty();
    }

    public static boolean hasNoCollisions(World world, Entity entity, net.minecraft.util.math.AxisAlignedBB box) {
        return world.getCollisionBoxes(entity, box).isEmpty();
    }

    public static boolean checkNoEntityCollision(World world, Entity entity) {
        return world.checkNoEntityCollision(entity.getEntityBoundingBox(), entity);
    }

    public static boolean isTopSolid(World world, net.minecraft.util.math.BlockPos pos, Entity entity) {
        return world.getBlockState(pos).isTopSolid();
    }

    public static net.minecraft.profiler.Profiler getProfiler(World world) {
        return world.profiler;
    }

    public static net.minecraft.world.IBlockAccess getBlockReader(World world, int chunkX, int chunkZ) {
        return world;
    }

    public static boolean hasBlockState(World world, net.minecraft.util.math.BlockPos pos, java.util.function.Predicate<Object> test) {
        return test.test(world.getBlockState(pos));
    }

    public static void markForRender(World world, net.minecraft.util.math.BlockPos pos, net.minecraft.block.state.IBlockState from,
                                     net.minecraft.block.state.IBlockState to) {
        world.markBlockRangeForRenderUpdate(pos, pos);
    }

    public static boolean isYOutOfBounds(World world, int y) {
        return isYOutOfBounds(y);
    }

    /** 1.16's {@code PointOfInterestType.forState(state)}: 1.12's villagers keep no points of interest, so none. */
    public static java.util.Optional<?> pointOfInterest(net.minecraft.block.state.IBlockState state) {
        return java.util.Optional.empty();
    }

    /** Forge 1.16's {@code markAndNotifyBlock}, which adds the depth of neighbour updates left; 1.12's has none. */
    public static void markAndNotifyBlock(World world, net.minecraft.util.math.BlockPos pos, @javax.annotation.Nullable net.minecraft.world.chunk.Chunk chunk,
                                          net.minecraft.block.state.IBlockState oldState, net.minecraft.block.state.IBlockState newState, int flags, int recursionLeft) {
        world.markAndNotifyBlock(pos, chunk, oldState, newState, flags);
    }

    /** 1.16's {@code serverWorld.getWorld()}: the server world itself, which is 1.12's server world here. */
    public static net.minecraft.world.WorldServer serverWorld(World world) {
        return (net.minecraft.world.WorldServer) world;
    }

    /** 1.16's is static ({@code World.isYOutOfBounds(y)}); 1.12's worlds are 256 high. */
    public static boolean isYOutOfBounds(int y) {
        return y < 0 || y >= 256;
    }

    public static void playMovingSound(World world, net.minecraft.entity.player.EntityPlayer player, Entity entity, net.minecraft.util.SoundEvent sound,
                                       net.minecraft.util.SoundCategory category, float volume, float pitch) {
        world.playSound(player, entity.posX, entity.posY, entity.posZ, sound, category, volume, pitch);
    }

    /** 1.16's explosion by mode (NONE, BREAK, DESTROY; typed loosely): 1.12's smokes or breaks. */
    public static net.minecraft.world.Explosion createExplosion(World world, Entity entity, double x, double y, double z, float strength, Object mode) {
        return world.createExplosion(entity, x, y, z, strength, !"NONE".equals(String.valueOf(mode)));
    }

    public static net.minecraft.world.Explosion createExplosion(World world, Entity entity, double x, double y, double z, float strength, boolean fire,
                                                               Object mode) {
        return world.newExplosion(entity, x, y, z, strength, fire, !"NONE".equals(String.valueOf(mode)));
    }

    // 1.16's block reader methods, on 1.12's block access.

    public static int getMaxLightLevel(net.minecraft.world.IBlockAccess access) {
        return 15;
    }

    public static int getBuildHeight(net.minecraft.world.IBlockAccess access) {
        return 256;
    }

    public static int getLightValue(net.minecraft.world.IBlockAccess access, net.minecraft.util.math.BlockPos pos) {
        return access.getBlockState(pos).getLightValue(access, pos);
    }

    public static int getLightFor(net.minecraft.world.IBlockAccess access, net.minecraft.world.EnumSkyBlock type, net.minecraft.util.math.BlockPos pos) {
        if (access instanceof World) return ((World) access).getLightFor(type, pos);
        int combined = access.getCombinedLight(pos, 0);
        return type == net.minecraft.world.EnumSkyBlock.SKY ? combined >> 20 & 15 : combined >> 4 & 15;
    }

    // 1.16's task executors (client and server), onto 1.12's scheduled tasks; typed loosely, as 1.16's executor
    // classes are its own.

    public static void enqueue(Object executor, Runnable task) {
        ((net.minecraft.util.IThreadListener) executor).addScheduledTask(task);
    }

    public static java.util.concurrent.CompletableFuture<Void> deferTask(Object executor, Runnable task) {
        java.util.concurrent.CompletableFuture<Void> done = new java.util.concurrent.CompletableFuture<>();
        ((net.minecraft.util.IThreadListener) executor).addScheduledTask(() -> {
            try {
                task.run();
                done.complete(null);
            } catch (Throwable t) {
                done.completeExceptionally(t);
            }
        });
        return done;
    }

    public static <T> java.util.concurrent.CompletableFuture<T> supplyAsync(Object executor, java.util.function.Supplier<T> task) {
        java.util.concurrent.CompletableFuture<T> done = new java.util.concurrent.CompletableFuture<>();
        ((net.minecraft.util.IThreadListener) executor).addScheduledTask(() -> {
            try {
                done.complete(task.get());
            } catch (Throwable t) {
                done.completeExceptionally(t);
            }
        });
        return done;
    }

    public static boolean isOnExecutionThread(Object executor) {
        return ((net.minecraft.util.IThreadListener) executor).isCallingFromMinecraftThread();
    }

    private WorldShims116() {}
}
