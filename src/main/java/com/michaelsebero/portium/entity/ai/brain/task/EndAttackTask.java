package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.world.PortiumGameRules;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.function.BiPredicate;

/** When the attack target dies: celebrate where it fell (and maybe dance), and forget it unless it's an unforgiven player. */
public class EndAttackTask extends Task<EntityLivingBase> {

    private final int celebrateDuration;
    private final BiPredicate<EntityLivingBase, EntityLivingBase> dancePredicate;

    public EndAttackTask(int celebrateDuration, BiPredicate<EntityLivingBase, EntityLivingBase> dancePredicate) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryModuleStatus.VALUE_PRESENT, MemoryModuleType.ANGRY_AT, MemoryModuleStatus.REGISTERED,
                MemoryModuleType.CELEBRATE_LOCATION, MemoryModuleStatus.VALUE_ABSENT, MemoryModuleType.DANCING, MemoryModuleStatus.REGISTERED));
        this.celebrateDuration = celebrateDuration;
        this.dancePredicate = dancePredicate;
    }

    @Override
    protected boolean shouldExecute(WorldServer world, EntityLivingBase entity) {
        return BrainUtil.brain(entity).getMemory(MemoryModuleType.ATTACK_TARGET).get().getHealth() <= 0.0F;
    }

    @Override
    protected void startExecuting(WorldServer world, EntityLivingBase entity, long gameTime) {
        Brain<?> brain = BrainUtil.brain(entity);
        EntityLivingBase target = brain.getMemory(MemoryModuleType.ATTACK_TARGET).get();
        if (this.dancePredicate.test(entity, target)) brain.replaceMemory(MemoryModuleType.DANCING, true, this.celebrateDuration);
        brain.replaceMemory(MemoryModuleType.CELEBRATE_LOCATION, new BlockPos(target), this.celebrateDuration);
        if (!(target instanceof EntityPlayer) || PortiumGameRules.get(world, PortiumGameRules.FORGIVE_DEAD_PLAYERS, true)) {
            brain.removeMemory(MemoryModuleType.ATTACK_TARGET);
            brain.removeMemory(MemoryModuleType.ANGRY_AT);
        }
    }
}
