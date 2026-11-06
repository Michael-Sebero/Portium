package com.michaelsebero.portium.entity.ai.brain;

import java.util.Random;

/** 1.16's {@code RangedInteger}: an inclusive range to pick from. */
public final class RangedInteger {

    private final int min;
    private final int max;

    public RangedInteger(int min, int max) {
        if (max < min) throw new IllegalArgumentException("max must be >= min");
        this.min = min;
        this.max = max;
    }

    public static RangedInteger of(int min, int max) {
        return new RangedInteger(min, max);
    }

    /** 1.16's {@code TickRangeConverter.convertRange}: seconds to ticks. */
    public static RangedInteger seconds(int min, int max) {
        return new RangedInteger(min * 20, max * 20);
    }

    public int getMinInclusive() {
        return this.min;
    }

    public int getMax() {
        return this.max;
    }

    public int getRandomWithinRange(Random random) {
        return this.min == this.max ? this.min : random.nextInt(this.max - this.min + 1) + this.min;
    }

    @Override
    public String toString() {
        return "IntRange[" + this.min + "-" + this.max + "]";
    }
}
