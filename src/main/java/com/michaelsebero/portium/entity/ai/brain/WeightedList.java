package com.michaelsebero.portium.entity.ai.brain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/** 1.16's {@code WeightedList}: shuffled so that heavier entries tend to come first. */
public class WeightedList<U> {

    private final List<Entry<U>> entries = new ArrayList<>();
    private final Random random = new Random();

    public WeightedList<U> addWeighted(U value, int weight) {
        this.entries.add(new Entry<>(value, weight));
        return this;
    }

    public WeightedList<U> randomizeWithWeight() {
        return this.randomizeWithWeight(this.random);
    }

    public WeightedList<U> randomizeWithWeight(Random random) {
        this.entries.forEach(entry -> entry.chance = -Math.pow(random.nextFloat(), 1.0F / (float) entry.weight));
        this.entries.sort(Comparator.comparingDouble(entry -> entry.chance));
        return this;
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    public Stream<U> getValueStream() {
        return this.entries.stream().map(entry -> entry.value);
    }

    public U getRandomValue(Random random) {
        return this.randomizeWithWeight(random).getValueStream().findFirst().orElseThrow(RuntimeException::new);
    }

    private static final class Entry<T> {
        private final T value;
        private final int weight;
        private double chance;

        private Entry(T value, int weight) {
            this.value = value;
            this.weight = weight;
        }
    }
}
