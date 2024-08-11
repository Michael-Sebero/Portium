package com.michaelsebero.portium.modern.mc.util.math.shapes;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

/**
 * 1.13's {@code net.minecraft.util.math.shapes.VoxelShape}: a block's shape, in block coordinates (0 to 1 for a block
 * that stays in its cell). Like 1.13's, it's a grid cut at the coordinates its boxes start and end at, with each cell
 * filled or not, so shapes combine exactly (union, intersection, difference); 1.12's bounding and collision boxes are
 * its boxes ({@link #toBoundingBoxList}).
 */
public class VoxelShape {

    private static final double EPSILON = 1.0E-7D;

    /** The cuts along each axis, ascending; a shape has one cell fewer than cuts along each. */
    final double[] xs;
    final double[] ys;
    final double[] zs;
    /** Filled cells, at {@code (x * ys + y) * zs + z} in cell counts. */
    final BitSet cells;
    private List<AxisAlignedBB> boxes;

    VoxelShape(double[] xs, double[] ys, double[] zs, BitSet cells) {
        this.xs = xs;
        this.ys = ys;
        this.zs = zs;
        this.cells = cells;
    }

    int size(EnumFacing.Axis axis) {
        return cuts(axis).length - 1;
    }

    double[] cuts(EnumFacing.Axis axis) {
        return axis == EnumFacing.Axis.X ? this.xs : axis == EnumFacing.Axis.Y ? this.ys : this.zs;
    }

    boolean filled(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < this.xs.length - 1 && y < this.ys.length - 1 && z < this.zs.length - 1
                && this.cells.get(index(x, y, z));
    }

    int index(int x, int y, int z) {
        return (x * (this.ys.length - 1) + y) * (this.zs.length - 1) + z;
    }

    /** Whether a point (block coordinates) is inside a filled cell. */
    boolean contains(double x, double y, double z) {
        return filled(cell(this.xs, x), cell(this.ys, y), cell(this.zs, z));
    }

    /** The cell along one axis that holds a coordinate, or -1 outside the grid. */
    static int cell(double[] cuts, double value) {
        if (value < cuts[0] || value >= cuts[cuts.length - 1]) return -1;
        int found = Arrays.binarySearch(cuts, value);
        return found >= 0 ? found : -found - 2;
    }

    @Srg("func_197766_b")
    public boolean isEmpty() {
        return this.cells.isEmpty();
    }

    @Srg("func_197762_b")
    public double getStart(EnumFacing.Axis axis) {
        int[] range = filledRange(axis);
        return range == null ? Double.POSITIVE_INFINITY : cuts(axis)[range[0]];
    }

    @Srg("func_197758_c")
    public double getEnd(EnumFacing.Axis axis) {
        int[] range = filledRange(axis);
        return range == null ? Double.NEGATIVE_INFINITY : cuts(axis)[range[1] + 1];
    }

    /** The first and last cell along an axis that any filled cell is in, or null if empty. */
    private int[] filledRange(EnumFacing.Axis axis) {
        if (isEmpty()) return null;
        int low = Integer.MAX_VALUE;
        int high = -1;
        int ny = this.ys.length - 1;
        int nz = this.zs.length - 1;
        for (int i = this.cells.nextSetBit(0); i >= 0; i = this.cells.nextSetBit(i + 1)) {
            int z = i % nz;
            int y = (i / nz) % ny;
            int x = i / (nz * ny);
            int at = axis == EnumFacing.Axis.X ? x : axis == EnumFacing.Axis.Y ? y : z;
            low = Math.min(low, at);
            high = Math.max(high, at);
        }
        return new int[] {low, high};
    }

    /** The box around the whole shape; 1.13 throws for an empty shape. */
    @Srg("func_197752_a")
    public AxisAlignedBB getBoundingBox() {
        if (isEmpty()) throw new UnsupportedOperationException("No bounds for empty shape.");
        return new AxisAlignedBB(getStart(EnumFacing.Axis.X), getStart(EnumFacing.Axis.Y), getStart(EnumFacing.Axis.Z),
                getEnd(EnumFacing.Axis.X), getEnd(EnumFacing.Axis.Y), getEnd(EnumFacing.Axis.Z));
    }

