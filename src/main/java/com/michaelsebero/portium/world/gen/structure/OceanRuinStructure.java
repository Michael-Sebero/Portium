package com.michaelsebero.portium.world.gen.structure;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.block.water.WaterUtil;
import com.michaelsebero.portium.entity.monster.EntityDrowned;
import com.michaelsebero.portium.init.PortiumEntities;
import com.michaelsebero.portium.init.PortiumLootTables;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Ocean ruins (1.13): stone brick ruins in cold and temperate oceans, sandstone ones in warm oceans. Big ruins are
 * often surrounded by a cluster of small ones. Cold ruins layer a whole, a cracked and a mossy version of the same
 * template at decreasing integrity, so they come out weathered.
 */
public class OceanRuinStructure extends OceanStructure {

    private static final float LARGE_PROBABILITY = 0.3F;
    private static final float CLUSTER_PROBABILITY = 0.9F;
    private static final String[] BRICK = numbered("brick_", 1, 8);
    private static final String[] CRACKED = numbered("cracked_", 1, 8);
    private static final String[] MOSSY = numbered("mossy_", 1, 8);
    private static final String[] BIG_BRICK = {"big_brick_1", "big_brick_2", "big_brick_3", "big_brick_8"};
    private static final String[] BIG_CRACKED = {"big_cracked_1", "big_cracked_2", "big_cracked_3", "big_cracked_8"};
    private static final String[] BIG_MOSSY = {"big_mossy_1", "big_mossy_2", "big_mossy_3", "big_mossy_8"};
    private static final String[] WARM = numbered("warm_", 1, 8);
    private static final String[] BIG_WARM = numbered("big_warm_", 4, 7);

    static final String[] TEMPLATES = concat(BRICK, CRACKED, MOSSY, BIG_BRICK, BIG_CRACKED, BIG_MOSSY, WARM, BIG_WARM);

    OceanRuinStructure() {
        super("Ocean_Ruin", 16, 8, 14357621);
    }

    private static String[] numbered(String prefix, int from, int to) {
        String[] names = new String[to - from + 1];
        for (int i = from; i <= to; i++) names[i - from] = prefix + i;
        return names;
    }

    private static String[] concat(String[]... groups) {
        return java.util.Arrays.stream(groups).flatMap(java.util.Arrays::stream).toArray(String[]::new);
    }

    @Override
    protected boolean canSpawnIn(Biome biome) {
        return OceanBiomes.isOcean(biome);
    }

    @Override
    protected List<StructurePiece> createPieces(World world, Random random, int chunkX, int chunkZ, Biome biome) {
        Rotation rotation = Rotation.values()[random.nextInt(Rotation.values().length)];
        BlockPos origin = new BlockPos(chunkX << 4, 90, chunkZ << 4);
        boolean warm = OceanBiomes.isWarm(biome);
        List<StructurePiece> pieces = new ArrayList<>();
        boolean large = random.nextFloat() <= LARGE_PROBABILITY;
        addRuin(pieces, origin, rotation, random, warm, large, large ? 0.9F : 0.8F);
        if (large && random.nextFloat() <= CLUSTER_PROBABILITY) addCluster(pieces, random, rotation, origin, warm);
        return pieces;
    }

