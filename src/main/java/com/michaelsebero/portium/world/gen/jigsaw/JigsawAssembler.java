package com.michaelsebero.portium.world.gen.jigsaw;

import com.michaelsebero.portium.world.gen.jigsaw.JigsawPools.Element;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawPools.Empty;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawPools.Feature;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawPools.ListElement;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawPools.Pool;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawPools.Projection;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawPools.Single;
import com.michaelsebero.portium.world.gen.jigsaw.JigsawTemplate.Jigsaw;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * 1.16's {@code JigsawManager}: lays out a jigsaw structure from a start pool. Each placed piece offers its jigsaws;
 * for each, candidates from the jigsaw's pool (then its fallback) are tried in random order, in random rotations,
 * until one has a facing jigsaw it can attach by and fits: inside the structure's reach and clear of other pieces
 * (or, inside its parent's box, clear of its siblings). Rigid pieces hang off rigid parents by their jigsaws; anything
 * joined to terrain-matching streets sits at the ground's height there.
 */
public final class JigsawAssembler {

    private static final Rotation[] ROTATIONS = Rotation.values();
    private static final boolean DEBUG = Boolean.getBoolean("portium.dev.jigsaw");

    /** A piece placed in a layout. */
    public static final class Placed {
        public final Element element;
        public final BlockPos origin;
        public final Rotation rotation;
        public final StructureBoundingBox box;
        public final int depth;
        @Nullable
        final Placed parent;
        final List<Placed> children = new ArrayList<>();

        Placed(Element element, BlockPos origin, Rotation rotation, StructureBoundingBox box, int depth, @Nullable Placed parent) {
            this.element = element;
            this.origin = origin;
            this.rotation = rotation;
            this.box = box;
            this.depth = depth;
            this.parent = parent;
        }
    }

    private final JigsawData data;
    private final HeightSampler heights;
    private final Random random;
    private final int maxDepth;
    private final StructureBoundingBox reach;
    private final boolean expansionHack;
    private final java.util.Map<String, Integer> poolHeights = new java.util.HashMap<>();
    private final List<Placed> placed = new ArrayList<>();
    private final Deque<Placed> queue = new ArrayDeque<>();

    private JigsawAssembler(JigsawData data, HeightSampler heights, Random random, int maxDepth, StructureBoundingBox reach, boolean expansionHack) {
        this.expansionHack = expansionHack;
        this.data = data;
        this.heights = heights;
        this.random = random;
        this.maxDepth = maxDepth;
        this.reach = reach;
    }

    /**
     * Lays out a structure starting from {@code startPool} at a block position: at the ground's height there (less
     * one, as 1.16 sinks start pieces) when {@code onSurface}, else at {@code start}'s height.
     */
    public static List<Placed> assemble(JigsawData data, HeightSampler heights, String startPool, BlockPos start, int maxDepth, boolean onSurface,
                                        boolean expansionHack, Random random) {
        Pool pool = data.pools().getPool(startPool);
        if (pool == null || pool.elements.isEmpty()) return Collections.emptyList();
        Element element = pool.elements.get(random.nextInt(pool.elements.size()));
        if (element instanceof Empty) return Collections.emptyList();
        Rotation rotation = ROTATIONS[random.nextInt(ROTATIONS.length)];
        StructureBoundingBox box = data.box(element, start, rotation);
        if (box == null) return Collections.emptyList();
        int centerX = (box.maxX + box.minX) / 2;
        int centerZ = (box.maxZ + box.minZ) / 2;
        int y = onSurface ? start.getY() + heights.surface(centerX, centerZ) : start.getY();
        int lift = y - (box.minY + 1);
        BlockPos origin = start.up(lift);
        box = data.box(element, origin, rotation);
        StructureBoundingBox reach = new StructureBoundingBox(centerX - 80, y - 80, centerZ - 80, centerX + 80, y + 80, centerZ + 80);
        JigsawAssembler assembler = new JigsawAssembler(data, heights, random, maxDepth, reach, expansionHack);
        Placed root = new Placed(element, origin, rotation, box, 0, null);
        assembler.placed.add(root);
        if (maxDepth > 0) {
            assembler.queue.add(root);
            while (!assembler.queue.isEmpty()) assembler.tryChildren(assembler.queue.removeFirst());
        }
        return assembler.placed;
    }

    private void tryChildren(Placed piece) {
        boolean pieceRigid = piece.element.projection == Projection.RIGID;
        int pieceMinY = piece.box.minY;
        for (Jigsaw jigsaw : this.data.jigsaws(piece.element, piece.rotation, this.random)) {
            EnumFacing facing = jigsaw.front;
            BlockPos jigsawPos = jigsaw.pos.add(piece.origin);
            BlockPos target = jigsawPos.offset(facing);
            int jigsawLift = jigsawPos.getY() - pieceMinY;
            Pool pool = this.data.pools().getPool(jigsaw.pool);
            if (pool == null || pool.elements.isEmpty() && !"empty".equals(jigsaw.pool)) continue;
            Pool fallback = pool == null ? null : this.data.pools().getPool(pool.fallback);
            boolean inside = piece.box.isVecInside(target);
            List<Element> candidates = new ArrayList<>();
            if (pool != null && piece.depth != this.maxDepth) candidates.addAll(shuffled(pool.elements));
            if (fallback != null) candidates.addAll(shuffled(fallback.elements));
            int groundHeight = -1;
            candidateLoop:
            for (Element candidate : candidates) {
                if (candidate instanceof Empty) break;
                boolean candidateRigid = candidate.projection == Projection.RIGID;
                for (Rotation rotation : shuffled(Arrays.asList(ROTATIONS))) {
                    for (Jigsaw other : this.data.jigsaws(candidate, rotation, this.random)) {
                        if (!jigsaw.canAttach(other)) continue;
                        BlockPos offset = target.subtract(other.pos);
                        StructureBoundingBox candidateBox = this.data.box(candidate, offset, rotation);
                        if (candidateBox == null) continue;
                        int otherLift = other.pos.getY();
                        int y;
                        if (pieceRigid && candidateRigid) {
                            y = pieceMinY + jigsawLift - otherLift + facing.getYOffset();
                        } else {
                            if (groundHeight == -1) groundHeight = this.heights.surface(jigsawPos.getX(), jigsawPos.getZ());
                            y = groundHeight - otherLift;
                        }
                        // The candidate's bottom (its origin's height): its jigsaw meets the parent's, or the ground.
                        BlockPos origin = new BlockPos(offset.getX(), y, offset.getZ());
                        StructureBoundingBox placedBox = this.data.box(candidate, origin, rotation);
                        this.expand(candidate, rotation, origin, placedBox);
                        if (!this.fits(placedBox, piece, inside)) {
                            if (DEBUG && jigsaw.target.contains("building_entrance")) {
                                com.michaelsebero.portium.Portium.LOGGER.info("  no fit: {} for {} at {} (inside {}) box {} parent {}", candidate, jigsaw.target, target,
                                        inside, placedBox, piece.box);
                            }
                            continue;
                        }
                        Placed child = new Placed(candidate, origin, rotation, placedBox, piece.depth + 1, piece);
                        piece.children.add(child);
                        this.placed.add(child);
                        if (piece.depth + 1 <= this.maxDepth) this.queue.addLast(child);
                        break candidateLoop;
                    }
                }
            }
        }
    }

    /**
     * 1.16's "expansion hack" (villages and outposts): a piece no more than 16 blocks tall is made tall enough for the
     * tallest piece that could hang off its jigsaws pointing into itself, so streets have room for their houses.
     */
    private void expand(Element element, Rotation rotation, BlockPos origin, StructureBoundingBox box) {
        if (!this.expansionHack || box.getYSize() > 16) return;
        int tallest = 0;
        for (Jigsaw jigsaw : this.data.jigsaws(element, rotation, this.random)) {
            if (!box.isVecInside(jigsaw.pos.add(origin).offset(jigsaw.front))) continue;
            Pool pool = this.data.pools().getPool(jigsaw.pool);
            if (pool == null) continue;
            tallest = Math.max(tallest, Math.max(this.poolHeight(pool), this.poolHeight(this.data.pools().getPool(pool.fallback))));
        }
        if (tallest > 0) box.maxY = box.minY + Math.max(tallest + 1, box.getYSize()) - 1;
    }

    /** The tallest piece of a pool (1.16's {@code getMaxSize}). */
    private int poolHeight(@Nullable Pool pool) {
        if (pool == null) return 0;
        return this.poolHeights.computeIfAbsent(pool.name, key -> {
            int tallest = 0;
            for (Element element : new java.util.LinkedHashSet<>(pool.elements)) {
                StructureBoundingBox box = this.data.box(element, BlockPos.ORIGIN, Rotation.NONE);
                if (box != null) tallest = Math.max(tallest, box.getYSize());
            }
            return tallest;
        });
    }

    /** Within reach and clear of the pieces it could collide with (its siblings, if it's inside its parent's box). */
    private boolean fits(StructureBoundingBox box, Placed parent, boolean inside) {
        if (inside) {
            if (!contains(parent.box, box)) return false;
            for (Placed sibling : parent.children) {
                if (overlaps(sibling.box, box)) return false;
            }
            return true;
        }
        if (!contains(this.reach, box)) return false;
        for (Placed other : this.placed) {
            if (overlaps(other.box, box)) return false;
        }
        return true;
    }

    /** Overlap of the boxes shrunk by a quarter block, as 1.16's voxel shapes test them: touching faces don't count. */
    private static boolean overlaps(StructureBoundingBox a, StructureBoundingBox b) {
        return a.maxX >= b.minX && a.minX <= b.maxX && a.maxZ >= b.minZ && a.minZ <= b.maxZ && a.maxY >= b.minY && a.minY <= b.maxY;
    }

    private static boolean contains(StructureBoundingBox outer, StructureBoundingBox inner) {
        return inner.minX >= outer.minX && inner.maxX <= outer.maxX && inner.minY >= outer.minY && inner.maxY <= outer.maxY && inner.minZ >= outer.minZ
                && inner.maxZ <= outer.maxZ;
    }

    private <T> List<T> shuffled(List<T> list) {
        List<T> copy = new ArrayList<>(list);
        Collections.shuffle(copy, this.random);
        return copy;
    }

    /** Whether an element places blocks of its own (the empty one doesn't). */
    static boolean isReal(Element element) {
        return element instanceof Single || element instanceof ListElement || element instanceof Feature;
    }
}
