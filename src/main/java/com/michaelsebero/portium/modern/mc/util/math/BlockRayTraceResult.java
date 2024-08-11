package com.michaelsebero.portium.modern.mc.util.math;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/** 1.16's {@code net.minecraft.util.math.BlockRayTraceResult}: a block hit, as 1.12's ray trace result of a block. */
public class BlockRayTraceResult extends RayTraceResult {

    @Srg("field_216358_e")
    private final boolean inside;

    public BlockRayTraceResult(Vec3d hit, EnumFacing face, BlockPos pos, boolean inside) {
        this(false, hit, face, pos, inside);
    }

    public BlockRayTraceResult(boolean miss, Vec3d hit, EnumFacing face, BlockPos pos, boolean inside) {
        super(miss ? Type.MISS : Type.BLOCK, hit, face, pos);
        this.inside = inside;
    }

    /** 1.12's result of a block or a miss, as 1.16's. */
    public static BlockRayTraceResult of(RayTraceResult hit) {
        Vec3d at = hit.hitVec == null ? Vec3d.ZERO : hit.hitVec;
        BlockPos pos = hit.getBlockPos() == null ? new BlockPos(at) : hit.getBlockPos();
        EnumFacing face = hit.sideHit == null ? EnumFacing.UP : hit.sideHit;
        return new BlockRayTraceResult(hit.typeOfHit == Type.MISS, at, face, pos, false);
    }

    @Srg("func_216352_a")
    public static BlockRayTraceResult createMiss(Vec3d hit, EnumFacing face, BlockPos pos) {
        return new BlockRayTraceResult(true, hit, face, pos, false);
    }

    @Srg("func_216351_a")
    public BlockRayTraceResult withFace(EnumFacing face) {
        return new BlockRayTraceResult(this.typeOfHit == Type.MISS, this.hitVec, face, getBlockPos(), this.inside);
    }

    @Srg("func_237485_a_")
    public BlockRayTraceResult withPosition(BlockPos pos) {
        return new BlockRayTraceResult(this.typeOfHit == Type.MISS, this.hitVec, this.sideHit, pos, this.inside);
    }

    @Srg("func_216350_a")
    public BlockPos getPos() {
        return getBlockPos();
    }

    @Srg("func_216354_b")
    public EnumFacing getFace() {
        return this.sideHit;
    }

    @Srg("func_216346_c")
    public Type getType() {
        return this.typeOfHit;
    }

    @Srg("func_216353_d")
    public boolean isInside() {
        return this.inside;
    }

    @Srg("func_216347_e")
    public Vec3d getHitVec() {
        return this.hitVec;
    }
}
