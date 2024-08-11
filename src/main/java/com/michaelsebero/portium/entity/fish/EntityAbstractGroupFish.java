package com.michaelsebero.portium.entity.fish;

import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Fish that swim in schools (1.13): one fish leads and the others follow it around, up to a maximum group size.
 * Fish spawned together start in the same school.
 */
public abstract class EntityAbstractGroupFish extends EntityAbstractFish {

    @Nullable
    private EntityAbstractGroupFish leader;
    private int groupSize = 1;

    protected EntityAbstractGroupFish(World world) {
        super(world);
    }

    @Override
    protected void initEntityAI() {
        super.initEntityAI();
        this.tasks.addTask(5, new AIFollowLeader(this));
    }

    @Override
    public int getMaxSpawnedInChunk() {
        return this.getMaxGroupSize();
    }

    public int getMaxGroupSize() {
        return super.getMaxSpawnedInChunk();
    }

    @Override
    protected boolean canRandomSwim() {
        return !this.hasLeader();
    }

    public boolean hasLeader() {
        return this.leader != null && this.leader.isEntityAlive();
    }

    public void joinGroup(EntityAbstractGroupFish leader) {
        this.leader = leader;
        leader.groupSize++;
    }

    public void leaveGroup() {
        if (this.leader != null) this.leader.groupSize--;
        this.leader = null;
    }

    public boolean isLeader() {
        return this.groupSize > 1;
    }

    public boolean canGroupGrow() {
        return this.isLeader() && this.groupSize < this.getMaxGroupSize();
    }

    public boolean inRangeOfLeader() {
        return this.leader != null && this.getDistanceSq(this.leader) <= 121.0D;
    }

    public void moveToLeader() {
        if (this.hasLeader()) this.getNavigator().tryMoveToEntityLiving(this.leader, 1.0D);
    }

    /** Takes in as many of the given leaderless fish as the school has room for. */
    public void recruit(List<? extends EntityAbstractGroupFish> fish) {
        fish.stream().filter(other -> !other.hasLeader() && other != this)
                .limit(Math.max(0, this.getMaxGroupSize() - this.groupSize))
                .forEach(other -> other.joinGroup(this));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.isLeader() && this.world.rand.nextInt(200) == 1) {
            if (this.world.getEntitiesWithinAABB(this.getClass(), this.getEntityBoundingBox().grow(8.0D)).size() <= 1) this.groupSize = 1;
        }
    }

    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        super.onInitialSpawn(difficulty, data);
        if (data instanceof GroupData) {
            this.joinGroup(((GroupData) data).leader);
            return data;
        }
        return new GroupData(this);
    }

    public static class GroupData implements IEntityLivingData {

        public final EntityAbstractGroupFish leader;

        public GroupData(EntityAbstractGroupFish leader) {
            this.leader = leader;
        }
    }

    /** Joins or forms a school with nearby fish of the same kind and keeps close to its leader. */
    static class AIFollowLeader extends EntityAIBase {

        private final EntityAbstractGroupFish fish;
        private int navigateTimer;
        private int cooldown;

        AIFollowLeader(EntityAbstractGroupFish fish) {
            this.fish = fish;
            this.cooldown = this.nextCooldown();
        }

        private int nextCooldown() {
            // 1.13's formula, which works out to 200-219 ticks.
            return 200 + this.fish.getRNG().nextInt(200) % 20;
        }

        @Override
        public boolean shouldExecute() {
            if (this.fish.isLeader()) return false;
            if (this.fish.hasLeader()) return true;
            if (this.cooldown > 0) {
                --this.cooldown;
                return false;
            }
            this.cooldown = this.nextCooldown();
            List<EntityAbstractGroupFish> nearby = this.fish.world.<EntityAbstractGroupFish>getEntitiesWithinAABB(this.fish.getClass(),
                    this.fish.getEntityBoundingBox().grow(8.0D), other -> other != null && (other.canGroupGrow() || !other.hasLeader()));
            EntityAbstractGroupFish leader = nearby.stream().filter(EntityAbstractGroupFish::canGroupGrow).findAny().orElse(this.fish);
            leader.recruit(nearby);
            return this.fish.hasLeader();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.fish.hasLeader() && this.fish.inRangeOfLeader();
        }

        @Override
        public void startExecuting() {
            this.navigateTimer = 0;
        }

        @Override
        public void resetTask() {
            this.fish.leaveGroup();
        }

        @Override
        public void updateTask() {
            if (--this.navigateTimer <= 0) {
                this.navigateTimer = 10;
                this.fish.moveToLeader();
            }
        }
    }
}