    /** Each box of {@link #toBoundingBoxList}, by its corners. */
    @Srg("func_197755_b")
    public void forEachBox(VoxelShapes.ILineConsumer action) {
        for (AxisAlignedBB box : toBoundingBoxList()) action.consume(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    /** Each edge of each box, by its ends (edges boxes share come once per box). */
    @Srg("func_197754_a")
    public void forEachEdge(VoxelShapes.ILineConsumer action) {
        for (AxisAlignedBB box : toBoundingBoxList()) {
            double[] xs = {box.minX, box.maxX};
            double[] ys = {box.minY, box.maxY};
            double[] zs = {box.minZ, box.maxZ};
            for (double y : ys) for (double z : zs) action.consume(box.minX, y, z, box.maxX, y, z);
            for (double x : xs) for (double z : zs) action.consume(x, box.minY, z, x, box.maxY, z);
            for (double x : xs) for (double y : ys) action.consume(x, y, box.minZ, x, y, box.maxZ);
        }
    }

    /** The fewest boxes covering the filled cells, found greedily: runs along z, then rows along y, then slabs along x. */
    @Srg("func_197756_d")
    public List<AxisAlignedBB> toBoundingBoxList() {
        if (this.boxes != null) return this.boxes;
        int nx = this.xs.length - 1;
        int ny = this.ys.length - 1;
        int nz = this.zs.length - 1;
        BitSet left = (BitSet) this.cells.clone();
        List<AxisAlignedBB> found = new ArrayList<>();
        for (int start = left.nextSetBit(0); start >= 0; start = left.nextSetBit(start + 1)) {
            int z0 = start % nz;
            int y0 = (start / nz) % ny;
            int x0 = start / (nz * ny);
            int z1 = z0;
            while (z1 + 1 < nz && left.get(index(x0, y0, z1 + 1))) z1++;
            int y1 = y0;
            while (y1 + 1 < ny && run(left, x0, y1 + 1, z0, z1)) y1++;
            int x1 = x0;
            while (x1 + 1 < nx && slab(left, x1 + 1, y0, y1, z0, z1)) x1++;
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) left.clear(index(x, y, z0), index(x, y, z1) + 1);
            }
            found.add(new AxisAlignedBB(this.xs[x0], this.ys[y0], this.zs[z0], this.xs[x1 + 1], this.ys[y1 + 1], this.zs[z1 + 1]));
        }
        this.boxes = Collections.unmodifiableList(found);
        return this.boxes;
    }

    private boolean run(BitSet left, int x, int y, int z0, int z1) {
        for (int z = z0; z <= z1; z++) {
            if (!left.get(index(x, y, z))) return false;
        }
        return true;
    }

    private boolean slab(BitSet left, int x, int y0, int y1, int z0, int z1) {
        for (int y = y0; y <= y1; y++) {
            if (!run(left, x, y, z0, z1)) return false;
        }
        return true;
    }

    @Srg("func_197751_a")
    public VoxelShape withOffset(double x, double y, double z) {
        if (isEmpty()) return this;
        return new VoxelShape(shift(this.xs, x), shift(this.ys, y), shift(this.zs, z), this.cells);
    }

    private static double[] shift(double[] cuts, double by) {
        double[] moved = new double[cuts.length];
        for (int i = 0; i < cuts.length; i++) moved[i] = cuts[i] + by;
        return moved;
    }

    /** The same shape with no cut that leaves the cells on either side alike. */
    @Srg("func_197753_c")
    public VoxelShape simplify() {
        if (isEmpty()) return VoxelShapes.empty();
        VoxelShape shape = this;
        for (EnumFacing.Axis axis : EnumFacing.Axis.values()) shape = shape.merged(axis);
        return shape;
    }

