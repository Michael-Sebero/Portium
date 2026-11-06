package com.michaelsebero.portium.entity.ai.brain;

/** A remembered value, forgotten after its time to live runs out (or never). */
public class Memory<T> {

    private final T value;
    private long timeToLive;

    public Memory(T value, long timeToLive) {
        this.value = value;
        this.timeToLive = timeToLive;
    }

    public static <T> Memory<T> create(T value) {
        return new Memory<>(value, Long.MAX_VALUE);
    }

    public static <T> Memory<T> create(T value, long timeToLive) {
        return new Memory<>(value, timeToLive);
    }

    public void tick() {
        if (this.isForgettable()) --this.timeToLive;
    }

    public T getValue() {
        return this.value;
    }

    public long getTimeToLive() {
        return this.timeToLive;
    }

    public boolean isForgotten() {
        return this.timeToLive <= 0L;
    }

    public boolean isForgettable() {
        return this.timeToLive != Long.MAX_VALUE;
    }

    @Override
    public String toString() {
        return this.value + (this.isForgettable() ? " (ttl: " + this.timeToLive + ")" : "");
    }
}
