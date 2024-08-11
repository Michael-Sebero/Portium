package com.michaelsebero.portium.world.gen.feature;

import com.google.common.collect.Lists;
import com.michaelsebero.portium.block.coral.BlockCoralFan;
import com.michaelsebero.portium.block.coral.BlockCoralPlant;
import com.michaelsebero.portium.block.water.WaterUtil;
import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Warm ocean coral (1.13 CoralTreeFeature, CoralClawFeature and CoralMushroomFeature), picked at random per call.
 * {@code pos} is the ocean floor.
 */
public class WorldGenCoralReef extends WorldGenerator {

    @Override
    public boolean generate(World world, Random rand, BlockPos pos) {
        IBlockState coral = CoralPicker.block(rand);
        switch (rand.nextInt(3)) {
            case 0: return this.tree(world, rand, pos, coral);
            case 1: return this.claw(world, rand, pos, coral);
            default: return this.mushroom(world, rand, pos, coral);
        }
    }

    /** Places one coral block with plants, a pickle or wall fans around it, if the spot is open water. */
    private boolean placePiece(World world, Random rand, BlockPos pos, IBlockState coral) {
        IBlockState here = world.getBlockState(pos);
        boolean replaceable = WaterUtil.isPlainWaterSource(here) || here.getBlock() instanceof BlockCoralPlant || here.getBlock() instanceof BlockCoralFan;
        BlockPos above = pos.up();
        if (!replaceable || !WaterUtil.isPlainWaterSource(world.getBlockState(above))) return false;
        world.setBlockState(pos, coral, 3);
        if (rand.nextFloat() < 0.25F) {
            world.setBlockState(above, CoralPicker.plantOrFan(rand), 2);
        } else if (rand.nextFloat() < 0.05F && PortiumBlocks.SEA_PICKLE != null) {
            world.setBlockState(above, PortiumBlocks.SEA_PICKLE.getDefaultState().withProperty(com.michaelsebero.portium.block.BlockSeaPickle.PICKLES, rand.nextInt(4) + 1), 2);
        }
        for (EnumFacing facing : EnumFacing.Plane.HORIZONTAL) {
            if (rand.nextFloat() < 0.2F) {
                BlockPos side = pos.offset(facing);
                if (WaterUtil.isPlainWaterSource(world.getBlockState(side))) world.setBlockState(side, CoralPicker.wallFan(rand, facing), 2);
            }
        }
        return true;
    }

    private boolean tree(World world, Random rand, BlockPos pos, IBlockState coral) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos);
        int trunk = rand.nextInt(3) + 1;
        for (int i = 0; i < trunk; i++) {
            if (!this.placePiece(world, rand, cursor, coral)) return true;
            cursor.move(EnumFacing.UP);
        }
        BlockPos top = cursor.toImmutable();
        List<EnumFacing> directions = Lists.newArrayList(EnumFacing.Plane.HORIZONTAL);
        Collections.shuffle(directions, rand);
        for (EnumFacing direction : directions.subList(0, rand.nextInt(3) + 2)) {
            cursor.setPos(top).move(direction);
            int length = rand.nextInt(5) + 2;
            int sinceTurn = 0;
            for (int i = 0; i < length && this.placePiece(world, rand, cursor, coral); i++) {
                sinceTurn++;
                cursor.move(EnumFacing.UP);
                if (i == 0 || (sinceTurn >= 2 && rand.nextFloat() < 0.25F)) {
                    cursor.move(direction);
                    sinceTurn = 0;
                }
            }
        }
        return true;
    }

    private boolean claw(World world, Random rand, BlockPos pos, IBlockState coral) {
        if (!this.placePiece(world, rand, pos, coral)) return false;
        EnumFacing main = EnumFacing.Plane.HORIZONTAL.random(rand);
        List<EnumFacing> arms = Lists.newArrayList(main, main.rotateY(), main.rotateYCCW());
        Collections.shuffle(arms, rand);
        for (EnumFacing arm : arms.subList(0, rand.nextInt(2) + 2)) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos);
            int base = rand.nextInt(2) + 1;
            cursor.move(arm);
            EnumFacing growth;
            int reach;
            if (arm == main) {
                growth = main;
                reach = rand.nextInt(3) + 2;
            } else {
                cursor.move(EnumFacing.UP);
                growth = rand.nextBoolean() ? arm : EnumFacing.UP;
                reach = rand.nextInt(3) + 3;
            }
            for (int i = 0; i < base && this.placePiece(world, rand, cursor, coral); i++) cursor.move(growth);
            cursor.move(growth.getOpposite());
            cursor.move(EnumFacing.UP);
            for (int i = 0; i < reach; i++) {
                cursor.move(main);
                if (!this.placePiece(world, rand, cursor, coral)) break;
                if (rand.nextFloat() < 0.25F) cursor.move(EnumFacing.UP);
            }
        }
        return true;
    }

    private boolean mushroom(World world, Random rand, BlockPos pos, IBlockState coral) {
        int height = rand.nextInt(3) + 3;
        int sizeX = rand.nextInt(3) + 3;
        int sizeZ = rand.nextInt(3) + 3;
        int sink = rand.nextInt(3) + 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = 0; x <= sizeX; x++) {
            for (int y = 0; y <= height; y++) {
                for (int z = 0; z <= sizeZ; z++) {
                    boolean xEdge = x == 0 || x == sizeX;
                    boolean yEdge = y == 0 || y == height;
                    boolean zEdge = z == 0 || z == sizeZ;
                    boolean onShell = xEdge || yEdge || zEdge;
                    boolean onCorner = (xEdge && yEdge) || (zEdge && yEdge) || (xEdge && zEdge);
                    if (onShell && !onCorner && rand.nextFloat() >= 0.1F) {
                        cursor.setPos(pos.getX() + x, pos.getY() + y - sink, pos.getZ() + z);
                        this.placePiece(world, rand, cursor, coral);
                    }
                }
            }
        }
        return true;
    }
}
