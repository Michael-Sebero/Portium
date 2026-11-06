package com.michaelsebero.portium.entity.villager;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 1.14's villager gossip: what a villager has heard about each player (and mob), by kind. A player's reputation is
 * the weighted sum; it changes trade prices and whether iron golems turn on the player. Gossip fades daily and passes
 * between villagers when they meet, a little weaker each time.
 */
public final class Gossips116 {

    public enum Type {
        MAJOR_NEGATIVE(-5, 100, 10, 10),
        MINOR_NEGATIVE(-1, 200, 20, 20),
        MINOR_POSITIVE(1, 200, 1, 5),
        MAJOR_POSITIVE(5, 100, 0, 100),
        TRADING(1, 25, 2, 20);

        final int weight;
        final int max;
        final int decayPerDay;
        final int decayPerTransfer;

        Type(int weight, int max, int decayPerDay, int decayPerTransfer) {
            this.weight = weight;
            this.max = max;
            this.decayPerDay = decayPerDay;
            this.decayPerTransfer = decayPerTransfer;
        }

        public String getName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        static Type byName(String name) {
            for (Type type : values()) {
                if (type.getName().equals(name)) return type;
            }
            return null;
        }
    }

    private final Map<UUID, EnumMap<Type, Integer>> gossips = new HashMap<>();

    /** Adds gossip of a kind about a target, up to that kind's limit. */
    public void add(UUID target, Type type, int value) {
        EnumMap<Type, Integer> about = this.gossips.computeIfAbsent(target, key -> new EnumMap<>(Type.class));
        int now = Math.min(type.max, about.getOrDefault(type, 0) + value);
        about.put(type, Math.max(0, now));
        // A major positive (curing) outweighs grudges of the same weight, as 1.14 merges them.
        if (type == Type.MAJOR_POSITIVE) about.remove(Type.MAJOR_NEGATIVE);
    }

    /** Takes gossip of a kind off a target (golems forgive, cures replace grudges). */
    public void remove(UUID target, Type type) {
        EnumMap<Type, Integer> about = this.gossips.get(target);
        if (about != null) {
            about.remove(type);
            if (about.isEmpty()) this.gossips.remove(target);
        }
    }

    /** The weighted sum of everything heard about a target. */
    public int getReputation(UUID target) {
        EnumMap<Type, Integer> about = this.gossips.get(target);
        if (about == null) return 0;
        int sum = 0;
        for (Map.Entry<Type, Integer> entry : about.entrySet()) sum += entry.getKey().weight * entry.getValue();
        return sum;
    }

    /** A day passed: every kind fades by its daily amount. */
    public void decay() {
        Iterator<Map.Entry<UUID, EnumMap<Type, Integer>>> targets = this.gossips.entrySet().iterator();
        while (targets.hasNext()) {
            EnumMap<Type, Integer> about = targets.next().getValue();
            about.replaceAll((type, value) -> value - type.decayPerDay);
            about.values().removeIf(value -> value <= 0);
            if (about.isEmpty()) targets.remove();
        }
    }

    /** Tells another villager some of what this one knows: up to ten pieces, each weaker by its kind's transfer loss. */
    public void transferTo(Gossips116 other, Random random, int count) {
        Object[] targets = this.gossips.keySet().toArray();
        for (int i = 0; i < count && targets.length > 0; i++) {
            UUID target = (UUID) targets[random.nextInt(targets.length)];
            EnumMap<Type, Integer> about = this.gossips.get(target);
            if (about == null || about.isEmpty()) continue;
            Type[] types = about.keySet().toArray(new Type[0]);
            Type type = types[random.nextInt(types.length)];
            // Major positive gossip (a cure) is personal, as 1.14 keeps it.
            if (type == Type.MAJOR_POSITIVE) continue;
            int value = about.get(type) - type.decayPerTransfer;
            if (value < 2) continue;
            EnumMap<Type, Integer> theirs = other.gossips.computeIfAbsent(target, key -> new EnumMap<>(Type.class));
            theirs.put(type, Math.max(theirs.getOrDefault(type, 0), value));
        }
    }

    /** As 1.14 saves it: a list of {Type, Target, Value}. */
    public NBTTagList write() {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<UUID, EnumMap<Type, Integer>> target : this.gossips.entrySet()) {
            for (Map.Entry<Type, Integer> entry : target.getValue().entrySet()) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setString("Type", entry.getKey().getName());
                tag.setUniqueId("Target", target.getKey());
                tag.setInteger("Value", entry.getValue());
                list.appendTag(tag);
            }
        }
        return list;
    }

    public void read(NBTTagList list) {
        this.gossips.clear();
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            Type type = Type.byName(tag.getString("Type"));
            if (type == null || !tag.hasUniqueId("Target")) continue;
            this.gossips.computeIfAbsent(tag.getUniqueId("Target"), key -> new EnumMap<>(Type.class)).put(type, tag.getInteger("Value"));
        }
    }
}
