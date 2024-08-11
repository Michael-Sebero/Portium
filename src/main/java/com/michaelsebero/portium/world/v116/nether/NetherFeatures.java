package com.michaelsebero.portium.world.v116.nether;

import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.world.v116.GenFlags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.Random;
import java.util.function.Predicate;

/**
 * 1.16's Nether features: springs, fire patches, glowstone, ores, ancient debris, basalt pillars, deltas and columns,
 * and blobs of basalt or blackstone. Each follows 1.16's algorithm and draws its random numbers in the same order.
 * Blocks go in without neighbour updates, as 1.16's world generation places them. Features that can reach past the
 * chunks a 1.12 population has loaded see unloaded spots as bedrock and leave them alone, so they never make more
 * chunks generate.
 */
final class NetherFeatures {

    /** The Nether's sea level in 1.16: its lava sea's surface. */
    static final int SEA_LEVEL = 32;

    private static final EnumFacing[] DIRECTIONS = EnumFacing.values();

    // ---- Springs ----

    /**
     * A lava spring: a source in a spot with exactly {@code rocks} of the accepted blocks and {@code holes} air
     * blocks around it (sides and below), which then flows.
     */
    static void spring(World world, BlockPos pos, boolean needsBlockBelow, int rocks, int holes, Predicate<Block> accepted) {
        if (!accepted.test(world.getBlockState(pos.up()).getBlock())) return;
        if (needsBlockBelow && !accepted.test(world.getBlockState(pos.down()).getBlock())) return;
        IBlockState here = world.getBlockState(pos);
        if (!isAir(world, pos, here) && !accepted.test(here.getBlock())) return;
        int rock = 0;
        if (accepted.test(world.getBlockState(pos.west()).getBlock())) rock++;
        if (accepted.test(world.getBlockState(pos.east()).getBlock())) rock++;
        if (accepted.test(world.getBlockState(pos.north()).getBlock())) rock++;
        if (accepted.test(world.getBlockState(pos.south()).getBlock())) rock++;
        if (accepted.test(world.getBlockState(pos.down()).getBlock())) rock++;
        int hole = 0;
        if (world.isAirBlock(pos.west())) hole++;
        if (world.isAirBlock(pos.east())) hole++;
        if (world.isAirBlock(pos.north())) hole++;
        if (world.isAirBlock(pos.south())) hole++;
        if (world.isAirBlock(pos.down())) hole++;
        // Flowing lava schedules its own first flow when placed.
        if (rock == rocks && hole == holes) world.setBlockState(pos, Blocks.FLOWING_LAVA.getDefaultState(), GenFlags.GENERATION);
    }

    static boolean netherrack(Block block) {
        return block == Blocks.NETHERRACK;
    }

    /** What a basalt delta's springs may sit in. */
    static boolean deltaSpringRock(Block block) {
        return block == Blocks.NETHERRACK || block == Blocks.SOUL_SAND || block == Blocks.GRAVEL || block == Blocks.MAGMA
                || block == PortiumBlocks.BLACKSTONE;
    }

    // ---- Patches ----

    /**
     * 1.16's random patch: {@code tries} spots within 7 sideways and 3 up or down, each taking the block if it's
     * empty, the block can stay there and (with a {@code ground}) it stands on that.
     */
    static void patch(World world, Random random, BlockPos origin, IBlockState state, int tries, @Nullable Block ground) {
        for (int i = 0; i < tries; i++) {
            BlockPos at = origin.add(random.nextInt(8) - random.nextInt(8), random.nextInt(4) - random.nextInt(4), random.nextInt(8) - random.nextInt(8));
            if (world.isAirBlock(at) && canStay(world, at, state) && (ground == null || world.getBlockState(at.down()).getBlock() == ground)) {
                world.setBlockState(at, state, GenFlags.GENERATION);
            }
        }
    }

    private static boolean canStay(World world, BlockPos pos, IBlockState state) {
        if (pos.getY() < 0 || pos.getY() >= 256) return false;
        Block block = state.getBlock();
        return block instanceof BlockBush ? ((BlockBush) block).canBlockStay(world, pos, state) : block.canPlaceBlockAt(world, pos);
    }

    // ---- Glowstone ----

