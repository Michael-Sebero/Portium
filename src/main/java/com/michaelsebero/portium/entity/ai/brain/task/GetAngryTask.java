package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.world.PortiumGameRules;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldServer;

/** Stops being angry at someone who has died (a player only if dead players are forgiven). */
public class GetAngryTask<E extends EntityLiving> extends Task<E> {

    public GetAngryTask() {
        super(ImmutableMap.of(MemoryModuleType.ANGRY_AT, MemoryModuleStatus.VALUE_PRESENT));
    }

    @Override
    protected void startExecuting(WorldServer world, E entity, long gameTime) {
        BrainUtil.getTargetFromMemory(entity, MemoryModuleType.ANGRY_AT).ifPresent(target -> {
            if (target.getHealth() <= 0.0F && (!(target instanceof EntityPlayer) || PortiumGameRules.get(world, PortiumGameRules.FORGIVE_DEAD_PLAYERS, true))) {
                BrainUtil.brain(entity).removeMemory(MemoryModuleType.ANGRY_AT);
            }
        });
    }
}
