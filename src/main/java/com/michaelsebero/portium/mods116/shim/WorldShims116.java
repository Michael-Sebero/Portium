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
    /** 1.16's {@code server.getAllLevels()}. */
    public static Iterable<net.minecraft.world.WorldServer> allWorlds(net.minecraft.server.MinecraftServer server) {
        return java.util.Arrays.asList(server.worlds);
    }

    /** The server's world of a 1.16 dimension name, or null. */
    public static Object worldNamed(net.minecraft.server.MinecraftServer server, String name) {
        for (net.minecraft.world.WorldServer world : server.worlds) {
            if (world != null && new net.minecraft.util.ResourceLocation(name).equals(new net.minecraft.util.ResourceLocation(dimensionName(world)))) return world;
        }
        return null;
    }

    /** A 1.16 registry key's name (1.16's own class, read by its method's id). */
    @javax.annotation.Nullable
    public static net.minecraft.util.ResourceLocation keyName(Object key) {
        try {
            java.lang.reflect.Method location = key.getClass().getMethod("func_240901_a_");
            return (net.minecraft.util.ResourceLocation) location.invoke(key);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /** Forge 1.16's {@code BiomeDictionary.getTypes(key)}: the types of 1.12's biome of that name (none if there's none). */
    public static java.util.Set<net.minecraftforge.common.BiomeDictionary.Type> biomeTypes(Object key) {
        net.minecraft.util.ResourceLocation name = keyName(key);
        net.minecraft.world.biome.Biome biome = name == null ? null : net.minecraftforge.fml.common.registry.ForgeRegistries.BIOMES.getValue(name);
        return biome == null ? java.util.Collections.emptySet() : net.minecraftforge.common.BiomeDictionary.getTypes(biome);
    }

    public static boolean biomeHasType(Object key, net.minecraftforge.common.BiomeDictionary.Type type) {
        return biomeTypes(key).contains(type);
    }

    /** 1.16's biome category name (Biome.Category's constant) for a 1.12 biome, from its dictionary types. */
    public static String categoryName(net.minecraft.world.biome.Biome biome) {
        if (biome != null) return com.michaelsebero.portium.mods116.ModernBiomes116.category(biome);
        java.util.Set<net.minecraftforge.common.BiomeDictionary.Type> types = net.minecraftforge.common.BiomeDictionary.getTypes(biome);
        if (types.contains(net.minecraftforge.common.BiomeDictionary.Type.NETHER)) return "NETHER";
        if (types.contains(net.minecraftforge.common.BiomeDictionary.Type.END)) return "THEEND";
        if (types.contains(net.minecraftforge.common.BiomeDictionary.Type.MUSHROOM)) return "MUSHROOM";
        if (types.contains(net.minecraftforge.common.BiomeDictionary.Type.OCEAN)) return "OCEAN";
        if (types.contains(net.minecraftforge.common.BiomeDictionary.Type.RIVER)) return "RIVER";
        if (types.contains(net.minecraftforge.common.BiomeDictionary.Type.BEACH)) return "BEACH";
        if (types.contains(net.minecraftforge.common.BiomeDictionary.Type.SWAMP)) return "SWAMP";
        if (types.contains(net.minecraftforge.common.BiomeDictionary.Type.JUNGLE)) return "JUNGLE";
        if (types.contains(net.minecraftforge.common.BiomeDictionary.Type.MESA)) return "MESA";
        if (types.contains(net.minecraftforge.common.BiomeDictionary.Type.SAVANNA)) return "SAVANNA";
        if (types.contains(net.minecraftforge.common.BiomeDictionary.Type.SNOWY) && !types.contains(net.minecraftforge.common.BiomeDictionary.Type.CONIFEROUS)) return "ICY";
        if (types.contains(net.minecraftforge.common.BiomeDictionary.Type.CONIFEROUS)) return "TAIGA";
        if (types.contains(net.minecraftforge.common.BiomeDictionary.Type.SANDY)) return "DESERT";
        if (types.contains(net.minecraftforge.common.BiomeDictionary.Type.MOUNTAIN)) return "EXTREME_HILLS";
        if (types.contains(net.minecraftforge.common.BiomeDictionary.Type.FOREST)) return "FOREST";
        if (types.contains(net.minecraftforge.common.BiomeDictionary.Type.PLAINS)) return "PLAINS";
        return "NONE";
    }

    /** A 1.16 game rule key's name (1.16's own class, read by its method's id). */
    private static String ruleName(Object key) {
        try {
            return (String) key.getClass().getMethod("func_223576_a").invoke(key);
        } catch (ReflectiveOperationException e) {
            return String.valueOf(key);
        }
    }

    public static boolean ruleBoolean(net.minecraft.world.GameRules rules, Object key) {
        return rules.getBoolean(ruleName(key));
    }

    public static int ruleInt(net.minecraft.world.GameRules rules, Object key) {
        return rules.getInt(ruleName(key));
    }

    /** 1.16's ambient light of a dimension: the Nether's and End's glow a little, as 1.12 lights them. */
    public static float ambientLight(DimensionType type) {
        return type == DimensionType.NETHER ? 0.1F : 0.0F;
    }

    // 1.16's dimension type properties, from the dimension's 1.12 world provider (or vanilla's, by the type).

    @javax.annotation.Nullable
    private static net.minecraft.world.WorldProvider provider(DimensionType type) {
        World world = net.minecraftforge.common.DimensionManager.getWorld(type.getId());
        return world == null ? null : world.provider;
    }

    public static boolean hasSkyLight(DimensionType type) {
        net.minecraft.world.WorldProvider provider = provider(type);
        return provider != null ? provider.hasSkyLight() : type == DimensionType.OVERWORLD;
    }

    /** {@code isUltrawarm}: water evaporates (the Nether). */
    public static boolean ultrawarm(DimensionType type) {
        net.minecraft.world.WorldProvider provider = provider(type);
        return provider != null ? provider.doesWaterVaporize() : type == DimensionType.NETHER;
    }

    /** {@code isNatural}: compasses and clocks work, beds don't explode. */
    public static boolean natural(DimensionType type) {
        net.minecraft.world.WorldProvider provider = provider(type);
        return provider != null ? provider.isSurfaceWorld() : type == DimensionType.OVERWORLD;
    }

    public static double coordinateScale(DimensionType type) {
        net.minecraft.world.WorldProvider provider = provider(type);
        return provider != null ? provider.getMovementFactor() : type == DimensionType.NETHER ? 8.0D : 1.0D;
    }

    public static boolean hasCeiling(DimensionType type) {
        net.minecraft.world.WorldProvider provider = provider(type);
        return provider != null ? provider.isNether() || !provider.hasSkyLight() && type != DimensionType.THE_END : type == DimensionType.NETHER;
    }

    public static boolean piglinSafe(DimensionType type) {
        return type == DimensionType.NETHER;
    }

    public static boolean bedWorks(DimensionType type) {
        net.minecraft.world.WorldProvider provider = provider(type);
        return provider != null ? provider.canRespawnHere() : type == DimensionType.OVERWORLD;
    }

    public static boolean respawnAnchorWorks(DimensionType type) {
        return type == DimensionType.NETHER;
    }

    public static boolean hasRaids(DimensionType type) {
        return type == DimensionType.OVERWORLD;
    }

    public static int logicalHeight(DimensionType type) {
        net.minecraft.world.WorldProvider provider = provider(type);
        return provider != null ? provider.getActualHeight() : type == DimensionType.NETHER ? 128 : 256;
    }

    public static boolean hasDragonFight(DimensionType type) {
        return type == DimensionType.THE_END;
    }

    /** {@code hasFixedTime}: the sky doesn't turn (the Nether's and End's). */
    public static boolean hasFixedTime(DimensionType type) {
        return !natural(type);
    }

    public static float brightness(World world, net.minecraft.util.math.BlockPos pos) {
        return world.getLightBrightness(pos);
    }

    public static long dayTime(World world) {
        return world.getWorldTime();
    }

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

    /**
     * Forge 1.16's {@code world.isAreaLoaded(center, range)}: 1.12's own of that name, which a normal install has only
     * under its SRG id (Create's rotation propagation asks it before every neighbour).
     */
    public static boolean isAreaLoaded(World world, net.minecraft.util.math.BlockPos center, int range) {
        return world.isAreaLoaded(center, range);
    }

    public static boolean setBlockState(World world, net.minecraft.util.math.BlockPos pos, net.minecraft.block.state.IBlockState state, int flags,
                                        int depth) {
        return setBlockState(world, pos, state, flags);
    }

    // 1.16 mods' world features, placed as 1.12 populates a chunk (see ModernWorldGen116). 1.16 gives a feature a
    // region of chunks around it and drops writes outside; 1.12 has only the 2x2 chunks it populates loaded, and loads
    // (and generates) any other chunk a feature touches. While features are placed, a mod's block reads and writes in
    // that world stay in those chunks: outside, it reads air and its writes are dropped, and writes make no neighbour
    // or observer updates (1.16's region makes none either, and they'd reach the chunks next to the 2x2).

    /** The world features are being placed in (server thread), or null. */
    @javax.annotation.Nullable
    private static World generating;
    /** The populated 2x2 chunks' lowest block x and z. */
    private static int generatingX, generatingZ;

    /** Runs the placement of a chunk's features in the 2x2 chunks from it, which 1.12 has loaded to populate it. */
    public static void placeFeatures(World world, int chunkX, int chunkZ, Runnable placement) {
        World previous = generating;
        int previousX = generatingX, previousZ = generatingZ;
        generating = world;
        generatingX = chunkX << 4;
        generatingZ = chunkZ << 4;
        try {
            placement.run();
        } finally {
            generating = previous;
            generatingX = previousX;
            generatingZ = previousZ;
        }
    }

    private static boolean outsidePlacement(net.minecraft.util.math.BlockPos pos) {
        int x = pos.getX() - generatingX, z = pos.getZ() - generatingZ;
        return x < 0 || x > 31 || z < 0 || z > 31;
    }

    /** 1.16's {@code world.setBlockState(pos, state, flags)}: 1.12's, kept in the populated chunks while features are placed. */
    public static boolean setBlockState(World world, net.minecraft.util.math.BlockPos pos, net.minecraft.block.state.IBlockState state, int flags) {
        if (world != generating) return world.setBlockState(pos, state, flags);
        if (outsidePlacement(pos)) return false;
        return world.setBlockState(pos, state, flags & ~1 | 16);
    }

    /** 1.16's {@code world.getBlockState(pos)}: the whole state (see BlockShims), air outside the populated chunks while features are placed. */
    public static net.minecraft.block.state.IBlockState getBlockState(net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
        if (world == generating && outsidePlacement(pos)) return net.minecraft.init.Blocks.AIR.getDefaultState();
        return com.michaelsebero.portium.modern.shim.BlockShims.getActualBlockState(world, pos);
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

    /** 1.16's {@code IDayTimeReader.getCelestialAngleByTime(partialTicks)}: 1.12's celestial angle. */
    public static float getCelestialAngle(World world, float partialTicks) {
        return world.getCelestialAngle(partialTicks);
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

    /** 1.16's {@code regionFile.close()}: 1.12's, which a normal install has under its SRG id. */
    public static void closeRegion(net.minecraft.world.chunk.storage.RegionFile file) throws java.io.IOException {
        file.close();
    }

    /** 1.16's {@code template.placeInWorld(world, pos, settings, random)}: 1.12's placing, blocks sent to clients (Create's ponder scenes). */
    public static void placeTemplate(net.minecraft.world.gen.structure.template.Template template, World world, net.minecraft.util.math.BlockPos pos,
                                     net.minecraft.world.gen.structure.template.PlacementSettings settings, java.util.Random random) {
        template.addBlocksToWorld(world, pos, processor(template, world, pos, settings), settings, 2);
    }

    /** 1.16's {@code placeInWorldChunk(world, pos, settings, random)}: as {@code placeInWorld} (Create's schematicannon). */
    public static void placeTemplateInChunk(net.minecraft.world.gen.structure.template.Template template, World world, net.minecraft.util.math.BlockPos pos,
                                            net.minecraft.world.gen.structure.template.PlacementSettings settings, java.util.Random random) {
        template.addBlocksToWorld(world, pos, processor(template, world, pos, settings), settings, 2);
    }

    /** 1.16's processors of placement settings, which 1.12's lack (they go to 1.12's placing as its template processor). */
    private static final java.util.Map<net.minecraft.world.gen.structure.template.PlacementSettings, java.util.List<Object>> PROCESSORS =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /** 1.16's {@code settings.addProcessor(processor)}. */
    public static net.minecraft.world.gen.structure.template.PlacementSettings addProcessor(net.minecraft.world.gen.structure.template.PlacementSettings settings,
                                                                                           Object processor) {
        PROCESSORS.computeIfAbsent(settings, key -> new java.util.ArrayList<>()).add(processor);
        return settings;
    }

    /** 1.16's {@code settings.clearProcessors()}. */
    public static net.minecraft.world.gen.structure.template.PlacementSettings clearProcessors(net.minecraft.world.gen.structure.template.PlacementSettings settings) {
        PROCESSORS.remove(settings);
        return settings;
    }

    /** 1.16's {@code settings.removeProcessor(processor)}. */
    public static net.minecraft.world.gen.structure.template.PlacementSettings removeProcessor(net.minecraft.world.gen.structure.template.PlacementSettings settings,
                                                                                              Object processor) {
        java.util.List<Object> processors = PROCESSORS.get(settings);
        if (processors != null) processors.remove(processor);
        return settings;
    }

    /** 1.16's {@code settings.getProcessors()}. */
    public static java.util.List<Object> processors(net.minecraft.world.gen.structure.template.PlacementSettings settings) {
        java.util.List<Object> processors = PROCESSORS.get(settings);
        return processors == null ? new java.util.ArrayList<>() : processors;
    }

    /** 1.16's {@code settings.setRotationPivot(pos)}: 1.12 rotates templates about their origin; kept as that. */
    public static net.minecraft.world.gen.structure.template.PlacementSettings setRotationPivot(net.minecraft.world.gen.structure.template.PlacementSettings settings,
                                                                                               net.minecraft.util.math.BlockPos pivot) {
        return settings;
    }

    /** 1.16's {@code settings.getRotationPivot()}: 1.12 rotates templates about their origin. */
    public static net.minecraft.util.math.BlockPos rotationPivot(net.minecraft.world.gen.structure.template.PlacementSettings settings) {
        return net.minecraft.util.math.BlockPos.ORIGIN;
    }

    /**
     * The settings' 1.16 processors as one 1.12 template processor: each block passes through Forge 1.16's
     * {@code process(world, origin, pos, raw, info, settings, template)} (or 1.16's six-argument one), in order.
     */
    @javax.annotation.Nullable
    private static net.minecraft.world.gen.structure.template.ITemplateProcessor processor(net.minecraft.world.gen.structure.template.Template template, World world,
                                                                                             net.minecraft.util.math.BlockPos origin,
                                                                                             net.minecraft.world.gen.structure.template.PlacementSettings settings) {
        java.util.List<Object> processors = PROCESSORS.get(settings);
        if (processors == null || processors.isEmpty()) return null;
        java.util.List<Object> each = new java.util.ArrayList<>(processors);
        return (target, pos, info) -> {
            net.minecraft.world.gen.structure.template.Template.BlockInfo current = info;
            for (Object processor : each) {
                if (current == null) return null;
                current = processBlock(processor, world, origin, pos, info, current, settings, template);
            }
            return current;
        };
    }

    @javax.annotation.Nullable
    private static net.minecraft.world.gen.structure.template.Template.BlockInfo processBlock(Object processor, World world, net.minecraft.util.math.BlockPos origin,
                                                                                          net.minecraft.util.math.BlockPos pos,
                                                                                          net.minecraft.world.gen.structure.template.Template.BlockInfo raw,
                                                                                          net.minecraft.world.gen.structure.template.Template.BlockInfo info,
                                                                                          net.minecraft.world.gen.structure.template.PlacementSettings settings,
                                                                                          net.minecraft.world.gen.structure.template.Template template) {
        try {
            for (Class<?> c = processor.getClass(); c != null; c = c.getSuperclass()) {
                for (java.lang.reflect.Method method : c.getDeclaredMethods()) {
                    int count = method.getParameterCount();
                    if (!("process".equals(method.getName()) && count == 7) && !("func_230386_a_".equals(method.getName()) && count == 6)) continue;
                    method.setAccessible(true);
                    Object result = count == 7 ? method.invoke(processor, world, origin, pos, raw, info, settings, template)
                            : method.invoke(processor, world, origin, pos, raw, info, settings);
                    return (net.minecraft.world.gen.structure.template.Template.BlockInfo) result;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            com.michaelsebero.portium.Portium.LOGGER.warn("A 1.16 structure processor ({}) failed; the block is placed as it is", processor.getClass().getName(), e);
        }
        return info;
    }

    /**
     * 1.16's {@code template.read(nbt)}: 1.12's, once Minecraft's blocks in the palette(s) are renamed from their 1.13+
     * names (Create's ponder scenes stand on {@code white_concrete} and {@code snow_block}, which 1.12 reads as air).
     */
    public static void readTemplate(net.minecraft.world.gen.structure.template.Template template, NBTTagCompound nbt) {
        NBTTagCompound copy = nbt.copy();
        if (copy.hasKey("palettes", 9)) {
            net.minecraft.nbt.NBTTagList palettes = copy.getTagList("palettes", 9);
            for (int i = 0; i < palettes.tagCount(); i++) legacyPalette((net.minecraft.nbt.NBTTagList) palettes.get(i));
        }
        if (copy.hasKey("palette", 9)) legacyPalette(copy.getTagList("palette", 10));
        template.read(copy);
    }

    private static void legacyPalette(net.minecraft.nbt.NBTTagList palette) {
        for (int i = 0; i < palette.tagCount(); i++) {
            NBTTagCompound entry = palette.getCompoundTagAt(i);
            if (!new ResourceLocation(entry.getString("Name")).getNamespace().equals("minecraft")) continue;
            palette.set(i, net.minecraft.nbt.NBTUtil.writeBlockState(new NBTTagCompound(), NbtShims116.readBlockState(entry)));
        }
    }

    /** 1.16's {@code template.placeInWorld(world, pos, pivot, settings, random, flags)}: 1.12's placing with the flags. */
    public static boolean placeTemplate(net.minecraft.world.gen.structure.template.Template template, World world, net.minecraft.util.math.BlockPos pos,
                                        net.minecraft.util.math.BlockPos pivot, net.minecraft.world.gen.structure.template.PlacementSettings settings,
                                        java.util.Random random, int flags) {
        template.addBlocksToWorld(world, pos, processor(template, world, pos, settings), settings, flags);
        return true;
    }

    /** 1.16's static {@code World.isOutsideBuildHeight(pos)}. */
    public static boolean isOutsideBuildHeight(net.minecraft.util.math.BlockPos pos) {
        return pos.getY() < 0 || pos.getY() >= 256;
    }

    /** 1.16's {@code world.getChunk(x, z, status, required)}: the chunk if loaded, or made if required (Create's fluid pipes ask). */
    @javax.annotation.Nullable
    public static net.minecraft.world.chunk.Chunk getChunk(World world, int x, int z, Object status, boolean required) {
        return required ? world.getChunk(x, z) : world.getChunkProvider().getLoadedChunk(x, z);
    }

    /** 1.16's {@code WorldEntitySpawner.canCreatureTypeSpawnAtLocation(placement, world, pos, type)}: 1.12's by the placement's name. */
    public static boolean canSpawnAt(Object placement, World world, net.minecraft.util.math.BlockPos pos, Object type) {
        net.minecraft.entity.EntityLiving.SpawnPlacementType legacy = "IN_WATER".equals(String.valueOf(placement))
                ? net.minecraft.entity.EntityLiving.SpawnPlacementType.IN_WATER : net.minecraft.entity.EntityLiving.SpawnPlacementType.ON_GROUND;
        return net.minecraft.world.WorldEntitySpawner.canCreatureTypeSpawnAtLocation(legacy, world, pos);
    }

    /** 1.16's {@code ThreadTaskExecutor.getName()}: its thread's name. */
    public static String executorName(Object executor) {
        return executor instanceof net.minecraft.server.MinecraftServer ? "Server" : "Client";
    }
}
