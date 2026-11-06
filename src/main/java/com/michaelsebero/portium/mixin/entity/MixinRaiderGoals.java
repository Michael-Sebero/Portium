package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.entity.ai.EntityAIPatrol;
import com.michaelsebero.portium.entity.ai.EntityAIRaiderFindTarget;
import net.minecraft.entity.monster.EntityEvoker;
import net.minecraft.entity.monster.EntityIllusionIllager;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityVindicator;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.14's raider goals for 1.12's illagers and witches: patrolling, and (vindicators) turning on a target as a group. */
@Mixin({EntityVindicator.class, EntityEvoker.class, EntityIllusionIllager.class, EntityWitch.class})
public abstract class MixinRaiderGoals extends EntityMob {

    private MixinRaiderGoals(World world) {
        super(world);
    }

    @Inject(method = "initEntityAI", at = @At("TAIL"))
    private void portium$addRaiderGoals(CallbackInfo ci) {
        this.tasks.addTask(4, new EntityAIPatrol(this, 0.7D, 0.595D));
        if ((Object) this instanceof EntityVindicator) this.tasks.addTask(3, new EntityAIRaiderFindTarget(this, 10.0F));
    }
}
