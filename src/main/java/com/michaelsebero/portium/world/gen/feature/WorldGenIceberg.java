package com.michaelsebero.portium.world.gen.feature;

import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Random;

/**
 * Icebergs (1.13 IcebergFeature): rounded or pointed packed-ice (or blue ice) shapes floating at sea level with a
 * snowy top, a submerged base, and sometimes a melted channel through them. {@code pos} is at sea level.
 * Adapted from Backportium's port of the vanilla generator.
 */
public class WorldGenIceberg extends WorldGenerator {

    private final IBlockState ice;

    public WorldGenIceberg(IBlockState ice) {
        super(false);
        this.ice = ice;
    }

    @Override
    public boolean generate(World world, Random rand, BlockPos pos) {
        boolean snowy = rand.nextDouble() > 0.7D;
        double angle = rand.nextDouble() * 2.0D * Math.PI;
        int ellipseA = 11 - rand.nextInt(5);
        int ellipseB = 3 + rand.nextInt(3);
        boolean rounded = rand.nextDouble() > 0.7D;
        int height = rounded ? rand.nextInt(6) + 6 : rand.nextInt(15) + 3;
        if (!rounded && rand.nextDouble() > 0.9D) height += rand.nextInt(19) + 7;
        int depth = Math.min(height + rand.nextInt(11), 18);
        int width = Math.min(height + rand.nextInt(7) - rand.nextInt(5), 11);
        int radius = rounded ? ellipseA : 11;

        for (int x = -radius; x < radius; x++) {
            for (int z = -radius; z < radius; z++) {
                for (int y = 0; y < height; y++) {
                    int ring = rounded ? roundedProfile(y, height, width) : pointedProfile(rand, y, height, width);
                    if (rounded || x < ring) this.placeAt(world, rand, pos, height, x, y, z, ring, radius, rounded, ellipseB, angle, snowy);
                }
            }
        }
        this.smoothOverhangs(world, pos, width, height, rounded, ellipseA);
        for (int x = -radius; x < radius; x++) {
            for (int z = -radius; z < radius; z++) {
                for (int y = -1; y > -depth; y--) {
                    int r = rounded ? MathHelper.ceil(radius * (1.0F - (float) Math.pow(y, 2.0D) / (depth * 8.0F))) : radius;
                    int ring = baseProfile(rand, -y, depth, width);
                    if (x < ring) this.placeAt(world, rand, pos, depth, x, y, z, ring, r, rounded, ellipseB, angle, snowy);
                }
            }
        }
        boolean carve = rounded ? rand.nextDouble() > 0.1D : rand.nextDouble() > 0.7D;
        if (carve) this.carveChannel(rand, world, width, height, pos, rounded, ellipseA, angle, ellipseB);
        return true;
    }

    private void carveChannel(Random rand, World world, int width, int height, BlockPos pos, boolean rounded, int ellipseA, double angle, int ellipseB) {
        int signX = rand.nextBoolean() ? -1 : 1;
        int signZ = rand.nextBoolean() ? -1 : 1;
        int offsetX = rand.nextInt(Math.max(width / 2 - 2, 1));
        if (rand.nextBoolean()) offsetX = width / 2 + 1 - rand.nextInt(Math.max(width - width / 2 - 1, 1));
        int offsetZ = rand.nextInt(Math.max(width / 2 - 2, 1));
        if (rand.nextBoolean()) offsetZ = width / 2 + 1 - rand.nextInt(Math.max(width - width / 2 - 1, 1));
        if (rounded) offsetX = offsetZ = rand.nextInt(Math.max(ellipseA - 5, 1));
        BlockPos center = new BlockPos(signX * offsetX, 0, signZ * offsetZ);
        double channelAngle = rounded ? angle + Math.PI / 2.0D : rand.nextDouble() * 2.0D * Math.PI;
        for (int y = 0; y < height - 3; y++) {
            this.carveLayer(pointedProfile(rand, y, height, width), y, pos, world, false, channelAngle, center, ellipseA, ellipseB);
        }
        for (int y = -1; y > -height + rand.nextInt(5); y--) {
            this.carveLayer(baseProfile(rand, -y, height, width), y, pos, world, true, channelAngle, center, ellipseA, ellipseB);
        }
    }

    private void carveLayer(int ring, int y, BlockPos pos, World world, boolean underwater, double angle, BlockPos center, int ellipseA, int ellipseB) {
        int a = ring + 1 + ellipseA / 3;
        int b = Math.min(ring - 3, 3) + ellipseB / 2 - 1;
        for (int x = -a; x < a; x++) {
            for (int z = -a; z < a; z++) {
                if (ellipse(x, z, center, a, b, angle) < 0.0D) {
                    BlockPos target = pos.add(x, y, z);
                    Block block = world.getBlockState(target).getBlock();
                    if (isIce(block) || block == Blocks.SNOW) {
                        if (underwater) {
                            this.setBlockAndNotifyAdequately(world, target, Blocks.WATER.getDefaultState());
                        } else {
                            this.setBlockAndNotifyAdequately(world, target, Blocks.AIR.getDefaultState());
                            if (world.getBlockState(target.up()).getBlock() == Blocks.SNOW) this.setBlockAndNotifyAdequately(world, target.up(), Blocks.AIR.getDefaultState());
                        }
                    }
                }
            }
        }
    }

