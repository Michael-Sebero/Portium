package com.michaelsebero.portium.world.v116.feature;

import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.world.v116.GenFlags;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Huge crimson and warped fungi (1.16's HugeFungusFeature): a stem 4-13 tall (twice that one time in twelve), a cap of
 * wart blocks studded with shroomlights, and on crimson ones weeping vines. One in 17 generated fungi has a 3x3 stem.
 * A planted one (from bone meal) clears what's in its way; a generated one only grows into free space.
 */
public final class HugeFungus {

    public static boolean place(World world, Random random, BlockPos pos, boolean warped, boolean planted) {
        Block nylium = warped ? PortiumBlocks.WARPED_NYLIUM : PortiumBlocks.CRIMSON_NYLIUM;
        Block stemBlock = warped ? PortiumBlocks.WARPED_STEM : PortiumBlocks.CRIMSON_STEM;
        Block hatBlock = warped ? PortiumBlocks.WARPED_WART_BLOCK : Blocks.NETHER_WART_BLOCK;
        if (nylium == null || stemBlock == null || hatBlock == null || PortiumBlocks.SHROOMLIGHT == null) return false;
        if (world.getBlockState(pos.down()).getBlock() != nylium) return false;
        int height = MathHelper.getInt(random, 4, 13);
        if (random.nextInt(12) == 0) height *= 2;
        // The generator's height: 128 in the Nether.
        if (!planted && pos.getY() + height + 1 >= world.getActualHeight()) return false;
        boolean thick = !planted && random.nextFloat() < 0.06F;
        world.setBlockState(pos, Blocks.AIR.getDefaultState(), planted ? 4 : 4 | 16);
        Shape shape = new Shape(world, random, planted, stemBlock.getDefaultState(), hatBlock.getDefaultState(),
                PortiumBlocks.SHROOMLIGHT.getDefaultState(), hatBlock == Blocks.NETHER_WART_BLOCK);
        shape.stems(pos, height, thick);
        shape.hat(pos, height, thick);
        return true;
    }

    private static final class Shape {

        final World world;
        final Random random;
        final boolean planted;
        final IBlockState stem;
        final IBlockState hat;
        final IBlockState decor;
        final boolean crimson;

        Shape(World world, Random random, boolean planted, IBlockState stem, IBlockState hat, IBlockState decor, boolean crimson) {
            this.world = world;
            this.random = random;
            this.planted = planted;
            this.stem = stem;
            this.hat = hat;
            this.decor = decor;
            this.crimson = crimson;
        }

        /** A spot the fungus may fill: replaceable, or (for the stem) a plant in the way. */
        boolean isFree(BlockPos pos, boolean plantsToo) {
            Material material = this.world.getBlockState(pos).getMaterial();
            return material.isReplaceable() || plantsToo && material == Material.PLANTS;
        }

        void set(BlockPos pos, IBlockState state) {
            this.world.setBlockState(pos, state, this.planted ? 3 : GenFlags.GENERATION);
        }

        /** Clears a spot for a planted fungus, dropping what was there, unless the spot below is open. */
        void clearForPlanted(BlockPos pos) {
            if (this.planted && !this.world.isAirBlock(pos.down())) this.world.destroyBlock(pos, true);
        }

        void stems(BlockPos origin, int height, boolean thick) {
            int radius = thick ? 1 : 0;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    boolean corner = thick && Math.abs(dx) == radius && Math.abs(dz) == radius;
                    for (int dy = 0; dy < height; dy++) {
                        BlockPos pos = origin.add(dx, dy, dz);
                        if (!this.isFree(pos, true)) continue;
                        if (this.planted) {
                            this.clearForPlanted(pos);
                            this.world.setBlockState(pos, this.stem, 3);
                        } else if (!corner || this.random.nextFloat() < 0.1F) {
                            this.set(pos, this.stem);
                        }
                    }
                }
            }
        }

        void hat(BlockPos origin, int height, boolean thick) {
            int hatHeight = Math.min(this.random.nextInt(1 + height / 3) + 5, height);
            int bottom = height - hatHeight;
            for (int dy = bottom; dy <= height; dy++) {
                int radius = dy < height - this.random.nextInt(3) ? 2 : 1;
                if (hatHeight > 8 && dy < bottom + 4) radius = 3;
                if (thick) radius++;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        boolean edgeX = dx == -radius || dx == radius;
                        boolean edgeZ = dz == -radius || dz == radius;
                        boolean inside = !edgeX && !edgeZ && dy != height;
                        boolean corner = edgeX && edgeZ;
                        boolean low = dy < bottom + 3;
                        BlockPos pos = origin.add(dx, dy, dz);
                        if (!this.isFree(pos, false)) continue;
                        this.clearForPlanted(pos);
                        if (low) {
                            if (!inside) this.hatWithVines(pos);
                        } else if (inside) {
                            this.fungus(pos, 0.1F, 0.2F, this.crimson ? 0.1F : 0.0F);
                        } else if (corner) {
                            this.fungus(pos, 0.01F, 0.7F, this.crimson ? 0.083F : 0.0F);
                        } else {
                            this.fungus(pos, 5.0E-4F, 0.98F, this.crimson ? 0.07F : 0.0F);
                        }
                    }
                }
            }
        }

        private void fungus(BlockPos pos, float decorChance, float hatChance, float vineChance) {
            if (this.random.nextFloat() < decorChance) {
                this.set(pos, this.decor);
            } else if (this.random.nextFloat() < hatChance) {
                this.set(pos, this.hat);
                if (this.random.nextFloat() < vineChance) this.vine(pos);
            }
        }

        private void hatWithVines(BlockPos pos) {
            if (this.world.getBlockState(pos.down()).getBlock() == this.hat.getBlock()) {
                this.set(pos, this.hat);
            } else if (this.random.nextFloat() < 0.15D) {
                this.set(pos, this.hat);
                if (this.crimson && this.random.nextInt(11) == 0) this.vine(pos);
            }
        }

        private void vine(BlockPos pos) {
            BlockPos below = pos.down();
            if (PortiumBlocks.WEEPING_VINES == null || !this.world.isAirBlock(below)) return;
            int length = MathHelper.getInt(this.random, 1, 5);
            if (this.random.nextInt(7) == 0) length *= 2;
            NetherVines.weepingColumn(this.world, this.random, below, length, 23, 25, this.planted ? GenFlags.GROWTH : GenFlags.GENERATION);
        }
    }

    private HugeFungus() {}
}
