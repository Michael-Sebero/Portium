package com.michaelsebero.portium.world.v116.nether;

import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.world.v116.noise.SeedRandom;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.BitSet;
import java.util.Random;

/**
 * 1.16's Nether caves (NetherCaveCarver): wide, flattened tunnels from one chunk in five, started from every chunk
 * within 8 and carved into this one. Below y 32 they fill with lava; they never cut into the lava sea itself.
 */
final class NetherCaves {

    /** How many chunks away caves start from. */
    private static final int RANGE = 8;
    /** How long a tunnel runs, from 1.16's carver range of 4 chunks. */
    private static final int LENGTH = (4 * 2 - 1) * 16;
    private static final int MAX_HEIGHT = 128;
    private static final float PROBABILITY = 0.2F;
    private static final IBlockState AIR = Blocks.AIR.getDefaultState();
    private static final IBlockState LAVA = Blocks.LAVA.getDefaultState();

    private final long seed;

    NetherCaves(long seed) {
        this.seed = seed;
    }

    void carve(int chunkX, int chunkZ, ChunkPrimer primer) {
        SeedRandom random = new SeedRandom();
        BitSet carved = new BitSet(16 * 16 * 256);
        for (int fromX = chunkX - RANGE; fromX <= chunkX + RANGE; fromX++) {
            for (int fromZ = chunkZ - RANGE; fromZ <= chunkZ + RANGE; fromZ++) {
                // The Nether's biomes all have this one carver, first in their list.
                random.setLargeFeatureSeed(this.seed, fromX, fromZ);
                if (random.nextFloat() <= PROBABILITY) this.carveFrom(primer, random, fromX, fromZ, chunkX, chunkZ, carved);
            }
        }
    }

    private void carveFrom(ChunkPrimer primer, Random random, int fromX, int fromZ, int chunkX, int chunkZ, BitSet carved) {
        int length = LENGTH;
        int caves = random.nextInt(random.nextInt(random.nextInt(10) + 1) + 1);
        for (int i = 0; i < caves; i++) {
            double x = fromX * 16 + random.nextInt(16);
            double y = random.nextInt(MAX_HEIGHT);
            double z = fromZ * 16 + random.nextInt(16);
            int tunnels = 1;
            if (random.nextInt(4) == 0) {
                float radius = 1.0F + random.nextFloat() * 6.0F;
                this.room(primer, random.nextLong(), chunkX, chunkZ, x, y, z, radius, carved);
                tunnels += random.nextInt(4);
            }
            for (int j = 0; j < tunnels; j++) {
                float yaw = random.nextFloat() * ((float) Math.PI * 2.0F);
                float pitch = (random.nextFloat() - 0.5F) / 4.0F;
                float thickness = (random.nextFloat() * 2.0F + random.nextFloat()) * 2.0F;
                int steps = length - random.nextInt(length / 4);
                this.tunnel(primer, random.nextLong(), chunkX, chunkZ, x, y, z, thickness, yaw, pitch, 0, steps, 5.0D, carved);
            }
        }
    }

    private void room(ChunkPrimer primer, long seed, int chunkX, int chunkZ, double x, double y, double z, float radius, BitSet carved) {
        double width = 1.5D + (double) (MathHelper.sin(((float) Math.PI / 2F)) * radius);
        double height = width * 0.5D;
        this.carveSphere(primer, seed, chunkX, chunkZ, x + 1.0D, y, z, width, height, carved);
    }

    private void tunnel(ChunkPrimer primer, long seed, int chunkX, int chunkZ, double x, double y, double z, float thickness, float yaw, float pitch,
                        int step, int steps, double verticalScale, BitSet carved) {
        Random random = new Random(seed);
        int branchAt = random.nextInt(steps / 2) + steps / 4;
        boolean steep = random.nextInt(6) == 0;
        float yawChange = 0.0F;
        float pitchChange = 0.0F;
        for (int i = step; i < steps; i++) {
            double width = 1.5D + (double) (MathHelper.sin((float) Math.PI * (float) i / (float) steps) * thickness);
            double height = width * verticalScale;
            float cosPitch = MathHelper.cos(pitch);
            x += MathHelper.cos(yaw) * cosPitch;
            y += MathHelper.sin(pitch);
            z += MathHelper.sin(yaw) * cosPitch;
            pitch *= steep ? 0.92F : 0.7F;
            pitch += pitchChange * 0.1F;
            yaw += yawChange * 0.1F;
            pitchChange *= 0.9F;
            yawChange *= 0.75F;
            pitchChange += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            yawChange += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;
            if (i == branchAt && thickness > 1.0F) {
                this.tunnel(primer, random.nextLong(), chunkX, chunkZ, x, y, z, random.nextFloat() * 0.5F + 0.5F, yaw - ((float) Math.PI / 2F), pitch / 3.0F,
                        i, steps, 1.0D, carved);
                this.tunnel(primer, random.nextLong(), chunkX, chunkZ, x, y, z, random.nextFloat() * 0.5F + 0.5F, yaw + ((float) Math.PI / 2F), pitch / 3.0F,
                        i, steps, 1.0D, carved);
                return;
            }
            if (random.nextInt(4) != 0) {
                if (!reaches(chunkX, chunkZ, x, z, i, steps, thickness)) return;
                this.carveSphere(primer, seed, chunkX, chunkZ, x, y, z, width, height, carved);
            }
        }
    }