    /** A glowstone blob hanging from a ceiling of netherrack, basalt or blackstone. */
    static void glowstone(World world, Random random, BlockPos pos) {
        if (!world.isAirBlock(pos)) return;
        Block above = world.getBlockState(pos.up()).getBlock();
        if (above != Blocks.NETHERRACK && above != PortiumBlocks.BASALT && above != PortiumBlocks.BLACKSTONE) return;
        IBlockState glowstone = Blocks.GLOWSTONE.getDefaultState();
        world.setBlockState(pos, glowstone, GenFlags.GENERATION);
        for (int i = 0; i < 1500; i++) {
            BlockPos at = pos.add(random.nextInt(8) - random.nextInt(8), -random.nextInt(12), random.nextInt(8) - random.nextInt(8));
            if (!world.isAirBlock(at)) continue;
            int touching = 0;
            for (EnumFacing facing : DIRECTIONS) {
                if (world.getBlockState(at.offset(facing)).getBlock() == Blocks.GLOWSTONE) touching++;
                if (touching > 1) break;
            }
            if (touching == 1) world.setBlockState(at, glowstone, GenFlags.GENERATION);
        }
    }

    // ---- Ores ----

    static boolean isNetherrack(IBlockState state) {
        return state.getBlock() == Blocks.NETHERRACK;
    }

    /** 1.16's base_stone_nether tag: netherrack, basalt and blackstone. */
    static boolean isNetherStone(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.NETHERRACK || block == PortiumBlocks.BASALT || block == PortiumBlocks.BLACKSTONE;
    }

    /** 1.16's ore vein: a string of {@code size} overlapping spheres along a random line through {@code pos}. */
    static void ore(World world, Random random, BlockPos pos, Predicate<IBlockState> target, IBlockState ore, int size) {
        float angle = random.nextFloat() * (float) Math.PI;
        float reach = (float) size / 8.0F;
        int margin = MathHelper.ceil(((float) size / 16.0F * 2.0F + 1.0F) / 2.0F);
        double x1 = (double) pos.getX() + Math.sin(angle) * (double) reach;
        double x2 = (double) pos.getX() - Math.sin(angle) * (double) reach;
        double z1 = (double) pos.getZ() + Math.cos(angle) * (double) reach;
        double z2 = (double) pos.getZ() - Math.cos(angle) * (double) reach;
        double y1 = pos.getY() + random.nextInt(3) - 2;
        double y2 = pos.getY() + random.nextInt(3) - 2;
        int minX = pos.getX() - MathHelper.ceil(reach) - margin;
        int minY = pos.getY() - 2 - margin;
        int minZ = pos.getZ() - MathHelper.ceil(reach) - margin;
        int width = 2 * (MathHelper.ceil(reach) + margin);
        int height = 2 * (2 + margin);
        // 1.16 first checks the vein reaches below the ocean-floor heightmap, which it always does under the Nether's roof.
        placeOre(world, random, target, ore, size, x1, x2, z1, z2, y1, y2, minX, minY, minZ, width, height);
    }