    /** Drops the cuts along an axis between identical slices. */
    private VoxelShape merged(EnumFacing.Axis axis) {
        double[] cuts = cuts(axis);
        List<Integer> keep = new ArrayList<>();
        keep.add(0);
        for (int i = 1; i < cuts.length - 1; i++) {
            if (!sameSlices(axis, keep.get(keep.size() - 1), i)) keep.add(i);
        }
        if (keep.size() == cuts.length - 1) return this;
        double[] kept = new double[keep.size() + 1];
        for (int i = 0; i < keep.size(); i++) kept[i] = cuts[keep.get(i)];
        kept[keep.size()] = cuts[cuts.length - 1];
        double[] xs = axis == EnumFacing.Axis.X ? kept : this.xs;
        double[] ys = axis == EnumFacing.Axis.Y ? kept : this.ys;
        double[] zs = axis == EnumFacing.Axis.Z ? kept : this.zs;
        VoxelShape result = new VoxelShape(xs, ys, zs, new BitSet());
        for (int x = 0; x < xs.length - 1; x++) {
            for (int y = 0; y < ys.length - 1; y++) {
                for (int z = 0; z < zs.length - 1; z++) {
                    int sx = axis == EnumFacing.Axis.X ? keep.get(x) : x;
                    int sy = axis == EnumFacing.Axis.Y ? keep.get(y) : y;
                    int sz = axis == EnumFacing.Axis.Z ? keep.get(z) : z;
                    if (filled(sx, sy, sz)) result.cells.set(result.index(x, y, z));
                }
            }
        }
        return result;
    }

    private boolean sameSlices(EnumFacing.Axis axis, int first, int second) {
        int nx = this.xs.length - 1;
        int ny = this.ys.length - 1;
        int nz = this.zs.length - 1;
        int a = axis == EnumFacing.Axis.X ? ny : nx;
        int b = axis == EnumFacing.Axis.Z ? ny : nz;
        for (int i = 0; i < a; i++) {
            for (int j = 0; j < b; j++) {
                boolean one;
                boolean two;
                if (axis == EnumFacing.Axis.X) {
                    one = filled(first, i, j);
                    two = filled(second, i, j);
                } else if (axis == EnumFacing.Axis.Y) {
                    one = filled(i, first, j);
                    two = filled(i, second, j);
                } else {
                    one = filled(i, j, first);
                    two = filled(i, j, second);
                }
                if (one != two) return false;
            }
        }
        return true;
    }

    /** The lowest coordinate along an axis of the filled cells at the given point on the other two (in axis order). */
    @Srg("func_197764_a")
    public double min(EnumFacing.Axis axis, double first, double second) {
        int[] range = lineRange(axis, first, second);
        return range == null ? Double.POSITIVE_INFINITY : cuts(axis)[range[0]];
    }

    @Srg("func_197760_b")
    public double max(EnumFacing.Axis axis, double first, double second) {
        int[] range = lineRange(axis, first, second);
        return range == null ? Double.NEGATIVE_INFINITY : cuts(axis)[range[1] + 1];
    }

    private int[] lineRange(EnumFacing.Axis axis, double first, double second) {
        EnumFacing.Axis[] others = axis == EnumFacing.Axis.X ? new EnumFacing.Axis[] {EnumFacing.Axis.Y, EnumFacing.Axis.Z}
                : axis == EnumFacing.Axis.Y ? new EnumFacing.Axis[] {EnumFacing.Axis.X, EnumFacing.Axis.Z} : new EnumFacing.Axis[] {EnumFacing.Axis.X, EnumFacing.Axis.Y};
        int a = cell(cuts(others[0]), first);
        int b = cell(cuts(others[1]), second);
        if (a < 0 || b < 0) return null;
        int low = -1;
        int high = -1;
        for (int i = 0; i < size(axis); i++) {
            int x = axis == EnumFacing.Axis.X ? i : a;
            int y = axis == EnumFacing.Axis.Y ? i : axis == EnumFacing.Axis.X ? a : b;
            int z = axis == EnumFacing.Axis.Z ? i : b;
            if (filled(x, y, z)) {
                if (low < 0) low = i;
                high = i;
            }
        }
        return low < 0 ? null : new int[] {low, high};
    }

