package com.michaelsebero.portium.entity.villager;

import net.minecraft.nbt.NBTTagCompound;

/** 1.14's {@code VillagerData}: a villager's biome type, profession and level (1 novice to 5 master). Immutable. */
public final class VillagerData116 {

    private final VillagerType116 type;
    private final VillagerProfession116 profession;
    private final int level;

    public VillagerData116(VillagerType116 type, VillagerProfession116 profession, int level) {
        this.type = type;
        this.profession = profession;
        this.level = Math.max(1, Math.min(5, level));
    }

    public VillagerType116 getType() {
        return this.type;
    }

    public VillagerProfession116 getProfession() {
        return this.profession;
    }

    public int getLevel() {
        return this.level;
    }

    public VillagerData116 withType(VillagerType116 type) {
        return new VillagerData116(type, this.profession, this.level);
    }

    public VillagerData116 withProfession(VillagerProfession116 profession) {
        return new VillagerData116(this.type, profession, this.level);
    }

    public VillagerData116 withLevel(int level) {
        return new VillagerData116(this.type, this.profession, level);
    }

    /** As 1.14 saves it: {@code {type:"minecraft:plains", profession:"minecraft:farmer", level:1}}. */
    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("type", "minecraft:" + this.type.getName());
        tag.setString("profession", "minecraft:" + this.profession.getName());
        tag.setInteger("level", this.level);
        return tag;
    }

    public static VillagerData116 read(NBTTagCompound tag) {
        return new VillagerData116(VillagerType116.byName(tag.getString("type")), VillagerProfession116.byName(tag.getString("profession")),
                tag.hasKey("level") ? tag.getInteger("level") : 1);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof VillagerData116)) return false;
        VillagerData116 data = (VillagerData116) other;
        return data.type == this.type && data.profession == this.profession && data.level == this.level;
    }

    @Override
    public int hashCode() {
        return (this.type.ordinal() * 31 + this.profession.ordinal()) * 31 + this.level;
    }
}
