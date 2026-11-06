package com.michaelsebero.portium.entity.ai;

import com.michaelsebero.portium.entity.villager.EntityVillager116;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

/**
 * 1.14's {@code DefendVillageTargetGoal}: a village's iron golem (not one a player built) goes after a player that a
 * villager near it thinks badly of (reputation -100 or worse: hurting or killing villagers).
 */
public class EntityAIGolemDefendVillagers extends EntityAITarget {

    private final EntityIronGolem golem;
    private EntityPlayer target;

    public EntityAIGolemDefendVillagers(EntityIronGolem golem) {
        super(golem, false, true);
        this.golem = golem;
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (this.golem.isPlayerCreated() || this.golem.getRNG().nextInt(10) != 0) return false;
        AxisAlignedBB around = this.golem.getEntityBoundingBox().grow(10.0D, 8.0D, 10.0D);
        List<EntityVillager116> villagers = this.golem.world.getEntitiesWithinAABB(EntityVillager116.class, around);
        List<EntityPlayer> players = this.golem.world.getEntitiesWithinAABB(EntityPlayer.class, around);
        for (EntityVillager116 villager : villagers) {
            for (EntityPlayer player : players) {
                if (player.isCreative() || player.isSpectator()) continue;
                if (villager.getPlayerReputation(player) <= -100) {
                    this.target = player;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void startExecuting() {
        this.golem.setAttackTarget(this.target);
        super.startExecuting();
    }
}
