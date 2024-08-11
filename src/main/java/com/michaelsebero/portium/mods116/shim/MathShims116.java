package com.michaelsebero.portium.mods116.shim;

import com.michaelsebero.portium.modern.mc.util.math.BlockRayTraceResult;
import com.michaelsebero.portium.modern.mc.util.math.EntityRayTraceResult;

/** 1.16's ways of packing positions and reporting ray traces, which 1.12 lacks or has elsewhere. */
public final class MathShims116 {

    /** A chunk position's x, from 1.16's packed {@code long} (x in the low half). */
    public static int chunkX(long packed) {
        return (int) (packed & 4294967295L);
    }

    /** A chunk position's z, from the high half. */
    public static int chunkZ(long packed) {
        return (int) (packed >>> 32 & 4294967295L);
    }

    /** 1.13's {@code axis.getCoordinate(x, y, z)}: the coordinate along the axis. */
    public static int coordinate(net.minecraft.util.EnumFacing.Axis axis, int x, int y, int z) {
        return axis == net.minecraft.util.EnumFacing.Axis.X ? x : axis == net.minecraft.util.EnumFacing.Axis.Y ? y : z;
    }

    public static double coordinate(net.minecraft.util.EnumFacing.Axis axis, double x, double y, double z) {
        return axis == net.minecraft.util.EnumFacing.Axis.X ? x : axis == net.minecraft.util.EnumFacing.Axis.Y ? y : z;
    }

    // Ray trace results: 1.16 has a class for each kind; 1.12 one class with a kind field.

    /** 1.16's {@code getType} of any ray trace result. */
    public static net.minecraft.util.math.RayTraceResult.Type getType(net.minecraft.util.math.RayTraceResult result) {
        return result.typeOfHit;
    }

    /** 1.16's {@code getHitVec} of any ray trace result. */
    public static net.minecraft.util.math.Vec3d getHitVec(net.minecraft.util.math.RayTraceResult result) {
        return result.hitVec;
    }

    /**
     * A cast to 1.16's block ray trace result, which 1.12's results of blocks (and misses, which 1.16 reports as block
     * results) pass as a copy; an entity's fails as the cast would.
     */
    public static BlockRayTraceResult asBlockHit(Object result) {
        if (result == null || result instanceof BlockRayTraceResult) return (BlockRayTraceResult) result;
        net.minecraft.util.math.RayTraceResult hit = (net.minecraft.util.math.RayTraceResult) result;
        if (hit.typeOfHit == net.minecraft.util.math.RayTraceResult.Type.ENTITY) throw new ClassCastException("An entity's ray trace result isn't a block's");
        return BlockRayTraceResult.of(hit);
    }

    /** An {@code instanceof} 1.16's block ray trace result. */
    public static boolean isBlockHit(Object result) {
        return result instanceof BlockRayTraceResult || result instanceof net.minecraft.util.math.RayTraceResult
                && ((net.minecraft.util.math.RayTraceResult) result).typeOfHit != net.minecraft.util.math.RayTraceResult.Type.ENTITY;
    }

    /** A cast to 1.16's entity ray trace result. */
    public static EntityRayTraceResult asEntityHit(Object result) {
        if (result == null || result instanceof EntityRayTraceResult) return (EntityRayTraceResult) result;
        net.minecraft.util.math.RayTraceResult hit = (net.minecraft.util.math.RayTraceResult) result;
        if (hit.typeOfHit != net.minecraft.util.math.RayTraceResult.Type.ENTITY) throw new ClassCastException("A block's ray trace result isn't an entity's");
        return new EntityRayTraceResult(hit.entityHit, hit.hitVec);
    }

    /** An {@code instanceof} 1.16's entity ray trace result. */
    public static boolean isEntityHit(Object result) {
        return result instanceof EntityRayTraceResult || result instanceof net.minecraft.util.math.RayTraceResult
                && ((net.minecraft.util.math.RayTraceResult) result).typeOfHit == net.minecraft.util.math.RayTraceResult.Type.ENTITY;
    }

    private MathShims116() {}
}
