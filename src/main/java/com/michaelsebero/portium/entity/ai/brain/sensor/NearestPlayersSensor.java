package com.michaelsebero.portium.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.TargetPredicate;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldServer;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Players within 16 blocks, nearest first; the nearest seen, and the nearest seen that a monster may target. */
public class NearestPlayersSensor extends Sensor<EntityLivingBase> {

    @Override
    public Set<MemoryModuleType<?>> getUsedMemories() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_PLAYERS, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER);
    }

    @Override
    protected void update(WorldServer world, EntityLivingBase entity) {
        List<EntityPlayer> players = world.playerEntities.stream().filter(player -> !player.isSpectator())
                .filter(player -> entity.getDistanceSq(player) < 16.0D * 16.0D)
                .sorted(Comparator.comparingDouble(entity::getDistanceSq)).collect(Collectors.toList());
        Brain<?> brain = BrainUtil.brain(entity);
        brain.setMemory(MemoryModuleType.NEAREST_PLAYERS, players);
        List<EntityPlayer> visible = players.stream().filter(player -> canAttackTarget(entity, player)).collect(Collectors.toList());
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER, visible.isEmpty() ? null : visible.get(0));
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER, visible.stream().filter(TargetPredicate::canHostileTarget).findFirst());
    }
}
