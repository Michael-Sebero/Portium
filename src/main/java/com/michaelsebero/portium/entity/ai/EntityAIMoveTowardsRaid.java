package com.michaelsebero.portium.entity.ai;

import com.michaelsebero.portium.entity.raid.Raid116;
import com.michaelsebero.portium.entity.raid.RaidManager116;
import com.michaelsebero.portium.world.Villages116;
import com.michaelsebero.portium.world.poi.PoiManager116;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

import java.util.List;

/**
 * 1.14's raider goals {@code MoveTowardsRaidGoal} and {@code InvadeHomeGoal}: a raider of an ongoing raid with no
 * target walks to the village, then from house to house (beds and workstations near the raid's centre).
 */
public class EntityAIMoveTowardsRaid extends EntityAIBase {

    private final EntityCreature raider;
    private BlockPos target;
    private int recheck;

    public EntityAIMoveTowardsRaid(EntityCreature raider) {
        this.raider = raider;
        this.setMutexBits(1);
    }

    /** Gives a raider the goal once (raiders joining a raid, or loaded in one). */
    public static void addTo(EntityLiving raider) {
        if (!(raider instanceof EntityCreature)) return;
        for (EntityAITasks.EntityAITaskEntry entry : raider.tasks.taskEntries) {
            if (entry.action instanceof EntityAIMoveTowardsRaid) return;
        }
        raider.tasks.addTask(3, new EntityAIMoveTowardsRaid((EntityCreature) raider));
    }

    private Raid116 raid() {
        return RaidManager116.raidOf(this.raider);
    }

    @Override
    public boolean shouldExecute() {
        if (this.raider.isRiding() || this.raider.isBeingRidden() || this.raider.getAttackTarget() != null) return false;
        Raid116 raid = this.raid();
        if (raid == null || !raid.isActive() || raid.isOver() || raid.isStopped()) return false;
        if (--this.recheck > 0) return false;
        this.recheck = 20 + this.raider.getRNG().nextInt(20);
        this.target = this.pickTarget(raid);
        return this.target != null;
    }

    private BlockPos pickTarget(Raid116 raid) {
        if (!Villages116.isVillage(this.raider.world, new BlockPos(this.raider))) {
            Vec3d toward = RandomPositionGenerator.findRandomTargetBlockTowards(this.raider, 15, 4,
                    new Vec3d(raid.getCenter().getX() + 0.5D, raid.getCenter().getY(), raid.getCenter().getZ() + 0.5D));
            return toward == null ? raid.getCenter() : new BlockPos(toward);
        }
        if (!(this.raider.world instanceof WorldServer)) return null;
        List<BlockPos> homes = PoiManager116.get((WorldServer) this.raider.world).find(type -> true, raid.getCenter(), 48, false);
        return homes.isEmpty() ? null : homes.get(this.raider.getRNG().nextInt(homes.size()));
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.target != null && this.raider.getAttackTarget() == null && !this.raider.getNavigator().noPath()
                && this.raider.getDistanceSq(this.target) > 4.0D;
    }

    @Override
    public void startExecuting() {
        this.raider.getNavigator().tryMoveToXYZ(this.target.getX() + 0.5D, this.target.getY(), this.target.getZ() + 0.5D, 1.0D);
    }

    @Override
    public void resetTask() {
        this.target = null;
    }
}