    /** Scatters 4-8 small ruins in the squares around a big one, keeping clear of it. */
    private static void addCluster(List<StructurePiece> pieces, Random random, Rotation rotation, BlockPos origin, boolean warm) {
        int x = origin.getX();
        int z = origin.getZ();
        BlockPos corner = StructureTemplate.transform(new BlockPos(15, 0, 15), Mirror.NONE, rotation, BlockPos.ORIGIN).add(x, 0, z);
        StructureBoundingBox bigRuin = new StructureBoundingBox(x, 0, z, corner.getX(), 0, corner.getZ());
        int minX = Math.min(x, corner.getX());
        int minZ = Math.min(z, corner.getZ());
        List<BlockPos> spots = new ArrayList<>();
        spots.add(new BlockPos(minX - 16 + MathHelper.getInt(random, 1, 8), 90, minZ + 16 + MathHelper.getInt(random, 1, 7)));
        spots.add(new BlockPos(minX - 16 + MathHelper.getInt(random, 1, 8), 90, minZ + MathHelper.getInt(random, 1, 7)));
        spots.add(new BlockPos(minX - 16 + MathHelper.getInt(random, 1, 8), 90, minZ - 16 + MathHelper.getInt(random, 4, 8)));
        spots.add(new BlockPos(minX + MathHelper.getInt(random, 1, 7), 90, minZ + 16 + MathHelper.getInt(random, 1, 7)));
        spots.add(new BlockPos(minX + MathHelper.getInt(random, 1, 7), 90, minZ - 16 + MathHelper.getInt(random, 4, 6)));
        spots.add(new BlockPos(minX + 16 + MathHelper.getInt(random, 1, 7), 90, minZ + 16 + MathHelper.getInt(random, 3, 8)));
        spots.add(new BlockPos(minX + 16 + MathHelper.getInt(random, 1, 7), 90, minZ + MathHelper.getInt(random, 1, 7)));
        spots.add(new BlockPos(minX + 16 + MathHelper.getInt(random, 1, 7), 90, minZ - 16 + MathHelper.getInt(random, 4, 8)));
        int count = MathHelper.getInt(random, 4, 8);
        for (int i = 0; i < count && !spots.isEmpty(); i++) {
            BlockPos spot = spots.remove(random.nextInt(spots.size()));
            Rotation smallRotation = Rotation.values()[random.nextInt(Rotation.values().length)];
            BlockPos far = StructureTemplate.transform(new BlockPos(5, 0, 6), Mirror.NONE, smallRotation, BlockPos.ORIGIN).add(spot.getX(), 0, spot.getZ());
            StructureBoundingBox smallRuin = new StructureBoundingBox(spot.getX(), 0, spot.getZ(), far.getX(), 0, far.getZ());
            if (!smallRuin.intersectsWith(bigRuin)) addRuin(pieces, spot, smallRotation, random, warm, false, 0.8F);
        }
    }

    private static void addRuin(List<StructurePiece> pieces, BlockPos origin, Rotation rotation, Random random, boolean warm, boolean large, float integrity) {
        if (warm) {
            String[] names = large ? BIG_WARM : WARM;
            addPiece(pieces, names[random.nextInt(names.length)], origin, rotation, integrity, large);
        } else {
            int index = random.nextInt(large ? BIG_BRICK.length : BRICK.length);
            addPiece(pieces, (large ? BIG_BRICK : BRICK)[index], origin, rotation, integrity, large);
            addPiece(pieces, (large ? BIG_CRACKED : CRACKED)[index], origin, rotation, 0.7F, large);
            addPiece(pieces, (large ? BIG_MOSSY : MOSSY)[index], origin, rotation, 0.5F, large);
        }
    }

    private static void addPiece(List<StructurePiece> pieces, String name, BlockPos origin, Rotation rotation, float integrity, boolean large) {
        StructureTemplate template = StructureTemplates.get(new ResourceLocation(Tags.MOD_ID, "underwater_ruin/" + name));
        if (template != null) pieces.add(new Piece(template, origin, rotation, integrity, large));
    }

    static class Piece extends StructurePiece {

        private final StructureTemplate template;
        private final BlockPos origin;
        private final Rotation rotation;
        private final float integrity;
        private final boolean large;

        Piece(StructureTemplate template, BlockPos origin, Rotation rotation, float integrity, boolean large) {
            this.template = template;
            this.origin = origin;
            this.rotation = rotation;
            this.integrity = integrity;
            this.large = large;
        }