    /** The shape's slice nearest a face, stretched across the whole cell along that axis: what covers the face. */
    @Srg("func_212434_a")
    public VoxelShape project(EnumFacing side) {
        if (isEmpty() || this == VoxelShapes.fullCube()) return this;
        EnumFacing.Axis axis = side.getAxis();
        double at = side.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? 1.0D - EPSILON : EPSILON;
        int index = cell(cuts(axis), at);
        if (index < 0) return VoxelShapes.empty();
        return slice(axis, index);
    }

    /** One slice along an axis, spanning 0 to 1 along it. */
    VoxelShape slice(EnumFacing.Axis axis, int index) {
        double[] unit = {0.0D, 1.0D};
        double[] xs = axis == EnumFacing.Axis.X ? unit : this.xs;
        double[] ys = axis == EnumFacing.Axis.Y ? unit : this.ys;
        double[] zs = axis == EnumFacing.Axis.Z ? unit : this.zs;
        VoxelShape result = new VoxelShape(xs, ys, zs, new BitSet());
        for (int x = 0; x < xs.length - 1; x++) {
            for (int y = 0; y < ys.length - 1; y++) {
                for (int z = 0; z < zs.length - 1; z++) {
                    int sx = axis == EnumFacing.Axis.X ? index : x;
                    int sy = axis == EnumFacing.Axis.Y ? index : y;
                    int sz = axis == EnumFacing.Axis.Z ? index : z;
                    if (filled(sx, sy, sz)) result.cells.set(result.index(x, y, z));
                }
            }
        }
        return result;
    }

    /** Where a line from {@code start} to {@code end} first meets the shape placed at {@code pos}, or null. */
    @Srg("func_212433_a")
    public RayTraceResult rayTrace(Vec3d start, Vec3d end, BlockPos pos) {
        if (isEmpty()) return null;
        Vec3d direction = end.subtract(start);
        if (direction.lengthSquared() < EPSILON) return null;
        Vec3d inside = start.add(direction.scale(0.001D));
        if (contains(inside.x - pos.getX(), inside.y - pos.getY(), inside.z - pos.getZ())) {
            // 1.16 takes the result as a block ray trace result; 1.13 as 1.12's, which that is too.
            return new com.michaelsebero.portium.modern.mc.util.math.BlockRayTraceResult(inside,
                    EnumFacing.getFacingFromVector((float) direction.x, (float) direction.y, (float) direction.z).getOpposite(), pos, true);
        }
        RayTraceResult nearest = null;
        double distance = Double.MAX_VALUE;
        for (AxisAlignedBB box : toBoundingBoxList()) {
            RayTraceResult hit = box.offset(pos).calculateIntercept(start, end);
            if (hit == null) continue;
            double squared = hit.hitVec.squareDistanceTo(start);
            if (squared < distance) {
                distance = squared;
                nearest = new com.michaelsebero.portium.modern.mc.util.math.BlockRayTraceResult(hit.hitVec, hit.sideHit, pos, false);
            }
        }
        return nearest;
    }

    /** How far {@code box} may move along an axis, up to {@code offset}, before it meets the shape. */
    @Srg("func_212430_a")
    public double getAllowedOffset(EnumFacing.Axis axis, AxisAlignedBB box, double offset) {
        for (AxisAlignedBB part : toBoundingBoxList()) {
            if (axis == EnumFacing.Axis.X) {
                offset = part.calculateXOffset(box, offset);
            } else if (axis == EnumFacing.Axis.Y) {
                offset = part.calculateYOffset(box, offset);
            } else {
                offset = part.calculateZOffset(box, offset);
            }
        }
        return offset;
    }

    /** Whether the shape fills the whole block. */
    public boolean isFullCube() {
        if (isEmpty()) return false;
        List<AxisAlignedBB> boxes = simplify().toBoundingBoxList();
        if (boxes.size() != 1) return false;
        AxisAlignedBB box = boxes.get(0);
        return box.minX <= EPSILON && box.minY <= EPSILON && box.minZ <= EPSILON && box.maxX >= 1 - EPSILON && box.maxY >= 1 - EPSILON
                && box.maxZ >= 1 - EPSILON;
    }

    @Override
    public String toString() {
        return "VoxelShape[" + (isEmpty() ? "EMPTY" : getBoundingBox().toString()) + "]";
    }
}
