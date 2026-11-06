package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.entity.passive.EntityCat;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Creepers keep away from cats (1.14) as from ocelots. */
@Mixin(EntityCreeper.class)
public abstract class MixinEntityCreeperCats extends EntityMob {

    protected MixinEntityCreeperCats(World world) {
        super(world);
    }

    @Inject(method = "initEntityAI", at = @At("TAIL"))
    private void portium$avoidCats(CallbackInfo ci) {
        this.tasks.addTask(3, new EntityAIAvoidEntity<>(this, EntityCat.class, 6.0F, 1.0D, 1.2D));
    }
}
