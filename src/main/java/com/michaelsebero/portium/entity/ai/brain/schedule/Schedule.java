package com.michaelsebero.portium.entity.ai.brain.schedule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** 1.14's daily schedules: which activity a brain should be doing at a given time of day. */
public final class Schedule {

    public static final Schedule EMPTY = builder().add(0, Activity.IDLE).build();
    public static final Schedule SIMPLE = builder().add(5000, Activity.WORK).add(11000, Activity.REST).build();
    public static final Schedule VILLAGER_BABY = builder().add(10, Activity.IDLE).add(3000, Activity.PLAY)
            .add(6000, Activity.IDLE).add(10000, Activity.PLAY).add(12000, Activity.REST).build();
    public static final Schedule VILLAGER_DEFAULT = builder().add(10, Activity.IDLE).add(2000, Activity.WORK)
            .add(9000, Activity.MEET).add(11000, Activity.IDLE).add(12000, Activity.REST).build();

    private final Map<Activity, Duties> duties = new HashMap<>();

    public static Builder builder() {
        return new Builder(new Schedule());
    }

    /** The activity whose duty is most active at this time of day (0-23999). */
    public Activity getScheduledActivity(int dayTime) {
        return this.duties.entrySet().stream()
                .max(Comparator.comparingDouble(entry -> entry.getValue().update(dayTime)))
                .map(Map.Entry::getKey).orElse(Activity.IDLE);
    }

    public static final class Builder {
        private final Schedule schedule;
        private final List<Object[]> entries = new ArrayList<>();

        private Builder(Schedule schedule) {
            this.schedule = schedule;
        }

        public Builder add(int time, Activity activity) {
            this.entries.add(new Object[]{time, activity});
            return this;
        }

        public Schedule build() {
            for (Object[] entry : this.entries) this.schedule.duties.computeIfAbsent((Activity) entry[1], key -> new Duties());
            for (Object[] entry : this.entries) {
                int time = (Integer) entry[0];
                Activity activity = (Activity) entry[1];
                this.schedule.duties.forEach((other, duties) -> duties.add(time, other == activity ? 1.0F : 0.0F));
            }
            return this.schedule;
        }
    }

    /** 1.14's {@code ScheduleDuties}: an activity's weight from each start time on. */
    private static final class Duties {
        private final List<int[]> times = new ArrayList<>();
        private final List<Float> weights = new ArrayList<>();
        private int index;

        void add(int time, float weight) {
            TreeMap<Integer, Float> sorted = new TreeMap<>();
            for (int i = 0; i < this.times.size(); ++i) sorted.put(this.times.get(i)[0], this.weights.get(i));
            sorted.put(time, weight);
            this.times.clear();
            this.weights.clear();
            sorted.forEach((t, w) -> {
                this.times.add(new int[]{t});
                this.weights.add(w);
            });
            this.index = 0;
        }

        float update(int dayTime) {
            if (this.times.isEmpty()) return 0.0F;
            boolean beforeFirst = dayTime < this.times.get(this.index)[0];
            int start = beforeFirst ? 0 : this.index;
            float active = beforeFirst ? this.weights.get(this.weights.size() - 1) : this.weights.get(this.index);
            for (int i = start; i < this.times.size(); ++i) {
                if (this.times.get(i)[0] > dayTime) break;
                this.index = i;
                active = this.weights.get(i);
            }
            return active;
        }
    }
}
