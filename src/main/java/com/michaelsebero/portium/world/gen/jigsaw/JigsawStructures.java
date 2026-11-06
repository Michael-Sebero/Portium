package com.michaelsebero.portium.world.gen.jigsaw;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.world.WorldGeneration;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawAssembler.Placed;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * 1.16.5's jigsaw structures in 1.16.5 worlds: villages (by biome: plains, desert, savanna, snowy, taiga, which
 * replace 1.12's), pillager outposts, and Nether bastion remnants. Each region of the world grid gets one possible
 * start (1.16's spacing, separation and salt); a start that passes its biome (and rarity) checks is laid out whole
 * from the terrain's shape, then placed chunk by chunk as each populates, never reaching outside that chunk.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class JigsawStructures {

    /** One kind of jigsaw structure. */
    public abstract static class Kind {
        final String name;
        final int spacing;
        final int separation;
        final int salt;
        final int depth;

        Kind(String name, int spacing, int separation, int salt, int depth) {
            this.name = name;
            this.spacing = spacing;
            this.separation = separation;
            this.salt = salt;
            this.depth = depth;
        }

        public String getName() {
            return this.name;
        }

        abstract boolean inDimension(World world);

        /** The start pool for a start that may generate here, or null. */
        @Nullable
        abstract String startPool(World world, int chunkX, int chunkZ);

        boolean onSurface() {
            return true;
        }

        int startY() {
            return 0;
        }

        /** 1.16's {@code getPotentialFeatureChunk}: the region's one candidate chunk. */
        ChunkPos candidate(long seed, int chunkX, int chunkZ) {
            int regionX = Math.floorDiv(chunkX, this.spacing);
            int regionZ = Math.floorDiv(chunkZ, this.spacing);
            Random random = new Random((long) regionX * 341873128712L + (long) regionZ * 132897987541L + seed + this.salt);
            int x = random.nextInt(this.spacing - this.separation);
            int z = random.nextInt(this.spacing - this.separation);
            return new ChunkPos(regionX * this.spacing + x, regionZ * this.spacing + z);
        }

        /**
         * The random 1.16 hands a structure's {@code isFeatureChunk} for its region: the one that picked the
         * candidate, carried on past that pick (bastion or fortress is the next roll).
         */
        Random afterCandidate(long seed, int chunkX, int chunkZ) {
            int regionX = Math.floorDiv(chunkX, this.spacing);
            int regionZ = Math.floorDiv(chunkZ, this.spacing);
            Random random = new Random((long) regionX * 341873128712L + (long) regionZ * 132897987541L + seed + this.salt);
            random.nextInt(this.spacing - this.separation);
            random.nextInt(this.spacing - this.separation);
            return random;
        }
    }

    /**
     * 1.16's Nether fortress rule: the bastion grid's candidate chunk (27/4, salt 30084232) whose large-feature roll
     * makes it a fortress (two in five; the other three are bastions).
     */
    public static boolean isFortressStart(long seed, int chunkX, int chunkZ) {
        ChunkPos start = BASTION.candidate(seed, chunkX, chunkZ);
        return start.x == chunkX && start.z == chunkZ && BASTION.afterCandidate(seed, chunkX, chunkZ).nextInt(5) < 2;
    }

    /** The fortress start of the region holding a chunk, or null if that region's candidate is a bastion. */
    @Nullable
    public static ChunkPos fortressCandidate(long seed, int chunkX, int chunkZ) {
        ChunkPos start = BASTION.candidate(seed, chunkX, chunkZ);
        return isFortressStart(seed, start.x, start.z) ? start : null;
    }

    /** 1.16's large-feature seed for a chunk (rarity rolls). */
    static Random largeFeatureRandom(long seed, int chunkX, int chunkZ) {
        Random random = new Random(seed);
        long a = random.nextLong();
        long b = random.nextLong();
        random.setSeed((long) chunkX * a ^ (long) chunkZ * b ^ seed);
        return random;
    }

    static Biome biomeAt(World world, int chunkX, int chunkZ) {
        return world.getBiomeProvider().getBiome(new BlockPos((chunkX << 4) + 9, 0, (chunkZ << 4) + 9));
    }

    /** The village start pool for a biome: 1.16's five kinds, or any biome 1.12 (or a mod) lets villages spawn in. */
    @Nullable
    static String villagePool(Biome biome) {
        if (biome == Biomes.PLAINS) return "village/plains/town_centers";
        if (biome == Biomes.DESERT) return "village/desert/town_centers";
        if (biome == Biomes.SAVANNA) return "village/savanna/town_centers";
        if (biome == Biomes.ICE_PLAINS) return "village/snowy/town_centers";
        if (biome == Biomes.TAIGA) return "village/taiga/town_centers";
        if (!MapGenVillage.VILLAGE_SPAWN_BIOMES.contains(biome)) return null;
        switch (com.michaelsebero.portium.entity.villager.VillagerType116.byBiome(biome)) {
            case DESERT:
                return "village/desert/town_centers";
            case SAVANNA:
                return "village/savanna/town_centers";
            case SNOW:
                return "village/snowy/town_centers";
            case TAIGA:
                return "village/taiga/town_centers";
            default:
                return "village/plains/town_centers";
        }
    }

    public static final Kind VILLAGE = new Kind("Village", 32, 8, 10387312, 6) {
        @Override
        boolean inDimension(World world) {
            return world.provider.getDimensionType() == DimensionType.OVERWORLD;
        }

        @Nullable
        @Override
        String startPool(World world, int chunkX, int chunkZ) {
            return villagePool(biomeAt(world, chunkX, chunkZ));
        }
    };

    public static final Kind OUTPOST = new Kind("Pillager_Outpost", 32, 8, 165745296, 7) {
        @Override
        boolean inDimension(World world) {
            return world.provider.getDimensionType() == DimensionType.OVERWORLD;
        }

        @Nullable
        @Override
        String startPool(World world, int chunkX, int chunkZ) {
            if (villagePool(biomeAt(world, chunkX, chunkZ)) == null) return null;
            // 1.16: one in five candidates, and none within ten chunks of a village.
            Random random = new Random((long) ((chunkX >> 4) ^ (chunkZ >> 4) << 4) ^ world.getSeed());
            random.nextInt();
            if (random.nextInt(5) != 0) return null;
            for (int x = chunkX - 10; x <= chunkX + 10; x++) {
                for (int z = chunkZ - 10; z <= chunkZ + 10; z++) {
                    ChunkPos village = VILLAGE.candidate(world.getSeed(), x, z);
                    if (village.x == x && village.z == z && VILLAGE.startPool(world, x, z) != null) return null;
                }
            }
            return "pillager_outpost/base_plates";
        }
    };

    public static final Kind BASTION = new Kind("Bastion_Remnant", 27, 4, 30084232, 6) {
        @Override
        boolean inDimension(World world) {
            return world.provider.getDimensionType() == DimensionType.NETHER;
        }

        @Nullable
        @Override
        String startPool(World world, int chunkX, int chunkZ) {
            // 1.16: shares its grid with fortresses; three in five candidates are bastions. Never in basalt deltas.
            if (afterCandidate(world.getSeed(), chunkX, chunkZ).nextInt(5) < 2) return null;
            Biome biome = biomeAt(world, chunkX, chunkZ);
            if (biome.getRegistryName() != null && biome.getRegistryName().getPath().contains("basalt_deltas")) return null;
            return "bastion/starts";
        }

        @Override
        boolean onSurface() {
            return false;
        }

        @Override
        int startY() {
            return 33;
        }
    };

    private static final List<Kind> ALL = Arrays.asList(VILLAGE, OUTPOST, BASTION);
    /** How many chunks from its start a structure reaches (80 blocks, rounded up). */
    private static final int REACH = 6;

    private static final Map<World, Map<Long, List<Placed>>> LAYOUTS = new WeakHashMap<>();
    private static final Map<World, HeightSampler> HEIGHTS = new WeakHashMap<>();

    public static boolean generatesIn(World world) {
        if (!PortiumConfig.worldGen.jigsawStructures || !world.getWorldInfo().isMapFeaturesEnabled() || !WorldGeneration.is116(world)) return false;
        WorldType type = world.getWorldType();
        return type != WorldType.FLAT && type != WorldType.DEBUG_ALL_BLOCK_STATES && JigsawData.get() != null;
    }

    /** Whether 1.12's villages give way to these in a world. */
    public static boolean replacesVillages(World world) {
        return generatesIn(world);
    }

    private static HeightSampler heights(World world) {
        return HEIGHTS.computeIfAbsent(world, HeightSampler::new);
    }

    /** A start's layout, worked out once per world session (it comes out the same every time). */
    static List<Placed> layout(World world, Kind kind, ChunkPos start) {
        Map<Long, List<Placed>> layouts = LAYOUTS.computeIfAbsent(world, key -> new LinkedHashMap<Long, List<Placed>>(64, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, List<Placed>> eldest) {
                return this.size() > 128;
            }
        });
        long key = ChunkPos.asLong(start.x, start.z) * 31L + kind.salt;
        List<Placed> layout = layouts.get(key);
        if (layout == null) {
            String pool = kind.startPool(world, start.x, start.z);
            JigsawData data = JigsawData.get();
            if (pool == null || data == null) {
                layout = Collections.emptyList();
            } else {
                Random random = largeFeatureRandom(world.getSeed(), start.x, start.z);
                layout = JigsawAssembler.assemble(data, heights(world), pool, new BlockPos(start.x << 4, kind.startY(), start.z << 4), kind.depth,
                        kind.onSurface(), kind != BASTION, random);
            }
            layouts.put(key, layout);
            if (Boolean.getBoolean("portium.dev.jigsaw")) {
                com.michaelsebero.portium.Portium.LOGGER.info("Jigsaw {} at {}: {} pieces", kind.name, start, layout.size());
                for (Placed piece : layout) {
                    com.michaelsebero.portium.Portium.LOGGER.info("  depth {} {} {} at {} box {}", piece.depth, piece.element, piece.rotation, piece.origin, piece.box);
                }
            }
        }
        return layout;
    }

    @SubscribeEvent
    public static void onPopulate(PopulateChunkEvent.Pre event) {
        World world = event.getWorld();
        if (world.isRemote || !generatesIn(world)) return;
        int chunkX = event.getChunkX();
        int chunkZ = event.getChunkZ();
        // As 1.12 populates: the 16x16 area between this chunk's centre and its neighbours'.
        StructureBoundingBox area = new StructureBoundingBox((chunkX << 4) + 8, 0, (chunkZ << 4) + 8, (chunkX << 4) + 23, 255, (chunkZ << 4) + 23);
        JigsawData data = JigsawData.get();
        PLACING.set(true);
        try {
            for (Kind kind : ALL) {
                if (!kind.inDimension(world)) continue;
                for (ChunkPos start : startsNear(world, kind, chunkX, chunkZ)) {
                    for (Placed piece : layout(world, kind, start)) {
                        if (piece.box.intersectsWith(area)) JigsawPlacer.place(data, piece, world, area, heights(world));
                    }
                }
            }
        } finally {
            PLACING.set(false);
        }
    }

    /** The lake events fire on the terrain bus. */
    public static void registerTerrainEvents() {
        net.minecraftforge.common.MinecraftForge.TERRAIN_GEN_BUS.register(new TerrainBus());
    }

    public static final class TerrainBus {

        /**
         * No water or lava lakes where they'd cut into a village or outpost, as 1.16's lakes skip chunks with a village.
         * 1.12 already skips lakes where its own villages generate, but these pieces aren't placed through it.
         */
        @SubscribeEvent
        public void onLake(PopulateChunkEvent.Populate event) {
            PopulateChunkEvent.Populate.EventType type = event.getType();
            if (type != PopulateChunkEvent.Populate.EventType.LAKE && type != PopulateChunkEvent.Populate.EventType.LAVA) return;
            World world = event.getWorld();
            if (world.isRemote) return;
            boolean touches = generatesIn(world) ? lakeWouldTouch(world, event.getChunkX(), event.getChunkZ())
                    : legacyVillageNear(event.getGen(), event.getChunkX(), event.getChunkZ());
            if (!touches) return;
            event.setResult(net.minecraftforge.fml.common.eventhandler.Event.Result.DENY);
            if (Boolean.getBoolean("portium.dev.jigsaw")) {
                com.michaelsebero.portium.Portium.LOGGER.info("No {} in chunk {}, {}: a village or outpost is in reach", type, event.getChunkX(), event.getChunkZ());
            }
        }
    }

    /**
     * Whether a lake from this chunk's population could reach a village or outpost piece: 1.12's lakes are 16 blocks
     * across, centred within the populated area, so they span this chunk's x and z plus 15 blocks, with a little room
     * for the ground a lake strips around itself.
     */
    private static StructureBoundingBox lakeReach(int chunkX, int chunkZ) {
        int margin = 4;
        return new StructureBoundingBox((chunkX << 4) - margin, 0, (chunkZ << 4) - margin, (chunkX << 4) + 30 + margin, 255, (chunkZ << 4) + 30 + margin);
    }

    /**
     * 1.12's own villages: it skips lakes in a chunk whose population places village pieces, but a neighbour's lake
     * still reaches into them; none where one could reach a piece of a village whose start is known.
     */
    private static boolean legacyVillageNear(net.minecraft.world.gen.IChunkGenerator generator, int chunkX, int chunkZ) {
        if (!(generator instanceof net.minecraft.world.gen.ChunkGeneratorOverworld)) return false;
        MapGenVillage villages = ((com.michaelsebero.portium.mixin.accessor.ChunkGeneratorOverworldAccessor) generator).portium$getVillageGenerator();
        if (villages == null) return false;
        StructureBoundingBox reach = lakeReach(chunkX, chunkZ);
        for (net.minecraft.world.gen.structure.StructureStart start
                : ((com.michaelsebero.portium.mixin.accessor.MapGenStructureAccessor) villages).portium$getStructureMap().values()) {
            if (!start.isSizeableStructure() || !start.getBoundingBox().intersectsWith(reach)) continue;
            for (net.minecraft.world.gen.structure.StructureComponent piece : start.getComponents()) {
                if (piece.getBoundingBox().intersectsWith(reach)) return true;
            }
        }
        return false;
    }

    static boolean lakeWouldTouch(World world, int chunkX, int chunkZ) {
        StructureBoundingBox reach = lakeReach(chunkX, chunkZ);
        for (Kind kind : new Kind[] {VILLAGE, OUTPOST}) {
            if (!kind.inDimension(world)) continue;
            for (ChunkPos start : startsNear(world, kind, chunkX, chunkZ)) {
                for (Placed piece : layout(world, kind, start)) {
                    if (piece.box.intersectsWith(reach)) return true;
                }
            }
        }
        return false;
    }

    private static final ThreadLocal<Boolean> PLACING = ThreadLocal.withInitial(() -> false);

    /** Dev: a chunk loaded while placing pieces is a bug (it cascades); where it came from. */
    @SubscribeEvent
    public static void onChunkLoad(net.minecraftforge.event.world.ChunkEvent.Load event) {
        if (PLACING.get() && Boolean.getBoolean("portium.dev.jigsaw")) {
            com.michaelsebero.portium.Portium.LOGGER.warn("Chunk {} loaded while placing jigsaw pieces", event.getChunk().getPos(), new Throwable());
        }
    }

    /** Starts whose reach could cover a chunk. */
    private static List<ChunkPos> startsNear(World world, Kind kind, int chunkX, int chunkZ) {
        List<ChunkPos> starts = new ArrayList<>();
        long seed = world.getSeed();
        for (int regionX = Math.floorDiv(chunkX - REACH, kind.spacing); regionX <= Math.floorDiv(chunkX + 1 + REACH, kind.spacing); regionX++) {
            for (int regionZ = Math.floorDiv(chunkZ - REACH, kind.spacing); regionZ <= Math.floorDiv(chunkZ + 1 + REACH, kind.spacing); regionZ++) {
                ChunkPos start = kind.candidate(seed, regionX * kind.spacing, regionZ * kind.spacing);
                if (Math.abs(start.x - chunkX) <= REACH + 1 && Math.abs(start.z - chunkZ) <= REACH + 1) starts.add(start);
            }
        }
        return starts;
    }

    /**
     * Dev: every candidate start of each kind within {@code radius} regions of the origin: how many have a start pool
     * (the biome allows one), how many of those assemble, and their piece counts, by start biome. Works the layouts
     * out without generating chunks.
     */
    public static void survey(World world, int radius) {
        for (Kind kind : ALL) {
            if (!kind.inDimension(world)) continue;
            int candidates = 0;
            int pooled = 0;
            int built = 0;
            int pieces = 0;
            Map<String, int[]> byBiome = new java.util.TreeMap<>();
            for (int rx = -radius; rx < radius; rx++) {
                for (int rz = -radius; rz < radius; rz++) {
                    ChunkPos start = kind.candidate(world.getSeed(), rx * kind.spacing, rz * kind.spacing);
                    candidates++;
                    String biome = String.valueOf(biomeAt(world, start.x, start.z).getRegistryName());
                    int[] counts = byBiome.computeIfAbsent(biome, key -> new int[3]);
                    counts[0]++;
                    if (kind.startPool(world, start.x, start.z) == null) continue;
                    if (counts[1] < 3) com.michaelsebero.portium.Portium.LOGGER.info("  {} {} at block {} {}", kind.name, biome, start.x << 4, start.z << 4);
                    pooled++;
                    counts[1]++;
                    int size = layout(world, kind, start).size();
                    if (size > 0) {
                        built++;
                        counts[2]++;
                    }
                    pieces += size;
                }
            }
            com.michaelsebero.portium.Portium.LOGGER.info("Jigsaw survey {}: {} candidates, {} in allowed biomes, {} assembled, {} pieces on average",
                    kind.name, candidates, pooled, built, built == 0 ? 0 : pieces / built);
            for (Map.Entry<String, int[]> entry : byBiome.entrySet()) {
                if (entry.getValue()[1] > 0) {
                    com.michaelsebero.portium.Portium.LOGGER.info("  {}: {} candidates, {} allowed, {} assembled", entry.getKey(), entry.getValue()[0],
                            entry.getValue()[1], entry.getValue()[2]);
                }
            }
            int other = 0;
            for (int[] counts : byBiome.values()) if (counts[1] == 0) other += counts[0];
            com.michaelsebero.portium.Portium.LOGGER.info("  other biomes: {} candidates", other);
        }
    }

    // ---- Terrain fitting (1.16's "beardifier") ----

    /** 1.16's beard kernel, by offset from a piece (x and z from its box's sides, y from its bottom), each in [-12, 12). */
    private static final float[] BEARD = new float[24 * 24 * 24];

    static {
        for (int x = 0; x < 24; x++) {
            for (int y = 0; y < 24; y++) {
                for (int z = 0; z < 24; z++) BEARD[(x * 24 + y) * 24 + z] = (float) beardKernel(x - 12, y - 12, z - 12);
            }
        }
    }

    private static double beardKernel(int x, int y, int z) {
        double horizontal = x * x + z * z;
        double height = y + 0.5D;
        double heightSq = height * height;
        double falloff = Math.pow(Math.E, -(heightSq / 16.0D + horizontal / 16.0D));
        double sign = -height / Math.sqrt(heightSq / 2.0D + horizontal / 2.0D) / 2.0D;
        return sign * falloff;
    }

    private static double beard(int x, int y, int z) {
        if (x < -12 || x >= 12 || y < -12 || y >= 12 || z < -12 || z >= 12) return 0.0D;
        return BEARD[((x + 12) * 24 + y + 12) * 24 + z + 12];
    }

    /**
     * The boxes of rigid village and outpost pieces near a chunk, which 1.16's terrain makes room for (streets and
     * other terrain-matching pieces follow the ground and need none). Worked out before the chunk's terrain is.
     */
    public static List<StructureBoundingBox> rigidPiecesNear(World world, int chunkX, int chunkZ) {
        if (!generatesIn(world) || world.provider.getDimensionType() != DimensionType.OVERWORLD) return Collections.emptyList();
        StructureBoundingBox area = new StructureBoundingBox((chunkX << 4) - 12, 0, (chunkZ << 4) - 12, (chunkX << 4) + 27, 255, (chunkZ << 4) + 27);
        List<StructureBoundingBox> boxes = new ArrayList<>();
        for (Kind kind : new Kind[] {VILLAGE, OUTPOST}) {
            for (ChunkPos start : startsNear(world, kind, chunkX, chunkZ)) {
                for (Placed piece : layout(world, kind, start)) {
                    if (piece.element.projection == JigsawPools.Projection.RIGID && piece.box.intersectsWith(area)) boxes.add(piece.box);
                }
            }
        }
        return boxes;
    }

    /**
     * Shapes a chunk's terrain (stone and air, before its surface is laid) around rigid structure pieces as 1.16's
     * noise generator does: each adds its kernel to the terrain's density, filling below the piece's bottom and
     * clearing above it, fading over a few blocks. 1.12's terrain has no density to add to, so it's estimated from the
     * depth below the column's surface; places the pieces don't reach are left as they were.
     */
    public static void fitTerrain(net.minecraft.world.chunk.ChunkPrimer primer, int chunkX, int chunkZ, List<StructureBoundingBox> boxes,
                                  net.minecraft.block.state.IBlockState stone, net.minecraft.block.state.IBlockState water, int seaLevel) {
        if (boxes.isEmpty()) return;
        int minY = 255;
        int maxY = 0;
        for (StructureBoundingBox box : boxes) {
            minY = Math.min(minY, box.minY - 12);
            maxY = Math.max(maxY, box.minY + 11);
        }
        minY = Math.max(1, minY);
        maxY = Math.min(254, maxY);
        net.minecraft.block.state.IBlockState air = net.minecraft.init.Blocks.AIR.getDefaultState();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = (chunkX << 4) + x;
                int worldZ = (chunkZ << 4) + z;
                int surface = 255;
                while (surface > 0 && primer.getBlockState(x, surface, z).getMaterial() != net.minecraft.block.material.Material.ROCK) surface--;
                for (int y = minY; y <= maxY; y++) {
                    double contribution = 0.0D;
                    for (StructureBoundingBox box : boxes) {
                        int dx = Math.max(0, Math.max(box.minX - worldX, worldX - box.maxX));
                        int dz = Math.max(0, Math.max(box.minZ - worldZ, worldZ - box.maxZ));
                        contribution += beard(dx, y - box.minY, dz) * 0.8D;
                    }
                    if (Math.abs(contribution) < 1.0E-4D) continue;
                    double terrain = MathHelper.clamp((surface - y + 0.5D) / 20.0D, -1.0D, 1.0D);
                    double density = terrain / 2.0D - terrain * terrain * terrain / 24.0D + contribution;
                    net.minecraft.block.state.IBlockState now = primer.getBlockState(x, y, z);
                    boolean solid = now.getMaterial() == net.minecraft.block.material.Material.ROCK;
                    if (density > 0.0D && !solid) {
                        primer.setBlockState(x, y, z, stone);
                    } else if (density <= 0.0D && solid) {
                        primer.setBlockState(x, y, z, y < seaLevel ? water : air);
                    }
                }
            }
        }
    }

    /** Whether a position is inside a piece of a structure of a kind (1.16's "inside a structure piece" checks). */
    public static boolean isInside(World world, Kind kind, BlockPos pos) {
        if (!generatesIn(world) || !kind.inDimension(world)) return false;
        for (ChunkPos start : startsNear(world, kind, pos.getX() >> 4, pos.getZ() >> 4)) {
            for (Placed piece : layout(world, kind, start)) {
                if (piece.box.isVecInside(pos)) return true;
            }
        }
        return false;
    }

    /** Inside a piece of the structure of this {@code /locate} name (for 1.16 advancements' location predicates). */
    public static boolean isInside(World world, String name, BlockPos pos) {
        for (Kind kind : ALL) {
            if (kind.name.equalsIgnoreCase(name)) return isInside(world, kind, pos);
        }
        return false;
    }

    /** 1.16: monsters spawning inside a pillager outpost are pillagers. */
    @SubscribeEvent
    public static void onPotentialSpawns(net.minecraftforge.event.world.WorldEvent.PotentialSpawns event) {
        if (event.getType() != net.minecraft.entity.EnumCreatureType.MONSTER || event.getWorld().isRemote) return;
        if (!com.michaelsebero.portium.init.PortiumEntities.isRegistered(com.michaelsebero.portium.entity.monster.EntityPillager.class)) return;
        if (!isInside(event.getWorld(), OUTPOST, event.getPos())) return;
        if (Boolean.getBoolean("portium.dev.jigsaw")) com.michaelsebero.portium.Portium.LOGGER.info("Outpost spawn attempt at {}", event.getPos());
        event.getList().clear();
        event.getList().add(OUTPOST_SPAWN);
    }

    /**
     * The one entry: 1.12 asks for the list again before spawning and checks its pick is still in it, which a new
     * entry each time (entries compare by identity) never is.
     */
    private static final Biome.SpawnListEntry OUTPOST_SPAWN = new Biome.SpawnListEntry(com.michaelsebero.portium.entity.monster.EntityPillager.class, 1, 1, 1);

    /** The nearest start of a structure by its {@code /locate} name within 100 regions, or null. */
    @Nullable
    public static BlockPos findNearest(World world, String name, BlockPos pos) {
        if (!generatesIn(world)) return null;
        if (name.equalsIgnoreCase("Ruined_Portal")) return TemplateFeatures116.findNearestPortal(world, pos);
        if (name.equalsIgnoreCase("Nether_Fossil")) {
            return world.provider.getDimensionType() == net.minecraft.world.DimensionType.NETHER ? TemplateFeatures116.findNearestFossil(world, pos) : null;
        }
        for (Kind kind : ALL) {
            if (!kind.name.equalsIgnoreCase(name) || !kind.inDimension(world)) continue;
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            for (int ring = 0; ring <= 100; ring++) {
                BlockPos best = null;
                double bestSq = Double.MAX_VALUE;
                for (int dx = -ring; dx <= ring; dx++) {
                    for (int dz = -ring; dz <= ring; dz++) {
                        if (Math.abs(dx) != ring && Math.abs(dz) != ring) continue;
                        ChunkPos start = kind.candidate(world.getSeed(), chunkX + dx * kind.spacing, chunkZ + dz * kind.spacing);
                        if (kind.startPool(world, start.x, start.z) == null) continue;
                        BlockPos at = new BlockPos((start.x << 4) + 8, kind.startY(), (start.z << 4) + 8);
                        double sq = at.distanceSq(pos.getX(), at.getY(), pos.getZ());
                        if (sq < bestSq) {
                            best = at;
                            bestSq = sq;
                        }
                    }
                }
                if (best != null) return best;
            }
        }
        return null;
    }

    public static boolean isOurs(String name) {
        if (name.equalsIgnoreCase("Ruined_Portal") || name.equalsIgnoreCase("Nether_Fossil")) return true;
        for (Kind kind : ALL) {
            if (kind.name.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onUnload(WorldEvent.Unload event) {
        LAYOUTS.remove(event.getWorld());
        HEIGHTS.remove(event.getWorld());
    }

    private JigsawStructures() {}
}
