package com.michaelsebero.portium.entity.ai.brain.sensor;

import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.TargetPredicate;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;

import java.util.Random;
import java.util.Set;

/** 1.14's sensors: every so often, read the world into a brain's memories. */
public abstract class Sensor<E extends EntityLivingBase> {

    private static final Random RANDOM = new Random();
    private static final TargetPredicate WITH_SIGHT = new TargetPredicate().setDistance(16.0D).allowFriendlyFire().setSkipAttackChecks();
    private static final TargetPredicate WITHOUT_INVISIBILITY = new TargetPredicate().setDistance(16.0D).allowFriendlyFire()
            .setSkipAttackChecks().setUseInvisibilityCheck();

    private final int interval;
    private long counter;

    public Sensor(int interval) {
        this.interval = interval;
        this.counter = RANDOM.nextInt(interval);
    }

    public Sensor() {
        this(20);
    }

    public final void tick(WorldServer world, E entity) {
        if (--this.counter <= 0L) {
            this.counter = this.interval;
            this.update(world, entity);
        }
    }

    protected abstract void update(WorldServer world, E entity);

    public abstract Set<MemoryModuleType<?>> getUsedMemories();

    /** Whether the entity can see (or, while already attacking it, keeps track of) the target. */
    public static boolean canAttackTarget(EntityLivingBase entity, EntityLivingBase target) {
        return BrainUtil.brain(entity).hasMemory(MemoryModuleType.ATTACK_TARGET, target)
                ? WITHOUT_INVISIBILITY.canTarget(entity, target) : WITH_SIGHT.canTarget(entity, target);
    }
}
