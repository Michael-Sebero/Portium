package com.michaelsebero.portium.world.gen.bee;

import com.michaelsebero.portium.block.bee.BlockBeehive;
import com.michaelsebero.portium.entity.passive.bee.BeeFlowers;
import com.michaelsebero.portium.entity.passive.bee.EntityBee;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumEntities;
import com.michaelsebero.portium.tile.TileEntityBeehive;
import com.michaelsebero.portium.world.WorldGeneration;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.init.Biomes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 1.15's {@code BeehiveTreeDecorator}, after 1.12 grows an oak, fancy oak or birch: a bee nest with two or three bees
 * on the trunk under the lowest leaves, facing south. In 1.16.5 worlds the chance goes by biome (plains 5%, flower
 * forest 2%, forests and birch forests 0.2%); a sapling grown with flowers within two blocks gets 5%, in any world.
 */
public final class BeeNests {

    private static final ThreadLocal<Boolean> FROM_SAPLING = ThreadLocal.withInitial(() -> false);

    public static void growingSapling(boolean growing) {
        FROM_SAPLING.set(growing);
    }

    public static void afterTree(World world, Random random, BlockPos base) {
        if (PortiumBlocks.BEE_NEST == null || world.isRemote || !isOakOrBirch(world.getBlockState(base))) return;
        float chance;
        if (FROM_SAPLING.get()) {
            chance = hasFlowersNear(world, base) ? 0.05F : 0.0F;
        } else {
            chance = WorldGeneration.is116(world) ? biomeChance(world.getBiome(base)) : 0.0F;
        }
        if (chance <= 0.0F || random.nextFloat() >= chance) return;
        List<BlockPos> logs = new ArrayList<>();
        for (BlockPos pos = base; world.getBlockState(pos).getBlock() instanceof BlockLog && logs.size() < 32; pos = pos.up()) logs.add(pos);
        if (logs.isEmpty()) return;
        int lowestLeaf = lowestLeafY(world, base, logs.get(logs.size() - 1).getY() + 3);
        EnumFacing side = BlockBeehive.getGenerationDirection(random);
        int y = lowestLeaf != Integer.MAX_VALUE ? Math.max(lowestLeaf - 1, logs.get(0).getY())
                : Math.min(logs.get(0).getY() + 1 + random.nextInt(3), logs.get(logs.size() - 1).getY());
        BlockPos log = null;
        for (BlockPos pos : logs) {
            if (pos.getY() == y) log = pos;
        }
        if (log == null) return;
        BlockPos nest = log.offset(side);
        if (!world.isAirBlock(nest) || !world.isAirBlock(nest.offset(EnumFacing.SOUTH))) return;
        world.setBlockState(nest, PortiumBlocks.BEE_NEST.getDefaultState().withProperty(BlockBeehive.FACING, EnumFacing.SOUTH), 2);
        TileEntity tile = world.getTileEntity(nest);
        if (tile instanceof TileEntityBeehive && PortiumEntities.isRegistered(EntityBee.class)) {
            int bees = 2 + random.nextInt(2);
            for (int i = 0; i < bees; ++i) ((TileEntityBeehive) tile).tryEnterHive(new EntityBee(world), false, random.nextInt(599));
        }
    }

    /** Only oak and birch trees (1.12's old log variants) get nests. */
    private static boolean isOakOrBirch(net.minecraft.block.state.IBlockState state) {
        if (!(state.getBlock() instanceof net.minecraft.block.BlockOldLog)) return false;
        net.minecraft.block.BlockPlanks.EnumType type = state.getValue(net.minecraft.block.BlockOldLog.VARIANT);
        return type == net.minecraft.block.BlockPlanks.EnumType.OAK || type == net.minecraft.block.BlockPlanks.EnumType.BIRCH;
    }

    private static float biomeChance(Biome biome) {
        if (biome == Biomes.PLAINS || biome == Biomes.MUTATED_PLAINS) return 0.05F;
        if (biome == Biomes.MUTATED_FOREST) return 0.02F;
        if (biome == Biomes.FOREST || biome == Biomes.FOREST_HILLS || biome == Biomes.BIRCH_FOREST || biome == Biomes.BIRCH_FOREST_HILLS
                || biome == Biomes.MUTATED_BIRCH_FOREST || biome == Biomes.MUTATED_BIRCH_FOREST_HILLS) {
            return 0.002F;
        }
        return 0.0F;
    }

    private static int lowestLeafY(World world, BlockPos base, int top) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = base.getY(); y <= top; ++y) {
            for (int dx = -4; dx <= 4; ++dx) {
                for (int dz = -4; dz <= 4; ++dz) {
                    pos.setPos(base.getX() + dx, y, base.getZ() + dz);
                    if (world.getBlockState(pos).getBlock() instanceof BlockLeaves) return y;
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    /** 1.15's {@code Tree.hasNearbyFlora}: a flower within two blocks across and one up or down. */
    private static boolean hasFlowersNear(World world, BlockPos base) {
        for (BlockPos pos : BlockPos.getAllInBoxMutable(base.add(-2, -1, -2), base.add(2, 1, 2))) {
            if (BeeFlowers.isFlower(world, pos)) return true;
        }
        return false;
    }

    private BeeNests() {}
}
