package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.memory.WalkTarget;
import net.minecraft.entity.EntityCreature;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;

import java.util.Optional;

/** Wanders to a random spot on land. */
public class WalkRandomlyTask extends Task<EntityCreature> {

    private final float speed;
    private final int maxXZ;
    private final int maxY;

    public WalkRandomlyTask(float speed) {
        this(speed, 10, 7);
    }

    public WalkRandomlyTask(float speed, int maxXZ, int maxY) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryModuleStatus.VALUE_ABSENT));
        this.speed = speed;
        this.maxXZ = maxXZ;
        this.maxY = maxY;
    }

    @Override
    protected void startExecuting(WorldServer world, EntityCreature entity, long gameTime) {
        Optional<Vec3d> pos = Optional.ofNullable(com.michaelsebero.portium.entity.ai.brain.LandPos116.find(entity, this.maxXZ, this.maxY));
        BrainUtil.brain(entity).setMemory(MemoryModuleType.WALK_TARGET, pos.map(target -> new WalkTarget(target, this.speed, 0)));
    }
}
