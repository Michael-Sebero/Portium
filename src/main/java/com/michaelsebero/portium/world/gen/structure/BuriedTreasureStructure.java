package com.michaelsebero.portium.world.gen.structure;

import com.michaelsebero.portium.init.PortiumLootTables;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockStone;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Buried treasure (1.13): a chest buried in the stone or sandstone under a beach, in 1% of beach chunks. Treasure maps
 * from shipwrecks and ruins lead to these.
 */
public class BuriedTreasureStructure extends OceanStructure {

    private static final int SALT = 10387320;

    BuriedTreasureStructure() {
        super("Buried_Treasure", 1, 0, SALT);
    }

    @Override
    protected boolean canSpawnIn(Biome biome) {
        return OceanBiomes.isBeach(biome);
    }

    @Override
    public boolean hasStartAt(World world, int chunkX, int chunkZ) {
        if (!this.canSpawnIn(startBiome(world, chunkX, chunkZ))) return false;
        return new Random(chunkX * 341873128712L + chunkZ * 132897987541L + world.getSeed() + SALT).nextFloat() < 0.01F;
    }

    @Override
    public ChunkPos getStartInRegion(long seed, int regionX, int regionZ) {
        return new ChunkPos(regionX, regionZ);
    }

    @Override
    public BlockPos getLocatorPos(int chunkX, int chunkZ) {
        return new BlockPos((chunkX << 4) + 9, 0, (chunkZ << 4) + 9);
    }

    @Override
    protected List<StructurePiece> createPieces(World world, Random random, int chunkX, int chunkZ, Biome biome) {
        return Collections.singletonList(new Piece((chunkX << 4) + 9, (chunkZ << 4) + 9));
    }

    static class Piece extends StructurePiece {

        private final int x;
        private final int z;

        Piece(int x, int z) {
            this.x = x;
            this.z = z;
        }

        @Override
        public StructureBoundingBox getHorizontalBounds() {
            return new StructureBoundingBox(this.x, 0, this.z, this.x, 255, this.z);
        }

        @Override
        public int resolveY(World world, Random random) {
            return Math.max(TerrainHeights.oceanFloor(world, this.x, this.z), 0);
        }

        /** Digs down to the first stone-like block and buries the chest there, patching any hole around it. */
        @Override
        public void place(World world, int y, StructureBoundingBox clip, Random random) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(this.x, y, this.z);
            if (!clip.isVecInside(pos)) return;
            while (pos.getY() > 0) {
                IBlockState state = world.getBlockState(pos);
                IBlockState below = world.getBlockState(pos.down());
                if (isTreasureFloor(below)) {
                    IBlockState fill = state.getMaterial() != Material.AIR && !isFluid(state) ? state : Blocks.SAND.getDefaultState();
                    for (EnumFacing facing : EnumFacing.values()) {
                        BlockPos side = pos.offset(facing);
                        IBlockState sideState = world.getBlockState(side);
                        if (sideState.getMaterial() != Material.AIR && !isFluid(sideState)) continue;
                        IBlockState under = world.getBlockState(side.down());
                        boolean hollowBelow = under.getMaterial() == Material.AIR || isFluid(under);
                        world.setBlockState(side, hollowBelow && facing != EnumFacing.UP ? below : fill, 3);
                    }
                    if (world.getBlockState(pos).getBlock() != Blocks.CHEST) {
                        world.setBlockState(pos, Blocks.CHEST.getDefaultState(), 2);
                        TileEntity chest = world.getTileEntity(pos);
                        if (chest instanceof TileEntityChest) ((TileEntityChest) chest).setLootTable(PortiumLootTables.BURIED_TREASURE, random.nextLong());
                    }
                    return;
                }
                pos.move(EnumFacing.DOWN);
            }
        }

        private static boolean isTreasureFloor(IBlockState state) {
            if (state == Blocks.SANDSTONE.getDefaultState()) return true;
            if (state.getBlock() != Blocks.STONE) return false;
            BlockStone.EnumType type = state.getValue(BlockStone.VARIANT);
            return type == BlockStone.EnumType.STONE || type == BlockStone.EnumType.GRANITE || type == BlockStone.EnumType.DIORITE
                    || type == BlockStone.EnumType.ANDESITE;
        }

        private static boolean isFluid(IBlockState state) {
            return state.getBlock() instanceof BlockLiquid && state.getValue(BlockLiquid.LEVEL) == 0;
        }
    }
}
