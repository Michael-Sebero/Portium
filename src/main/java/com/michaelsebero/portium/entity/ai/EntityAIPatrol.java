package com.michaelsebero.portium.entity.ai;

import com.michaelsebero.portium.entity.raid.IRaider;
import com.michaelsebero.portium.entity.raid.RaiderState;
import com.michaelsebero.portium.entity.raid.Raiders116;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Random;

/**
 * 1.14's {@code PatrollerEntity.PatrolGoal}: a patrol walks toward its target in 10-block steps, angled off the
 * straight line; the leader picks a new target near the old one and the rest follow its steps. A lone patroller stops
 * patrolling.
 */
public class EntityAIPatrol extends EntityAIBase {

    private final EntityLiving owner;
    private final double speed;
    private final double leaderSpeed;
    private long cooldownUntil = -1L;

    public EntityAIPatrol(EntityLiving owner, double speed, double leaderSpeed) {
        this.owner = owner;
        this.speed = speed;
        this.leaderSpeed = leaderSpeed;
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        RaiderState state = IRaider.state(this.owner);
        boolean coolingDown = this.owner.world.getTotalWorldTime() < this.cooldownUntil;
        return state.isPatrolling() && this.owner.getAttackTarget() == null && !this.owner.isBeingRidden() && state.hasPatrolTarget() && !coolingDown;
    }

    @Override
    public void updateTask() {
        RaiderState state = IRaider.state(this.owner);
        boolean leader = state.isLeader();
        PathNavigate navigator = this.owner.getNavigator();
        if (!navigator.noPath()) return;
        List<EntityLiving> patrol = this.patrol();
        BlockPos target = state.getPatrolTarget();
        if (state.isPatrolling() && patrol.isEmpty()) {
            state.setPatrolling(false);
        } else if (leader && target.distanceSqToCenter(this.owner.posX, this.owner.posY, this.owner.posZ) < 100.0D) {
            state.resetPatrolTarget(this.owner);
        } else {
            Vec3d goal = new Vec3d(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
            Vec3d position = this.owner.getPositionVector();
            goal = position.subtract(goal).rotateYaw(90.0F).scale(0.4D).add(goal);
            Vec3d step = goal.subtract(position).normalize().scale(10.0D).add(position);
            BlockPos stepPos = Raiders116.surface(this.owner.world, new BlockPos(step));
            if (!navigator.tryMoveToXYZ(stepPos.getX(), stepPos.getY(), stepPos.getZ(), leader ? this.leaderSpeed : this.speed)) {
                this.wander();
                this.cooldownUntil = this.owner.world.getTotalWorldTime() + 200L;
            } else if (leader) {
                for (EntityLiving member : patrol) IRaider.state(member).setPatrolTarget(stepPos);
            }
        }
    }

    /** Raiders within 16 blocks that aren't raiding. */
    private List<EntityLiving> patrol() {
        return this.owner.world.getEntitiesWithinAABB(EntityLiving.class, this.owner.getEntityBoundingBox().grow(16.0D),
                other -> other instanceof IRaider && other != this.owner && Raiders116.notInRaid(other));
    }

    private boolean wander() {
        Random random = this.owner.getRNG();
        BlockPos pos = Raiders116.surface(this.owner.world, new BlockPos(this.owner).add(-8 + random.nextInt(16), 0, -8 + random.nextInt(16)));
        return this.owner.getNavigator().tryMoveToXYZ(pos.getX(), pos.getY(), pos.getZ(), this.speed);
    }
}
