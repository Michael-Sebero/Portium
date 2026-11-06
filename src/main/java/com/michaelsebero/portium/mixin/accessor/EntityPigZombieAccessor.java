package com.michaelsebero.portium.mixin.accessor;

import net.minecraft.entity.monster.EntityPigZombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(EntityPigZombie.class)
public interface EntityPigZombieAccessor {

    @Accessor("angerLevel")
    int portium$getAngerLevel();

    @Accessor("angerLevel")
    void portium$setAngerLevel(int angerLevel);

    @Accessor("angerTargetUUID")
    UUID portium$getAngerTarget();

    @Accessor("angerTargetUUID")
    void portium$setAngerTarget(UUID target);
}