    private void placeAt(World world, Random rand, BlockPos pos, int height, int x, int y, int z, int ring, int radius, boolean rounded, int ellipseB, double angle, boolean snowy) {
        double distance = rounded
                ? ellipse(x, z, BlockPos.ORIGIN, radius, narrowTop(y, height, ellipseB), angle)
                : jaggedCircle(x, z, ring, rand);
        if (distance >= 0.0D) return;
        double edge = rounded ? -0.5D : -6 - rand.nextInt(3);
        if (distance > edge && rand.nextDouble() > 0.9D) return;
        this.placeIceOrSnow(pos.add(x, y, z), world, rand, height - y, height, rounded, snowy);
    }

    private void placeIceOrSnow(BlockPos pos, World world, Random rand, int heightRemaining, int height, boolean lessSnow, boolean snowy) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!world.isAirBlock(pos) && block != Blocks.SNOW && block != Blocks.ICE && block != Blocks.WATER) return;
        boolean allowSnow = !lessSnow || rand.nextDouble() > 0.05D;
        int divisor = lessSnow ? 3 : 2;
        if (snowy && block != Blocks.WATER && heightRemaining <= rand.nextInt(Math.max(1, height / divisor)) + height * 0.6D && allowSnow) {
            this.setBlockAndNotifyAdequately(world, pos, Blocks.SNOW.getDefaultState());
        } else {
            this.setBlockAndNotifyAdequately(world, pos, this.ice);
        }
    }

    /** Removes floating ice and thin edges left by the shaping passes. */
    private void smoothOverhangs(World world, BlockPos pos, int width, int height, boolean rounded, int ellipseA) {
        int extent = rounded ? ellipseA : width / 2;
        for (int x = -extent; x <= extent; x++) {
            for (int z = -extent; z <= extent; z++) {
                for (int y = 0; y <= height; y++) {
                    BlockPos target = pos.add(x, y, z);
                    Block block = world.getBlockState(target).getBlock();
                    if (!isIce(block) && block != Blocks.SNOW) continue;
                    if (world.isAirBlock(target.down())) {
                        this.setBlockAndNotifyAdequately(world, target, Blocks.AIR.getDefaultState());
                        this.setBlockAndNotifyAdequately(world, target.up(), Blocks.AIR.getDefaultState());
                    } else if (isIce(block)) {
                        int open = 0;
                        if (!isIce(world.getBlockState(target.west()).getBlock())) open++;
                        if (!isIce(world.getBlockState(target.east()).getBlock())) open++;
                        if (!isIce(world.getBlockState(target.north()).getBlock())) open++;
                        if (!isIce(world.getBlockState(target.south()).getBlock())) open++;
                        if (open >= 3) this.setBlockAndNotifyAdequately(world, target, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
    }

    private static int narrowTop(int y, int height, int value) {
        return y > 0 && height - y <= 3 ? value - (4 - (height - y)) : value;
    }

    private static double jaggedCircle(int x, int z, int radius, Random rand) {
        float jitter = 10.0F * MathHelper.clamp(rand.nextFloat(), 0.2F, 0.8F) / radius;
        return jitter + x * x + z * z - radius * radius;
    }

    /** Negative inside a rotated ellipse with semi-axes {@code a} and {@code b}. */
    private static double ellipse(int x, int z, BlockPos center, int a, int b, double angle) {
        double dx = x - center.getX();
        double dz = z - center.getZ();
        double u = (dx * Math.cos(angle) - dz * Math.sin(angle)) / a;
        double v = (dx * Math.sin(angle) + dz * Math.cos(angle)) / b;
        return u * u + v * v - 1.0D;
    }

    private static int pointedProfile(Random rand, int y, int height, int width) {
        float spread = 3.5F - rand.nextFloat();
        float size = (1.0F - (float) Math.pow(y, 2.0D) / (height * spread)) * width;
        if (height > 15 + rand.nextInt(5)) {
            int adjusted = y < 3 + rand.nextInt(6) ? y / 2 : y;
            size = (1.0F - (float) adjusted / (height * spread * 0.4F)) * width;
        }
        return MathHelper.ceil(size / 2.0F);
    }

    private static int roundedProfile(int y, int height, int width) {
        return MathHelper.ceil((1.0F - (float) Math.pow(y, 2.0D) / height) * width / 2.0F);
    }

    private static int baseProfile(Random rand, int depthBelow, int depth, int width) {
        float spread = 1.0F + rand.nextFloat() / 2.0F;
        return MathHelper.ceil((1.0F - (float) depthBelow / (depth * spread)) * width / 2.0F);
    }

    private static boolean isIce(Block block) {
        return block == Blocks.PACKED_ICE || block == Blocks.SNOW || block == PortiumBlocks.BLUE_ICE;
    }
}
