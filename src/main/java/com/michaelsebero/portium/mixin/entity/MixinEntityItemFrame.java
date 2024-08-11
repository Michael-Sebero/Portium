package com.michaelsebero.portium.mixin.entity;

import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

/** An item frame's eye is at its centre (1.13), so a frame under a ceiling takes its light from its own cell, not the block above. */
@Mixin(EntityItemFrame.class)
public abstract class MixinEntityItemFrame extends EntityHanging {

    private MixinEntityItemFrame(World world) {
        super(world);
    }

    @Override
    public float getEyeHeight() {
        return 0.0F;
    }
}