    /** Whether the tunnel can still get to the chunk being carved before it ends. */
    private static boolean reaches(int chunkX, int chunkZ, double x, double z, int step, int steps, float thickness) {
        double dx = x - (double) (chunkX * 16 + 8);
        double dz = z - (double) (chunkZ * 16 + 8);
        double left = steps - step;
        double reach = thickness + 2.0F + 16.0F;
        return dx * dx + dz * dz - left * left <= reach * reach;
    }

    private void carveSphere(ChunkPrimer primer, long seed, int chunkX, int chunkZ, double x, double y, double z, double width, double height, BitSet carved) {
        // 1.16 makes a random for every sphere, though the Nether's carving never draws from it.
        double centerX = chunkX * 16 + 8;
        double centerZ = chunkZ * 16 + 8;
        if (x < centerX - 16.0D - width * 2.0D || z < centerZ - 16.0D - width * 2.0D || x > centerX + 16.0D + width * 2.0D
                || z > centerZ + 16.0D + width * 2.0D) {
            return;
        }
        int fromX = Math.max(MathHelper.floor(x - width) - chunkX * 16 - 1, 0);
        int toX = Math.min(MathHelper.floor(x + width) - chunkX * 16 + 1, 16);
        int fromY = Math.max(MathHelper.floor(y - height) - 1, 1);
        int toY = Math.min(MathHelper.floor(y + height) + 1, MAX_HEIGHT - 8);
        int fromZ = Math.max(MathHelper.floor(z - width) - chunkZ * 16 - 1, 0);
        int toZ = Math.min(MathHelper.floor(z + width) - chunkZ * 16 + 1, 16);
        if (touchesFluid(primer, fromX, toX, fromY, toY, fromZ, toZ)) return;
        for (int localX = fromX; localX < toX; localX++) {
            double fx = ((double) (localX + chunkX * 16) + 0.5D - x) / width;
            for (int localZ = fromZ; localZ < toZ; localZ++) {
                double fz = ((double) (localZ + chunkZ * 16) + 0.5D - z) / width;
                if (fx * fx + fz * fz >= 1.0D) continue;
                for (int localY = toY; localY > fromY; localY--) {
                    double fy = ((double) localY - 0.5D - y) / height;
                    if (fy <= -0.7D || fx * fx + fy * fy + fz * fz >= 1.0D) continue;
                    int index = localX | localZ << 4 | localY << 8;
                    if (carved.get(index)) continue;
                    carved.set(index);
                    if (isCarvable(primer.getBlockState(localX, localY, localZ).getBlock())) {
                        primer.setBlockState(localX, localY, localZ, localY <= 31 ? LAVA : AIR);
                    }
                }
            }
        }
    }

    /**
     * 1.16 skips a sphere whose box has lava or water in it: checked on the box's sides, and top and bottom
     * throughout, as 1.16 does.
     */
    private static boolean touchesFluid(ChunkPrimer primer, int fromX, int toX, int fromY, int toY, int fromZ, int toZ) {
        for (int x = fromX; x < toX; x++) {
            for (int z = fromZ; z < toZ; z++) {
                for (int y = fromY - 1; y <= toY + 1; y++) {
                    if (y >= 0 && y < 256 && isSourceFluid(primer.getBlockState(x, y, z))) return true;
                    boolean edge = x == fromX || x == toX - 1 || z == fromZ || z == toZ - 1;
                    if (y != toY + 1 && !edge) y = toY;
                }
            }
        }
        return false;
    }

    private static boolean isSourceFluid(IBlockState state) {
        Material material = state.getMaterial();
        return (material == Material.LAVA || material == Material.WATER) && state.getBlock().getMetaFromState(state) == 0;
    }

    private static boolean isCarvable(Block block) {
        return block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.GRASS || block == Blocks.NETHERRACK || block == Blocks.SOUL_SAND
                || block == Blocks.NETHER_WART_BLOCK || block != null && (block == PortiumBlocks.SOUL_SOIL || block == PortiumBlocks.CRIMSON_NYLIUM
                || block == PortiumBlocks.WARPED_NYLIUM || block == PortiumBlocks.WARPED_WART_BLOCK || block == PortiumBlocks.BASALT
                || block == PortiumBlocks.BLACKSTONE);
    }
}
