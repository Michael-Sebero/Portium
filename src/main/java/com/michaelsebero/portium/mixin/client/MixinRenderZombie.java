package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.api.IDrowningZombie;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderZombie;
import net.minecraft.entity.monster.EntityZombie;
import org.spongepowered.asm.mixin.Mixin;

/** A zombie (or husk) that is converting after drowning shakes (1.13). */
@Mixin(RenderZombie.class)
public abstract class MixinRenderZombie extends RenderBiped<EntityZombie> {

    private MixinRenderZombie(RenderManager manager, ModelBiped model, float shadow) {
        super(manager, model, shadow);
    }

    @Override
    protected void applyRotations(EntityZombie zombie, float ageInTicks, float rotationYaw, float partialTicks) {
        if (IDrowningZombie.of(zombie).portium$isDrowning()) {
            rotationYaw += (float) (Math.cos(zombie.ticksExisted * 3.25D) * Math.PI * 0.25D);
        }
        super.applyRotations(zombie, ageInTicks, rotationYaw, partialTicks);
    }
}