    private static void placeOre(World world, Random random, Predicate<IBlockState> target, IBlockState ore, int size, double x1, double x2,
                                 double z1, double z2, double y1, double y2, int minX, int minY, int minZ, int width, int height) {
        BitSet done = new BitSet(width * height * width);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        double[] spheres = new double[size * 4];
        for (int i = 0; i < size; i++) {
            float t = (float) i / (float) size;
            double x = x1 + (double) t * (x2 - x1);
            double y = y1 + (double) t * (y2 - y1);
            double z = z1 + (double) t * (z2 - z1);
            double scale = random.nextDouble() * (double) size / 16.0D;
            double radius = ((double) (MathHelper.sin((float) Math.PI * t) + 1.0F) * scale + 1.0D) / 2.0D;
            spheres[i * 4] = x;
            spheres[i * 4 + 1] = y;
            spheres[i * 4 + 2] = z;
            spheres[i * 4 + 3] = radius;
        }
        // Drops spheres that sit wholly inside another.
        for (int i = 0; i < size - 1; i++) {
            if (spheres[i * 4 + 3] <= 0.0D) continue;
            for (int j = i + 1; j < size; j++) {
                if (spheres[j * 4 + 3] <= 0.0D) continue;
                double dx = spheres[i * 4] - spheres[j * 4];
                double dy = spheres[i * 4 + 1] - spheres[j * 4 + 1];
                double dz = spheres[i * 4 + 2] - spheres[j * 4 + 2];
                double dr = spheres[i * 4 + 3] - spheres[j * 4 + 3];
                if (dr * dr > dx * dx + dy * dy + dz * dz) {
                    if (dr > 0.0D) spheres[j * 4 + 3] = -1.0D;
                    else spheres[i * 4 + 3] = -1.0D;
                }
            }
        }
        for (int i = 0; i < size; i++) {
            double radius = spheres[i * 4 + 3];
            if (radius < 0.0D) continue;
            double cx = spheres[i * 4];
            double cy = spheres[i * 4 + 1];
            double cz = spheres[i * 4 + 2];
            int fromX = Math.max(MathHelper.floor(cx - radius), minX);
            int fromY = Math.max(MathHelper.floor(cy - radius), minY);
            int fromZ = Math.max(MathHelper.floor(cz - radius), minZ);
            int toX = Math.max(MathHelper.floor(cx + radius), fromX);
            int toY = Math.max(MathHelper.floor(cy + radius), fromY);
            int toZ = Math.max(MathHelper.floor(cz + radius), fromZ);
            for (int x = fromX; x <= toX; x++) {
                double fx = ((double) x + 0.5D - cx) / radius;
                if (fx * fx >= 1.0D) continue;
                for (int y = fromY; y <= toY; y++) {
                    double fy = ((double) y + 0.5D - cy) / radius;
                    if (fx * fx + fy * fy >= 1.0D) continue;
                    for (int z = fromZ; z <= toZ; z++) {
                        double fz = ((double) z + 0.5D - cz) / radius;
                        if (fx * fx + fy * fy + fz * fz >= 1.0D) continue;
                        int index = x - minX + (y - minY) * width + (z - minZ) * width * height;
                        if (done.get(index)) continue;
                        done.set(index);
                        at.setPos(x, y, z);
                        if (y >= 0 && y < 256 && target.test(world.getBlockState(at))) placeOre(world, at, ore);
                    }
                }
            }
        }
    }

    /** 1.16's ore that never touches air, as ancient debris generates: up to {@code size} blocks scattered round {@code pos}. */
    static void hiddenOre(World world, Random random, BlockPos pos, Predicate<IBlockState> target, IBlockState ore, int size) {
        int count = random.nextInt(size + 1);
        for (int i = 0; i < count; i++) {
            int spread = Math.min(i, 7);
            BlockPos at = pos.add(offset(random, spread), offset(random, spread), offset(random, spread));
            if (at.getY() >= 0 && at.getY() < 256 && target.test(world.getBlockState(at)) && !touchesAir(world, at)) {
                placeOre(world, at, ore);
            }
        }
    }

    /**
     * Places an ore block. Gravel goes straight into the chunk: 1.12 makes a falling block placed in the world fall
     * on its next tick, where 1.16 leaves generated gravel hanging until something disturbs it. Ores only replace
     * stone that's as opaque and as dark, so the light and heightmap stay as they were.
     */
    private static void placeOre(World world, BlockPos pos, IBlockState ore) {
        if (ore.getBlock() instanceof BlockFalling) {
            Chunk chunk = world.getChunk(pos);
            ExtendedBlockStorage storage = chunk.getBlockStorageArray()[pos.getY() >> 4];
            IBlockState old = chunk.getBlockState(pos);
            if (storage != Chunk.NULL_BLOCK_STORAGE && old.getLightOpacity(world, pos) == ore.getLightOpacity(world, pos)
                    && old.getLightValue(world, pos) == ore.getLightValue(world, pos) && !old.getBlock().hasTileEntity(old)) {
                storage.set(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, ore);
                chunk.markDirty();
                world.notifyBlockUpdate(pos, old, ore, 2);
                return;
            }
        }
        world.setBlockState(pos, ore, GenFlags.GENERATION);
    }

    private static int offset(Random random, int spread) {
        return Math.round((random.nextFloat() - random.nextFloat()) * (float) spread);
    }

    private static boolean touchesAir(World world, BlockPos pos) {
        for (EnumFacing facing : DIRECTIONS) {
            if (world.isAirBlock(pos.offset(facing))) return true;
        }
        return false;
    }

