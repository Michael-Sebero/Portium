package com.michaelsebero.portium.entity.ai.brain.memory;

import net.minecraft.util.math.BlockPos;

import java.util.Objects;

/** A block position in a dimension (1.14's {@code GlobalPos}, by 1.12 dimension id). */
public final class GlobalPos {

    private final int dimension;
    private final BlockPos pos;

    private GlobalPos(int dimension, BlockPos pos) {
        this.dimension = dimension;
        this.pos = pos.toImmutable();
    }

    public static GlobalPos of(int dimension, BlockPos pos) {
        return new GlobalPos(dimension, pos);
    }

    public int getDimension() {
        return this.dimension;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GlobalPos)) return false;
        GlobalPos that = (GlobalPos) other;
        return this.dimension == that.dimension && this.pos.equals(that.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.dimension, this.pos);
    }

    @Override
    public String toString() {
        return this.dimension + " " + this.pos;
    }
}
