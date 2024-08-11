package com.michaelsebero.portium.modern.mc.util.math.shapes;

import com.google.common.math.DoubleMath;
import com.michaelsebero.portium.modern.Srg;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.BitSet;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.stream.Stream;

/** 1.13's {@code net.minecraft.util.math.shapes.VoxelShapes}: making and combining shapes. */
public final class VoxelShapes {

    private static final double EPSILON = 1.0E-7D;
    private static final VoxelShape EMPTY = new VoxelShape(new double[] {0.0D, 0.0D}, new double[] {0.0D, 0.0D}, new double[] {0.0D, 0.0D}, new BitSet());
    private static final VoxelShape FULL_CUBE = box(0, 0, 0, 1, 1, 1);

    @Srg("func_197880_a")
    public static VoxelShape empty() {
        return EMPTY;
    }

    @Srg("func_197868_b")
    public static VoxelShape fullCube() {
        return FULL_CUBE;
    }

    @Srg("func_197873_a")
    public static VoxelShape create(double x1, double y1, double z1, double x2, double y2, double z2) {
        return create(new AxisAlignedBB(x1, y1, z1, x2, y2, z2));
    }

    @Srg("func_197881_a")
    public static VoxelShape create(AxisAlignedBB box) {
        if (box.minX == 0 && box.minY == 0 && box.minZ == 0 && box.maxX == 1 && box.maxY == 1 && box.maxZ == 1) return FULL_CUBE;
        if (box.maxX - box.minX < EPSILON || box.maxY - box.minY < EPSILON || box.maxZ - box.minZ < EPSILON) return EMPTY;
        return box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    private static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
        BitSet cells = new BitSet();
        cells.set(0);
        return new VoxelShape(new double[] {x1, x2}, new double[] {y1, y2}, new double[] {z1, z2}, cells);
    }

    /** The union of two shapes. */
    @Srg("func_197872_a")
    public static VoxelShape or(VoxelShape first, VoxelShape second) {
        return combineAndSimplify(first, second, IBooleanFunction.OR);
    }

    /** 1.14's: the union of several shapes. */
    @Srg("func_216384_a")
    public static VoxelShape or(VoxelShape first, VoxelShape... others) {
        VoxelShape union = first;
        for (VoxelShape other : others) union = or(union, other);
        return union;
    }

    @Srg("func_197878_a")
    public static VoxelShape combineAndSimplify(VoxelShape first, VoxelShape second, IBooleanFunction function) {
        return combine(first, second, function).simplify();
    }

    /** The cells where {@code function} holds of the two shapes, over a grid cut wherever either is cut. */
    @Srg("func_197882_b")
    public static VoxelShape combine(VoxelShape first, VoxelShape second, IBooleanFunction function) {
        if (function.apply(false, false)) throw new IllegalArgumentException("Combining with a function true outside both shapes");
        if (first == second) return function.apply(true, true) ? first : EMPTY;
        boolean keepFirst = function.apply(true, false);
        boolean keepSecond = function.apply(false, true);
        if (first.isEmpty()) return keepSecond ? second : EMPTY;
        if (second.isEmpty()) return keepFirst ? first : EMPTY;
        double[] xs = merge(first.xs, second.xs);
        double[] ys = merge(first.ys, second.ys);
        double[] zs = merge(first.zs, second.zs);
        VoxelShape result = new VoxelShape(xs, ys, zs, new BitSet());
        for (int x = 0; x < xs.length - 1; x++) {
            double cx = (xs[x] + xs[x + 1]) / 2;
            for (int y = 0; y < ys.length - 1; y++) {
                double cy = (ys[y] + ys[y + 1]) / 2;
                for (int z = 0; z < zs.length - 1; z++) {
                    double cz = (zs[z] + zs[z + 1]) / 2;
                    if (function.apply(first.contains(cx, cy, cz), second.contains(cx, cy, cz))) result.cells.set(result.index(x, y, z));
                }
            }
        }
        return result.isEmpty() ? EMPTY : result;
    }