        private StructureTemplate.Settings settings(@Nullable StructureBoundingBox clip) {
            return new StructureTemplate.Settings().rotation(this.rotation).integrity(this.integrity).ignoreAir().clip(clip);
        }

        @Override
        public StructureBoundingBox getHorizontalBounds() {
            return this.template.getBoundingBox(this.origin, this.settings(null));
        }

        /**
         * Sits on the ocean floor at its origin, but drops down to the lowest floor under it when the ground slopes
         * away under most of its width (1.13).
         */
        @Override
        public int resolveY(World world, Random random) {
            BlockPos size = this.template.getSize();
            BlockPos far = StructureTemplate.transform(new BlockPos(size.getX() - 1, 0, size.getZ() - 1), Mirror.NONE, this.rotation, BlockPos.ORIGIN).add(this.origin);
            int start = TerrainHeights.oceanFloor(world, this.origin.getX(), this.origin.getZ());
            if (start < 0) start = this.firstLoadedFloor(world, far);
            if (start < 0) return world.getSeaLevel() - 10;
            int lowest = 512;
            int below = 0;
            for (BlockPos column : BlockPos.getAllInBoxMutable(new BlockPos(this.origin.getX(), 0, this.origin.getZ()), new BlockPos(far.getX(), 0, far.getZ()))) {
                int y = start - 1;
                IBlockState state = TerrainHeights.blockState(world, column.getX(), y, column.getZ());
                if (state == null) continue;
                while (y > 1 && (state.getMaterial() == Material.AIR || state.getMaterial() == Material.WATER || state.getMaterial() == Material.ICE
                        || state.getMaterial() == Material.PACKED_ICE)) {
                    state = TerrainHeights.blockState(world, column.getX(), --y, column.getZ());
                }
                lowest = Math.min(lowest, y);
                if (y < start - 3) ++below;
            }
            int width = Math.abs(this.origin.getX() - far.getX());
            return start - 1 - lowest > 2 && below > width - 2 ? lowest + 1 : start;
        }

        private int firstLoadedFloor(World world, BlockPos far) {
            for (BlockPos column : BlockPos.getAllInBoxMutable(new BlockPos(this.origin.getX(), 0, this.origin.getZ()), new BlockPos(far.getX(), 0, far.getZ()))) {
                int height = TerrainHeights.oceanFloor(world, column.getX(), column.getZ());
                if (height >= 0) return height;
            }
            return -1;
        }

        @Override
        public void place(World world, int y, StructureBoundingBox clip, Random random) {
            this.template.place(world, new BlockPos(this.origin.getX(), y, this.origin.getZ()), this.settings(clip), (pos, marker) -> {
                if ("chest".equals(marker)) {
                    boolean water = WaterUtil.isPlainWaterSource(world.getBlockState(pos)) || Waterlogging.isOpenToWater(world, pos);
                    world.setBlockState(pos, Blocks.CHEST.getDefaultState(), 2);
                    if (water) Waterlogging.setWaterloggedQuietly(world, pos);
                    TileEntity chest = world.getTileEntity(pos);
                    if (chest instanceof TileEntityChest) {
                        ((TileEntityChest) chest).setLootTable(this.large ? PortiumLootTables.UNDERWATER_RUIN_BIG : PortiumLootTables.UNDERWATER_RUIN_SMALL, random.nextLong());
                    }
                } else if ("drowned".equals(marker)) {
                    if (PortiumEntities.isRegistered(EntityDrowned.class)) {
                        EntityDrowned drowned = new EntityDrowned(world);
                        drowned.enablePersistence();
                        drowned.moveToBlockPosAndAngles(pos, 0.0F, 0.0F);
                        drowned.onInitialSpawn(world.getDifficultyForLocation(pos), null);
                        world.spawnEntity(drowned);
                    }
                    world.setBlockState(pos, pos.getY() > world.getSeaLevel() ? Blocks.AIR.getDefaultState() : Blocks.WATER.getDefaultState(), 2);
                }
            });
        }
    }
}
