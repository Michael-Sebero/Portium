package com.michaelsebero.portium.mods116.host;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;

/**
 * 1.16's {@code IBlockReader.rayTraceBlocks(context)} on any block reader (1.12's block access has no such default):
 * walks the blocks the line crosses, asking each for the shape the context wants (and its fluid's), and answers the
 * nearest hit, or a miss at the end. Create's Ponder picks the block under the mouse with it.
 */
public final class RayTraceGlue {

    public static BlockRayTraceResult rayTraceBlocks(Object world, RayTraceContext context) {
        IBlockReader reader = (IBlockReader) world;
        Vector3d start = context.func_222253_b();
        Vector3d end = context.func_222250_a();
        double dx = end.field_72450_a - start.field_72450_a;
        double dy = end.field_72448_b - start.field_72448_b;
        double dz = end.field_72449_c - start.field_72449_c;
        if (dx == 0 && dy == 0 && dz == 0) return miss(start, end);
        // Nudged a little inside the line, as 1.16 does, so a start on a block's face counts the block it looks into.
        double x0 = MathHelper.func_219803_d(-1.0E-7D, start.field_72450_a, end.field_72450_a);
        double y0 = MathHelper.func_219803_d(-1.0E-7D, start.field_72448_b, end.field_72448_b);
        double z0 = MathHelper.func_219803_d(-1.0E-7D, start.field_72449_c, end.field_72449_c);
        double x1 = MathHelper.func_219803_d(-1.0E-7D, end.field_72450_a, start.field_72450_a);
        double y1 = MathHelper.func_219803_d(-1.0E-7D, end.field_72448_b, start.field_72448_b);
        double z1 = MathHelper.func_219803_d(-1.0E-7D, end.field_72449_c, start.field_72449_c);
        int x = MathHelper.func_76128_c(x0), y = MathHelper.func_76128_c(y0), z = MathHelper.func_76128_c(z0);
        BlockPos.Mutable pos = new BlockPos.Mutable(x, y, z);
        BlockRayTraceResult hit = hitAt(reader, context, start, end, pos);
        if (hit != null) return hit;
        double lx = x1 - x0, ly = y1 - y0, lz = z1 - z0;
        int sx = (int) Math.signum(lx), sy = (int) Math.signum(ly), sz = (int) Math.signum(lz);
        double tx = sx == 0 ? Double.MAX_VALUE : sx / lx, ty = sy == 0 ? Double.MAX_VALUE : sy / ly, tz = sz == 0 ? Double.MAX_VALUE : sz / lz;
        double nx = tx * (sx > 0 ? 1.0D - frac(x0) : frac(x0));
        double ny = ty * (sy > 0 ? 1.0D - frac(y0) : frac(y0));
        double nz = tz * (sz > 0 ? 1.0D - frac(z0) : frac(z0));
        while (nx <= 1.0D || ny <= 1.0D || nz <= 1.0D) {
            if (nx < ny) {
                if (nx < nz) {
                    x += sx;
                    nx += tx;
                } else {
                    z += sz;
                    nz += tz;
                }
            } else if (ny < nz) {
                y += sy;
                ny += ty;
            } else {
                z += sz;
                nz += tz;
            }
            hit = hitAt(reader, context, start, end, pos.func_181079_c(x, y, z));
            if (hit != null) return hit;
        }
        return miss(start, end);
    }

    private static BlockRayTraceResult hitAt(IBlockReader reader, RayTraceContext context, Vector3d start, Vector3d end, BlockPos.Mutable mutable) {
        BlockPos pos = mutable.func_185334_h();
        BlockState state = reader.func_180495_p(pos);
        FluidState fluid = reader.func_204610_c(pos);
        VoxelShape blockShape = context.func_222251_a(state, reader, pos);
        BlockRayTraceResult block = blockShape.func_212433_a(start, end, pos);
        VoxelShape fluidShape = context.func_222252_a(fluid, reader, pos);
        BlockRayTraceResult liquid = fluidShape.func_212433_a(start, end, pos);
        if (block == null) return liquid;
        if (liquid == null) return block;
        return start.func_72436_e(block.func_216347_e()) <= start.func_72436_e(liquid.func_216347_e()) ? block : liquid;
    }

    private static BlockRayTraceResult miss(Vector3d start, Vector3d end) {
        Vector3d along = start.func_178788_d(end);
        return BlockRayTraceResult.func_216352_a(end, Direction.func_210769_a(along.field_72450_a, along.field_72448_b, along.field_72449_c),
                new BlockPos(end));
    }

    private static double frac(double value) {
        return value - MathHelper.func_76128_c(value);
    }

    private RayTraceGlue() {}
}