    /** Whether {@code function} holds of the two shapes anywhere. */
    @Srg("func_197879_c")
    public static boolean compare(VoxelShape first, VoxelShape second, IBooleanFunction function) {
        if (function.apply(false, false)) throw new IllegalArgumentException("Comparing with a function true outside both shapes");
        if (first == second) return function.apply(true, true);
        if (first.isEmpty()) return function.apply(false, true) && !second.isEmpty();
        if (second.isEmpty()) return function.apply(true, false);
        return !combine(first, second, function).isEmpty();
    }

    /** How far a box may move along an axis past these shapes, up to {@code offset}. */
    @Srg("func_212437_a")
    public static double getAllowedOffset(EnumFacing.Axis axis, AxisAlignedBB box, Stream<VoxelShape> shapes, double offset) {
        for (Iterator<VoxelShape> each = shapes.iterator(); each.hasNext(); offset = each.next().getAllowedOffset(axis, box, offset)) {
            if (Math.abs(offset) < EPSILON) return 0.0D;
        }
        return offset;
    }

    /** Whether a block's face toward {@code side} is hidden behind the neighbour's shape there. */
    @Srg("func_197875_a")
    public static boolean isCubeSideCovered(VoxelShape shape, VoxelShape adjacent, EnumFacing side) {
        if (shape == FULL_CUBE && adjacent == FULL_CUBE) return true;
        if (adjacent.isEmpty()) return false;
        EnumFacing.Axis axis = side.getAxis();
        boolean positive = side.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE;
        VoxelShape near = positive ? shape : adjacent;
        VoxelShape far = positive ? adjacent : shape;
        IBooleanFunction uncovered = positive ? IBooleanFunction.ONLY_FIRST : IBooleanFunction.ONLY_SECOND;
        return DoubleMath.fuzzyEquals(near.getEnd(axis), 1.0D, EPSILON) && DoubleMath.fuzzyEquals(far.getStart(axis), 0.0D, EPSILON)
                && !compare(near.slice(axis, near.size(axis) - 1), far.slice(axis, 0), uncovered);
    }

    /** Whether two neighbours' facing sides together fill the whole square between them. */
    @Srg("func_204642_b")
    public static boolean doAdjacentCubeSidesFillSquare(VoxelShape shape, VoxelShape adjacent, EnumFacing side) {
        if (shape == FULL_CUBE || adjacent == FULL_CUBE) return true;
        EnumFacing.Axis axis = side.getAxis();
        boolean positive = side.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE;
        VoxelShape near = positive ? shape : adjacent;
        VoxelShape far = positive ? adjacent : shape;
        if (!DoubleMath.fuzzyEquals(near.getEnd(axis), 1.0D, EPSILON)) near = EMPTY;
        if (!DoubleMath.fuzzyEquals(far.getStart(axis), 0.0D, EPSILON)) far = EMPTY;
        VoxelShape nearSide = near.isEmpty() ? EMPTY : near.slice(axis, near.size(axis) - 1);
        VoxelShape farSide = far.isEmpty() ? EMPTY : far.slice(axis, 0);
        return !compare(FULL_CUBE, combine(nearSide, farSide, IBooleanFunction.OR), IBooleanFunction.ONLY_FIRST);
    }

    /** The cuts of both grids, close ones taken as one. */
    private static double[] merge(double[] first, double[] second) {
        TreeSet<Double> cuts = new TreeSet<>();
        for (double cut : first) cuts.add(cut);
        for (double cut : second) cuts.add(cut);
        double[] merged = new double[cuts.size()];
        int count = 0;
        for (double cut : cuts) {
            if (count == 0 || cut - merged[count - 1] > EPSILON) merged[count++] = cut;
        }
        return count == merged.length ? merged : java.util.Arrays.copyOf(merged, count);
    }

    private VoxelShapes() {}

    /** What takes a shape's boxes or edges, by two corners or ends. */
    public interface ILineConsumer {
        void consume(double x1, double y1, double z1, double x2, double y2, double z2);
    }
}
