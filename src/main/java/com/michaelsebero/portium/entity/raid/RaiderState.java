package com.michaelsebero.portium.entity.raid;

import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

/** A raider's patrol and raid state, kept under 1.16's NBT names. */
public final class RaiderState {

    @Nullable
    private BlockPos patrolTarget;
    private boolean patrolLeader;
    private boolean patrolling;
    private int wave;
    private boolean canJoinRaid;
    private int joinDelay;
    /** The raid it belongs to, by id; -1 for none. */
    private int raidId = -1;
    /** Spawned by the world's own spawning (1.14's NATURAL spawn reason): such witches never join raids. */
    private boolean naturalSpawn;

    @Nullable
    public BlockPos getPatrolTarget() {
        return this.patrolTarget;
    }

    public boolean hasPatrolTarget() {
        return this.patrolTarget != null;
    }

    public void setPatrolTarget(BlockPos target) {
        this.patrolTarget = target;
        this.patrolling = true;
    }

    /** A new target up to 500 blocks away each way. */
    public void resetPatrolTarget(EntityLiving entity) {
        this.patrolTarget = new BlockPos(entity).add(-500 + entity.getRNG().nextInt(1000), 0, -500 + entity.getRNG().nextInt(1000));
        this.patrolling = true;
    }

    public boolean isLeader() {
        return this.patrolLeader;
    }

    public void setLeader(boolean leader) {
        this.patrolLeader = leader;
        this.patrolling = true;
    }

    /** Only for spawning: whether it leads, without starting a patrol. */
    void setLeaderOnly(boolean leader) {
        this.patrolLeader = leader;
    }

    public boolean isPatrolling() {
        return this.patrolling;
    }

    public void setPatrolling(boolean patrolling) {
        this.patrolling = patrolling;
    }

    public int getWave() {
        return this.wave;
    }

    public void setWave(int wave) {
        this.wave = wave;
    }

    public boolean canJoinRaid() {
        return this.canJoinRaid;
    }

    public void setCanJoinRaid(boolean canJoinRaid) {
        this.canJoinRaid = canJoinRaid;
    }

    public int getJoinDelay() {
        return this.joinDelay;
    }

    public void setJoinDelay(int joinDelay) {
        this.joinDelay = joinDelay;
    }

    public int getRaidId() {
        return this.raidId;
    }

    public void setRaidId(int raidId) {
        this.raidId = raidId;
    }

    public boolean isNaturalSpawn() {
        return this.naturalSpawn;
    }

    public void setNaturalSpawn(boolean naturalSpawn) {
        this.naturalSpawn = naturalSpawn;
    }

    public void write(NBTTagCompound tag) {
        if (this.patrolTarget != null) tag.setTag("PatrolTarget", NBTUtil.createPosTag(this.patrolTarget));
        tag.setBoolean("PatrolLeader", this.patrolLeader);
        tag.setBoolean("Patrolling", this.patrolling);
        tag.setInteger("Wave", this.wave);
        tag.setBoolean("CanJoinRaid", this.canJoinRaid);
        if (this.raidId >= 0) tag.setInteger("RaidId", this.raidId);
    }

    public void read(NBTTagCompound tag) {
        if (tag.hasKey("PatrolTarget", 10)) this.patrolTarget = NBTUtil.getPosFromTag(tag.getCompoundTag("PatrolTarget"));
        this.patrolLeader = tag.getBoolean("PatrolLeader");
        this.patrolling = tag.getBoolean("Patrolling");
        this.wave = tag.getInteger("Wave");
        this.canJoinRaid = tag.getBoolean("CanJoinRaid");
        this.raidId = tag.hasKey("RaidId", 3) ? tag.getInteger("RaidId") : -1;
    }
}