    // ---- Basalt ----

    /** A basalt pillar from a ceiling down to the floor, ragged at the sides, with a splash of basalt round its foot. */
    static void basaltPillar(World world, Random random, BlockPos pos) {
        if (!world.isAirBlock(pos) || world.isAirBlock(pos.up()) || PortiumBlocks.BASALT == null) return;
        IBlockState basalt = PortiumBlocks.BASALT.getDefaultState();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(pos);
        boolean north = true;
        boolean south = true;
        boolean west = true;
        boolean east = true;
        while (world.isAirBlock(at)) {
            if (at.getY() < 0 || at.getY() >= 256) return;
            world.setBlockState(at, basalt, GenFlags.GENERATION);
            north = north && pillarSide(world, random, at.north(), basalt);
            south = south && pillarSide(world, random, at.south(), basalt);
            west = west && pillarSide(world, random, at.west(), basalt);
            east = east && pillarSide(world, random, at.east(), basalt);
            at.move(EnumFacing.DOWN);
        }
        at.move(EnumFacing.UP);
        pillarFoot(world, random, at.north(), basalt);
        pillarFoot(world, random, at.south(), basalt);
        pillarFoot(world, random, at.west(), basalt);
        pillarFoot(world, random, at.east(), basalt);
        at.move(EnumFacing.DOWN);
        BlockPos.MutableBlockPos splash = new BlockPos.MutableBlockPos();
        for (int dx = -3; dx < 4; dx++) {
            for (int dz = -3; dz < 4; dz++) {
                int corner = Math.abs(dx) * Math.abs(dz);
                if (random.nextInt(10) >= 10 - corner) continue;
                splash.setPos(at.getX() + dx, at.getY(), at.getZ() + dz);
                int drop = 3;
                while (world.isAirBlock(splash.down())) {
                    splash.move(EnumFacing.DOWN);
                    if (--drop <= 0) break;
                }
                if (!world.isAirBlock(splash.down())) world.setBlockState(splash, basalt, GenFlags.GENERATION);
            }
        }
    }

    private static void pillarFoot(World world, Random random, BlockPos pos, IBlockState basalt) {
        if (random.nextBoolean()) world.setBlockState(pos, basalt, GenFlags.GENERATION);
    }

    private static boolean pillarSide(World world, Random random, BlockPos pos, IBlockState basalt) {
        if (random.nextInt(10) == 0) return false;
        world.setBlockState(pos, basalt, GenFlags.GENERATION);
        return true;
    }

    /** A basalt delta: a pool of lava sunk into the ground, rimmed with magma most of the time. */
    static void delta(World world, Random random, BlockPos pos) {
        boolean rimmed = random.nextDouble() < 0.9D;
        int rimX = rimmed ? spread(random, 0, 2) : 0;
        int rimZ = rimmed ? spread(random, 0, 2) : 0;
        boolean rim = rimmed && rimX != 0 && rimZ != 0;
        int sizeX = spread(random, 3, 4);
        int sizeZ = spread(random, 3, 4);
        int size = Math.max(sizeX, sizeZ);
        IBlockState lava = Blocks.LAVA.getDefaultState();
        IBlockState magma = Blocks.MAGMA.getDefaultState();
        for (ProximityBox box = new ProximityBox(pos, sizeX, 0, sizeZ); box.next(); ) {
            BlockPos at = box.pos();
            if (manhattan(at, pos) > size) break;
            if (!deltaSpot(world, at)) continue;
            if (rim) setLoaded(world, at, magma);
            BlockPos inner = at.add(rimX, 0, rimZ);
            // Placed lava checks its sides for water, so they must be loaded too.
            if (deltaSpot(world, inner) && world.isAreaLoaded(inner.add(-1, 0, -1), inner.add(1, 0, 1))) setLoaded(world, inner, lava);
        }
    }

    /** Ground open only to the sky above it, and not already lava or part of a structure. */
    private static boolean deltaSpot(World world, BlockPos pos) {
        Block block = loaded(world, pos).getBlock();
        if (block == Blocks.LAVA || block == Blocks.FLOWING_LAVA) return false;
        if (block == Blocks.BEDROCK || isNetherStructure(block)) return false;
        for (EnumFacing facing : DIRECTIONS) {
            boolean air = isAir(world, pos.offset(facing), loaded(world, pos.offset(facing)));
            if (air != (facing == EnumFacing.UP)) return false;
        }
        return true;
    }

