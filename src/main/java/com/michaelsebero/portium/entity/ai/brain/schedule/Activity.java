package com.michaelsebero.portium.entity.ai.brain.schedule;

/** 1.14's activities: the groups of tasks a brain switches between. */
public final class Activity {

    public static final Activity CORE = new Activity("core");
    public static final Activity IDLE = new Activity("idle");
    public static final Activity WORK = new Activity("work");
    public static final Activity PLAY = new Activity("play");
    public static final Activity REST = new Activity("rest");
    public static final Activity MEET = new Activity("meet");
    public static final Activity PANIC = new Activity("panic");
    public static final Activity RAID = new Activity("raid");
    public static final Activity PRE_RAID = new Activity("pre_raid");
    public static final Activity HIDE = new Activity("hide");
    public static final Activity FIGHT = new Activity("fight");
    public static final Activity CELEBRATE = new Activity("celebrate");
    public static final Activity ADMIRE_ITEM = new Activity("admire_item");
    public static final Activity AVOID = new Activity("avoid");
    public static final Activity RIDE = new Activity("ride");

    private final String id;

    private Activity(String id) {
        this.id = id;
    }

    public String getKey() {
        return this.id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
