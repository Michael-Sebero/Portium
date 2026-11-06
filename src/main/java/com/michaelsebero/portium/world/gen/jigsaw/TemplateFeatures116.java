package com.michaelsebero.portium.world.gen.jigsaw;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.compat.LegacyBlockEntityNbt;
import com.michaelsebero.portium.compat.LegacyBlockStates;
import com.michaelsebero.portium.world.gen.structure.StructureTemplate;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 1.16's single-template structures: ruined portals (overworld and Nether, dressed by biome, standing on land,
 * buried, in mountains, under water or over lava) and Nether fossils (in soul sand valleys). Laid out from the seed,
 * placed chunk by chunk as each populates.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class TemplateFeatures116 {

    enum Location { ON_LAND_SURFACE, PARTLY_BURIED, ON_OCEAN_FLOOR, IN_MOUNTAIN, UNDERGROUND, IN_NETHER }

    /** A laid-out portal or fossil: its template, where and how it's turned, and how it's dressed. */
    static final class Layout {
        final String template;
        final BlockPos origin;
        final Rotation rotation;
        final Mirror mirror;
        final StructureBoundingBox box;
        final Location location;
        final float mossiness;
        final boolean blackstone;
        final boolean vines;
        final boolean overgrown;
        final boolean cold;

        Layout(String template, BlockPos origin, Rotation rotation, Mirror mirror, StructureBoundingBox box, Location location, float mossiness,
               boolean blackstone, boolean vines, boolean overgrown, boolean cold) {
            this.template = template;
            this.origin = origin;
            this.rotation = rotation;
            this.mirror = mirror;
            this.box = box;
            this.location = location;
            this.mossiness = mossiness;
            this.blackstone = blackstone;
            this.vines = vines;
            this.overgrown = overgrown;
            this.cold = cold;
        }
    }

    private static final int PORTAL_SALT = 34222645;
    private static final int FOSSIL_SALT = 14357921;
    private static final Map<World, Map<Long, Layout>> LAYOUTS = new java.util.WeakHashMap<>();

    @SubscribeEvent
    public static void onPopulate(PopulateChunkEvent.Pre event) {
        World world = event.getWorld();
        if (world.isRemote || !JigsawStructures.generatesIn(world)) return;
        JigsawData data = JigsawData.get();
        int chunkX = event.getChunkX();
        int chunkZ = event.getChunkZ();
        StructureBoundingBox area = new StructureBoundingBox((chunkX << 4) + 8, 0, (chunkZ << 4) + 8, (chunkX << 4) + 23, 255, (chunkZ << 4) + 23);
        boolean nether = world.provider.getDimensionType() == DimensionType.NETHER;
        boolean overworld = world.provider.getDimensionType() == DimensionType.OVERWORLD;
        if (!nether && !overworld) return;
        int spacing = nether ? 25 : 40;
        int separation = nether ? 10 : 15;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                ChunkPos start = candidate(world.getSeed(), chunkX + dx, chunkZ + dz, spacing, separation, PORTAL_SALT);
                if (start.x != chunkX + dx || start.z != chunkZ + dz) continue;
                Layout layout = layout(world, start, true);
                if (layout != null && layout.box.intersectsWith(area)) place(data, world, layout, area);
            }
        }
        if (nether) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    ChunkPos start = candidate(world.getSeed(), chunkX + dx, chunkZ + dz, 2, 1, FOSSIL_SALT);
                    if (start.x != chunkX + dx || start.z != chunkZ + dz) continue;
                    Layout layout = layout(world, start, false);
                    if (layout != null && layout.box.intersectsWith(area)) place(data, world, layout, area);
                }
            }
        }
    }

    static ChunkPos candidate(long seed, int chunkX, int chunkZ, int spacing, int separation, int salt) {
        int regionX = Math.floorDiv(chunkX, spacing);
        int regionZ = Math.floorDiv(chunkZ, spacing);
        Random random = new Random((long) regionX * 341873128712L + (long) regionZ * 132897987541L + seed + salt);
        return new ChunkPos(regionX * spacing + random.nextInt(spacing - separation), regionZ * spacing + random.nextInt(spacing - separation));
    }

    @Nullable
    private static Layout layout(World world, ChunkPos start, boolean portal) {
        Map<Long, Layout> cache = LAYOUTS.computeIfAbsent(world, key -> new HashMap<>());
        long key = ChunkPos.asLong(start.x, start.z) * 2 + (portal ? 1 : 0);
        if (cache.containsKey(key)) return cache.get(key);
        Layout layout = portal ? portalLayout(world, start) : fossilLayout(world, start);
        if (layout != null && Boolean.getBoolean("portium.dev.jigsaw")) {
            com.michaelsebero.portium.Portium.LOGGER.info("Template feature at {}: {} {} at {} box {}", start, layout.template, layout.location, layout.origin, layout.box);
        }
        if (cache.size() > 512) cache.clear();
        cache.put(key, layout);
        return layout;
    }

    /** The nearest Nether fossil (by its start) within 100 regions of two chunks, or null (1.16's /locate nether_fossil). */
    @Nullable
    public static BlockPos findNearestFossil(World world, BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        for (int ring = 0; ring <= 100; ring++) {
            BlockPos best = null;
            double bestSq = Double.MAX_VALUE;
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) continue;
                    ChunkPos start = candidate(world.getSeed(), chunkX + dx * 2, chunkZ + dz * 2, 2, 1, FOSSIL_SALT);
                    if (layout(world, start, false) == null) continue;
                    BlockPos at = new BlockPos((start.x << 4) + 8, 64, (start.z << 4) + 8);
                    double sq = at.distanceSq(pos.getX(), 64, pos.getZ());
                    if (sq < bestSq) {
                        best = at;
                        bestSq = sq;
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    /** The nearest ruined portal (by its start) within 100 regions, or null. */
    @Nullable
    public static BlockPos findNearestPortal(World world, BlockPos pos) {
        boolean nether = world.provider.getDimensionType() == DimensionType.NETHER;
        int spacing = nether ? 25 : 40;
        int separation = nether ? 10 : 15;
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        for (int ring = 0; ring <= 100; ring++) {
            BlockPos best = null;
            double bestSq = Double.MAX_VALUE;
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) continue;
                    ChunkPos start = candidate(world.getSeed(), chunkX + dx * spacing, chunkZ + dz * spacing, spacing, separation, PORTAL_SALT);
                    if (layout(world, start, true) == null) continue;
                    BlockPos at = new BlockPos((start.x << 4) + 8, 64, (start.z << 4) + 8);
                    double sq = at.distanceSq(pos.getX(), 64, pos.getZ());
                    if (sq < bestSq) {
                        best = at;
                        bestSq = sq;
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    // ---- Ruined portals ----

    @Nullable
    private static Layout portalLayout(World world, ChunkPos start) {
        JigsawData data = JigsawData.get();
        if (data == null) return null;
        Random random = JigsawStructures.largeFeatureRandom(world.getSeed(), start.x, start.z);
        boolean nether = world.provider.getDimensionType() == DimensionType.NETHER;
        Biome biome = JigsawStructures.biomeAt(world, start.x, start.z);
        java.util.Set<BiomeDictionary.Type> types = BiomeDictionary.getTypes(biome);
        Location location;
        boolean airPocket;
        float mossiness = 0.2F;
        boolean vines = false;
        boolean overgrown = false;
        boolean blackstone = false;
        if (nether) {
            location = Location.IN_NETHER;
            airPocket = random.nextFloat() < 0.5F;
            mossiness = 0.0F;
            blackstone = true;
        } else if (types.contains(BiomeDictionary.Type.SANDY) && types.contains(BiomeDictionary.Type.HOT)) {
            location = Location.PARTLY_BURIED;
            airPocket = false;
            mossiness = 0.0F;
        } else if (types.contains(BiomeDictionary.Type.JUNGLE)) {
            location = Location.ON_LAND_SURFACE;
            airPocket = random.nextFloat() < 0.5F;
            mossiness = 0.8F;
            overgrown = true;
            vines = true;
        } else if (types.contains(BiomeDictionary.Type.SWAMP)) {
            location = Location.ON_OCEAN_FLOOR;
            airPocket = false;
            mossiness = 0.5F;
            vines = true;
        } else if (types.contains(BiomeDictionary.Type.MOUNTAIN)) {
            boolean inside = random.nextFloat() < 0.5F;
            location = inside ? Location.IN_MOUNTAIN : Location.ON_LAND_SURFACE;
            airPocket = inside || random.nextFloat() < 0.5F;
        } else if (types.contains(BiomeDictionary.Type.OCEAN)) {
            location = Location.ON_OCEAN_FLOOR;
            airPocket = false;
            mossiness = 0.8F;
        } else {
            boolean underground = random.nextFloat() < 0.5F;
            location = underground ? Location.UNDERGROUND : Location.ON_LAND_SURFACE;
            airPocket = underground || random.nextFloat() < 0.5F;
        }
        String template = random.nextFloat() < 0.05F ? "ruined_portal/giant_portal_" + (1 + random.nextInt(3)) : "ruined_portal/portal_" + (1 + random.nextInt(10));
        JigsawTemplate shape = data.template(template);
        if (shape == null) return null;
        Rotation rotation = Rotation.values()[random.nextInt(4)];
        Mirror mirror = random.nextFloat() < 0.5F ? Mirror.NONE : Mirror.FRONT_BACK;
        BlockPos corner = new BlockPos(start.x << 4, 0, start.z << 4);
        StructureBoundingBox flat = box(shape, corner, rotation, mirror);
        // Portium: no ruined portal over water (seas, rivers, swamps): any water under its footprint and there's none.
        if (!nether && overWater(world, flat)) return null;
        int height = shape.getSize().getY();
        int ground = groundHeight(world, flat, nether);
        int y = findY(random, world, location, airPocket, ground, height, nether);
        BlockPos origin = corner.up(y);
        boolean cold = types.contains(BiomeDictionary.Type.COLD) || types.contains(BiomeDictionary.Type.SNOWY);
        return new Layout(template, origin, rotation, mirror, box(shape, origin, rotation, mirror), location, mossiness,
                blackstone, vines, overgrown, cold);
    }

    private static StructureBoundingBox box(JigsawTemplate template, BlockPos origin, Rotation rotation, Mirror mirror) {
        BlockPos a = StructureTemplate.transform(BlockPos.ORIGIN, mirror, rotation, BlockPos.ORIGIN).add(origin);
        BlockPos b = StructureTemplate.transform(template.getSize().add(-1, -1, -1), mirror, rotation, BlockPos.ORIGIN).add(origin);
        return new StructureBoundingBox(a, b);
    }

    /** Whether any column under a footprint (every other block, and its edges) is water as generated. */
    private static boolean overWater(World world, StructureBoundingBox box) {
        HeightSampler heights = new HeightSampler(world);
        for (int x = box.minX - 1; x <= box.maxX + 1; x += 2) {
            for (int z = box.minZ - 1; z <= box.maxZ + 1; z += 2) {
                if (heights.isWater(x, z)) return true;
            }
        }
        return heights.isWater(box.maxX + 1, box.maxZ + 1);
    }

    /** The lowest of the ground's heights at the portal's corners (1.16 takes the corners' minimum). */
    private static int groundHeight(World world, StructureBoundingBox box, boolean nether) {
        if (nether) return 32;
        HeightSampler heights = new HeightSampler(world);
        int lowest = Integer.MAX_VALUE;
        for (int[] corner : new int[][] {{box.minX, box.minZ}, {box.maxX, box.minZ}, {box.minX, box.maxZ}, {box.maxX, box.maxZ}}) {
            lowest = Math.min(lowest, heights.surface(corner[0], corner[1]) - 1);
        }
        return lowest;
    }

    private static int findY(Random random, World world, Location location, boolean airPocket, int ground, int height, boolean nether) {
        switch (location) {
            case IN_NETHER:
                if (airPocket) return between(random, 32, 100);
                return random.nextFloat() < 0.5F ? between(random, 27, 29) : between(random, 29, 100);
            case IN_MOUNTAIN:
                return between(random, 70, Math.max(71, ground - height));
            case UNDERGROUND:
                return between(random, 15, Math.max(16, ground - height));
            case PARTLY_BURIED:
                return ground - height + between(random, 2, 8);
            default:
                return ground;
        }
    }

    private static int between(Random random, int min, int max) {
        return min + random.nextInt(Math.max(1, max - min + 1));
    }

    // ---- Nether fossils ----

    @Nullable
    private static Layout fossilLayout(World world, ChunkPos start) {
        JigsawData data = JigsawData.get();
        if (data == null) return null;
        Biome biome = JigsawStructures.biomeAt(world, start.x, start.z);
        if (biome.getRegistryName() == null || !biome.getRegistryName().getPath().contains("soul_sand_valley")) return null;
        Random random = JigsawStructures.largeFeatureRandom(world.getSeed(), start.x, start.z);
        String template = "nether_fossils/fossil_" + (1 + random.nextInt(14));
        JigsawTemplate shape = data.template(template);
        if (shape == null) return null;
        int x = (start.x << 4) + random.nextInt(16);
        int z = (start.z << 4) + random.nextInt(16);
        Rotation rotation = Rotation.values()[random.nextInt(4)];
        // 1.16: from a random height down to the first air over a solid block (needs the chunk's terrain: placed late).
        int y = 32 + random.nextInt(Math.max(1, 128 - 10 - 32));
        BlockPos origin = new BlockPos(x, y, z);
        StructureBoundingBox flat = box(shape, origin, rotation, Mirror.NONE);
        StructureBoundingBox tall = new StructureBoundingBox(flat.minX, 0, flat.minZ, flat.maxX, 255, flat.maxZ);
        return new Layout(template, origin, rotation, Mirror.NONE, tall, Location.IN_NETHER, 0.0F, false, false, false, false);
    }

    // ---- Placing ----

    private static void place(JigsawData data, World world, Layout layout, StructureBoundingBox clip) {
        JigsawTemplate template = data.template(layout.template);
        if (template == null) return;
        BlockPos origin = layout.origin;
        if (layout.template.startsWith("nether_fossils/")) origin = settleFossil(world, origin, clip);
        if (origin == null) return;
        Random random = new Random(MathHelper.getPositionRandom(origin));
        for (JigsawTemplate.Block block : template.getBlocks()) {
            BlockPos pos = StructureTemplate.transform(block.pos, layout.mirror, layout.rotation, BlockPos.ORIGIN).add(origin);
            if (!clip.isVecInside(pos)) continue;
            String name = block.name;
            if (name.equals("minecraft:structure_void") || name.equals("minecraft:structure_block") || name.equals("minecraft:jigsaw")) continue;
            if (name.equals("minecraft:air") && layout.template.startsWith("nether_fossils/")) continue;
            Random blockRandom = new Random(MathHelper.getPositionRandom(pos));
            String replaced = portalBlock(name, layout, blockRandom, world, pos);
            if (replaced == null) continue;
            IBlockState state = replaced.equals(name) ? block.state : LegacyBlockStates.convert(replaced, block.properties);
            state = state.withMirror(layout.mirror).withRotation(layout.rotation);
            // Air pockets keep the portal's own air; otherwise a buried portal's air is the ground's.
            if (state.getMaterial() == Material.AIR && layout.location != Location.ON_LAND_SURFACE && !world.isAirBlock(pos)
                    && layout.location != Location.IN_NETHER) {
                continue;
            }
            world.setBlockState(pos, state, 2);
            if (block.nbt != null) {
                TileEntity tile = world.getTileEntity(pos);
                if (tile != null) {
                    NBTTagCompound tag = LegacyBlockEntityNbt.convert(block.nbt.copy(), replaced, block.properties);
                    tag.setInteger("x", pos.getX());
                    tag.setInteger("y", pos.getY());
                    tag.setInteger("z", pos.getZ());
                    tile.readFromNBT(tag);
                    tile.mirror(layout.mirror);
                    tile.rotate(layout.rotation);
                }
            }
        }
        if (layout.template.startsWith("ruined_portal/")) dressPortal(world, layout, template, origin, clip, random);
    }

    /** 1.16's ruined portal processors on one block: gold falls away, lava and obsidian age, stone bricks crack and moss. */
    @Nullable
    private static String portalBlock(String name, Layout layout, Random random, World world, BlockPos pos) {
        if (layout.template.startsWith("nether_fossils/")) return name;
        if (name.equals("minecraft:gold_block") && random.nextFloat() < 0.3F) return null;
        if (name.equals("minecraft:lava")) {
            if (layout.location == Location.ON_OCEAN_FLOOR) return "minecraft:magma_block";
            if (layout.cold) return "minecraft:magma_block";
        }
        if (name.equals("minecraft:obsidian") && random.nextFloat() < 0.15F) return "minecraft:crying_obsidian";
        if (layout.blackstone) {
            switch (name) {
                case "minecraft:stone_bricks":
                case "minecraft:cracked_stone_bricks":
                case "minecraft:mossy_stone_bricks":
                    return "minecraft:polished_blackstone_bricks";
                case "minecraft:stone_brick_stairs":
                    return "minecraft:polished_blackstone_brick_stairs";
                case "minecraft:stone_brick_slab":
                    return "minecraft:polished_blackstone_brick_slab";
                case "minecraft:stone_brick_wall":
                    return "minecraft:polished_blackstone_brick_wall";
                case "minecraft:chiseled_stone_bricks":
                    return "minecraft:chiseled_polished_blackstone";
                case "minecraft:stone":
                    return "minecraft:blackstone";
                case "minecraft:iron_bars":
                    return "minecraft:chain";
                default:
                    break;
            }
        }
        if (layout.mossiness > 0.0F && name.equals("minecraft:stone_bricks")) {
            float roll = random.nextFloat();
            if (roll < 0.5F * layout.mossiness) return "minecraft:mossy_stone_bricks";
            if (roll < 0.5F) return "minecraft:cracked_stone_bricks";
        }
        return name;
    }

    /** Netherrack and magma spread on the ground around the portal, lava in the Nether's, vines on jungle and swamp ones. */
    private static void dressPortal(World world, Layout layout, JigsawTemplate template, BlockPos origin, StructureBoundingBox clip, Random random) {
        IBlockState netherrack = Blocks.NETHERRACK.getDefaultState();
        IBlockState magma = Blocks.MAGMA.getDefaultState();
        StructureBoundingBox box = layout.box;
        int centerX = (box.minX + box.maxX) / 2;
        int centerZ = (box.minZ + box.maxZ) / 2;
        int radius = Math.max(box.getXSize(), box.getZSize()) / 2 + 2;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                double distance = Math.sqrt((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ));
                if (distance > radius || random.nextFloat() > 1.0F - distance / (radius + 1.0F)) continue;
                BlockPos ground = new BlockPos(x, origin.getY() - 1, z);
                if (!clip.isVecInside(ground)) continue;
                IBlockState state = world.getBlockState(ground);
                if (state.getMaterial().isSolid() && state.getBlock() != Blocks.OBSIDIAN && state.getBlock() != Blocks.BEDROCK) {
                    world.setBlockState(ground, random.nextFloat() < 0.07F ? magma : netherrack, 2);
                }
            }
        }
        if (!layout.vines) return;
        IBlockState vine = Blocks.VINE.getDefaultState();
        for (int x = box.minX; x <= box.maxX; x++) {
            for (int y = box.minY; y <= box.maxY; y++) {
                for (int z = box.minZ; z <= box.maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!clip.isVecInside(pos) || !world.isAirBlock(pos) || random.nextFloat() > 0.2F) continue;
                    for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
                        BlockPos against = pos.offset(side);
                        if (clip.isVecInside(against) && world.getBlockState(against).isSideSolid(world, against, side.getOpposite())) {
                            world.setBlockState(pos, vine.withProperty(net.minecraft.block.BlockVine.getPropertyFor(side), true), 2);
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * A Nether fossil: down from its height to the first air over solid ground in its origin's column. Only the area
     * holding that column places it (so its height is the same for all of it); parts reaching past that area are left out.
     */
    @Nullable
    private static BlockPos settleFossil(World world, BlockPos origin, StructureBoundingBox clip) {
        if (origin.getX() < clip.minX || origin.getX() > clip.maxX || origin.getZ() < clip.minZ || origin.getZ() > clip.maxZ) return null;
        for (int y = origin.getY(); y > 32; y--) {
            BlockPos pos = new BlockPos(origin.getX(), y, origin.getZ());
            if (world.isAirBlock(pos) && world.getBlockState(pos.down()).isSideSolid(world, pos.down(), EnumFacing.UP)) {
                return pos;
            }
        }
        return null;
    }

    private TemplateFeatures116() {}
}