    private static boolean isNetherStructure(Block block) {
        return block == Blocks.NETHER_BRICK || block == Blocks.NETHER_BRICK_FENCE || block == Blocks.NETHER_BRICK_STAIRS || block == Blocks.NETHER_WART
                || block == Blocks.CHEST || block == Blocks.MOB_SPAWNER;
    }

    /** Clusters of basalt columns rising from floors (and the lava sea), each up to {@code height} tall. */
    static void basaltColumns(World world, Random random, BlockPos pos, int reachBase, int reachSpread, int heightBase, int heightSpread) {
        if (PortiumBlocks.BASALT == null || !canStartColumn(world, new BlockPos.MutableBlockPos(pos))) return;
        int height = spread(random, heightBase, heightSpread);
        boolean small = random.nextFloat() < 0.9F;
        int radius = Math.min(height, small ? 5 : 8);
        int count = small ? 50 : 15;
        int width = radius * 2 + 1;
        for (int i = 0; i < count; i++) {
            int x = pos.getX() - radius + random.nextInt(width);
            int y = pos.getY() + random.nextInt(1);
            int z = pos.getZ() - radius + random.nextInt(width);
            BlockPos at = new BlockPos(x, y, z);
            int columnHeight = height - manhattan(at, pos);
            if (columnHeight >= 0) columnCluster(world, at, columnHeight, spread(random, reachBase, reachSpread));
        }
    }

    private static void columnCluster(World world, BlockPos center, int height, int reach) {
        IBlockState basalt = PortiumBlocks.BASALT.getDefaultState();
        for (int z = center.getZ() - reach; z <= center.getZ() + reach; z++) {
            for (int x = center.getX() - reach; x <= center.getX() + reach; x++) {
                BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, center.getY(), z);
                int distance = Math.abs(x - center.getX()) + Math.abs(z - center.getZ());
                BlockPos.MutableBlockPos start = isOpenOrLavaSea(world, at) ? findColumnFloor(world, at, distance) : findColumnSpace(world, at, distance);
                if (start == null) continue;
                for (int left = height - distance / 2; left >= 0; left--) {
                    if (isOpenOrLavaSea(world, start)) {
                        setLoaded(world, start, basalt);
                        start.move(EnumFacing.UP);
                    } else {
                        if (loaded(world, start).getBlock() != PortiumBlocks.BASALT) break;
                        start.move(EnumFacing.UP);
                    }
                }
            }
        }
    }

    @Nullable
    private static BlockPos.MutableBlockPos findColumnFloor(World world, BlockPos.MutableBlockPos at, int distance) {
        while (at.getY() > 1 && distance > 0) {
            distance--;
            if (canStartColumn(world, at)) return at;
            at.move(EnumFacing.DOWN);
        }
        return null;
    }

    @Nullable
    private static BlockPos.MutableBlockPos findColumnSpace(World world, BlockPos.MutableBlockPos at, int distance) {
        while (at.getY() < 256 && distance > 0) {
            distance--;
            IBlockState state = loaded(world, at);
            if (blocksColumns(state.getBlock())) return null;
            if (isAir(world, at, state)) return at;
            at.move(EnumFacing.UP);
        }
        return null;
    }

    private static boolean canStartColumn(World world, BlockPos.MutableBlockPos at) {
        if (!isOpenOrLavaSea(world, at)) return false;
        BlockPos below = at.down();
        IBlockState under = loaded(world, below);
        return !isAir(world, below, under) && !blocksColumns(under.getBlock());
    }

    private static boolean isOpenOrLavaSea(World world, BlockPos pos) {
        IBlockState state = loaded(world, pos);
        Block block = state.getBlock();
        return isAir(world, pos, state) || (block == Blocks.LAVA || block == Blocks.FLOWING_LAVA) && pos.getY() <= SEA_LEVEL;
    }

    private static boolean blocksColumns(Block block) {
        return block == Blocks.LAVA || block == Blocks.FLOWING_LAVA || block == Blocks.BEDROCK || block == Blocks.MAGMA || block == Blocks.SOUL_SAND
                || isNetherStructure(block);
    }

    /** Turns the {@code target} blocks within a random distance of the first one below {@code pos} into {@code replacement}. */
    static void replaceBlob(World world, Random random, BlockPos pos, Block target, IBlockState replacement, int radiusBase, int radiusSpread) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(pos.getX(), MathHelper.clamp(pos.getY(), 1, 255), pos.getZ());
        while (true) {
            if (at.getY() <= 1) return;
            if (world.getBlockState(at).getBlock() == target) break;
            at.move(EnumFacing.DOWN);
        }
        BlockPos center = at.toImmutable();
        int radius = spread(random, radiusBase, radiusSpread);
        for (ProximityBox box = new ProximityBox(center, radius, radius, radius); box.next(); ) {
            BlockPos spot = box.pos();
            if (manhattan(spot, center) > radius) break;
            if (loaded(world, spot).getBlock() == target) setLoaded(world, spot, replacement);
        }
    }

    // ---- Shared ----

    /** 1.16's FeatureSpread: {@code base}, plus up to {@code spread} more. */
    static int spread(Random random, int base, int spread) {
        return spread == 0 ? base : base + random.nextInt(spread + 1);
    }

    static int manhattan(BlockPos a, BlockPos b) {
        float dx = (float) Math.abs(a.getX() - b.getX());
        float dy = (float) Math.abs(a.getY() - b.getY());
        float dz = (float) Math.abs(a.getZ() - b.getZ());
        return (int) (dx + dy + dz);
    }

    static boolean isAir(World world, BlockPos pos, IBlockState state) {
        return state.getMaterial() == Material.AIR || state.getBlock().isAir(state, world, pos);
    }

    /** A block, or bedrock where its chunk isn't loaded. */
    private static IBlockState loaded(World world, BlockPos pos) {
        if (pos.getY() < 0 || pos.getY() >= 256) return Blocks.AIR.getDefaultState();
        return world.isBlockLoaded(pos) ? world.getBlockState(pos) : Blocks.BEDROCK.getDefaultState();
    }

    private static void setLoaded(World world, BlockPos pos, IBlockState state) {
        if (world.isBlockLoaded(pos)) world.setBlockState(pos, state, GenFlags.GENERATION);
    }

    /**
     * 1.16's BlockPos.getProximitySortedBoxPositionsIterator: the spots of a box around {@code center} in rings of
     * growing Manhattan distance, each spot followed by its mirror across the centre's z.
     */
    static final class ProximityBox {

        private final int centerX;
        private final int centerY;
        private final int centerZ;
        private final int radiusX;
        private final int radiusY;
        private final int radiusZ;
        private final int maxDepth;
        private int depth;
        private int maxX;
        private int maxY;
        private int x;
        private int y;
        private boolean mirror;
        private int posX;
        private int posY;
        private int posZ;

        ProximityBox(BlockPos center, int radiusX, int radiusY, int radiusZ) {
            this.centerX = center.getX();
            this.centerY = center.getY();
            this.centerZ = center.getZ();
            this.radiusX = radiusX;
            this.radiusY = radiusY;
            this.radiusZ = radiusZ;
            this.maxDepth = radiusX + radiusY + radiusZ;
        }

        boolean next() {
            if (this.mirror) {
                this.mirror = false;
                this.posZ = this.centerZ - (this.posZ - this.centerZ);
                return true;
            }
            while (true) {
                if (this.y > this.maxY) {
                    this.x++;
                    if (this.x > this.maxX) {
                        this.depth++;
                        if (this.depth > this.maxDepth) return false;
                        this.maxX = Math.min(this.radiusX, this.depth);
                        this.x = -this.maxX;
                    }
                    this.maxY = Math.min(this.radiusY, this.depth - Math.abs(this.x));
                    this.y = -this.maxY;
                }
                int dx = this.x;
                int dy = this.y;
                int dz = this.depth - Math.abs(dx) - Math.abs(dy);
                this.y++;
                if (dz <= this.radiusZ) {
                    this.mirror = dz != 0;
                    this.posX = this.centerX + dx;
                    this.posY = this.centerY + dy;
                    this.posZ = this.centerZ + dz;
                    return true;
                }
            }
        }

        BlockPos pos() {
            return new BlockPos(this.posX, this.posY, this.posZ);
        }
    }

    private NetherFeatures() {}
}
